import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import { Loader2, Trophy, Swords, Clock } from "lucide-react";
import { toast } from "sonner";
import { api } from "../lib/api";
import { acquireStomp, releaseStomp, subscribe } from "../lib/websocket";
import { useAuthStore } from "../store/authStore";
import type { QueueStatus, RankedMatchFoundEvent, LeaderboardEntry } from "../lib/types";
import type { StompSubscription } from "@stomp/stompjs";

export function RankedPage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);

  const [status, setStatus] = useState<QueueStatus | null>(null);
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [elapsed, setElapsed] = useState(0);

  // Initial fetch
  useEffect(() => {
    api.get<QueueStatus>("/api/ranked/queue/status").then(setStatus).catch(() => {});
    api.get<LeaderboardEntry[]>("/api/ranked/leaderboard").then(setLeaderboard).catch(() => {});
  }, []);

  // Subscribe to match-found event over STOMP
  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    let sub: StompSubscription | null = null;

    acquireStomp()
      .then(() => {
        if (cancelled) return;
        sub = subscribe(`/topic/user/${user.userId}/ranked-match`, (msg) => {
          const ev: RankedMatchFoundEvent = JSON.parse(msg.body);
          toast.success("Match found! Joining game...");
          navigate(`/game/${ev.gameId}`);
        });
      })
      .catch(() => {});

    return () => {
      cancelled = true;
      sub?.unsubscribe();
      releaseStomp();
    };
  }, [user, navigate]);

  // Live "elapsed in queue" ticker
  useEffect(() => {
    if (!status?.inQueue) {
      setElapsed(0);
      return;
    }
    const tick = () => setElapsed(Math.floor((Date.now() - status.enqueuedAtMs) / 1000));
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [status?.inQueue, status?.enqueuedAtMs]);

  // Poll status while in queue so queueSize stays fresh
  useEffect(() => {
    if (!status?.inQueue) return;
    const id = setInterval(() => {
      api.get<QueueStatus>("/api/ranked/queue/status").then(setStatus).catch(() => {});
    }, 3000);
    return () => clearInterval(id);
  }, [status?.inQueue]);

  const onCooldown =
    status?.cooldownUntilMs != null && status.cooldownUntilMs > Date.now();
  const cooldownSecsLeft = onCooldown
    ? Math.ceil((status!.cooldownUntilMs! - Date.now()) / 1000)
    : 0;

  const handleJoin = async () => {
    setLoading(true);
    try {
      const s = await api.post<QueueStatus>("/api/ranked/queue");
      setStatus(s);
    } catch (e) {
      toast.error((e as Error).message || "Could not join queue");
    } finally {
      setLoading(false);
    }
  };

  const handleLeave = async () => {
    setLoading(true);
    try {
      const s = await api.delete<QueueStatus>("/api/ranked/queue");
      setStatus(s);
    } catch (e) {
      toast.error((e as Error).message || "Could not leave queue");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-3xl mx-auto space-y-6">
        <div className="text-center space-y-2">
          <h1 className="text-3xl font-bold flex items-center justify-center gap-2">
            <Swords className="w-7 h-7" /> Ranked
          </h1>
          <p className="text-muted-foreground">
            10-player fixed config, ELO-matched, no bots
          </p>
        </div>

        <Card>
          <CardContent className="pt-6 flex items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <Trophy className="w-8 h-8 text-yellow-500" />
              <div>
                <p className="text-muted-foreground text-sm">Your ELO</p>
                <p className="text-2xl font-semibold">{status?.elo ?? "—"}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Clock className="w-8 h-8 text-blue-500" />
              <div>
                <p className="text-muted-foreground text-sm">Players in queue</p>
                <p className="text-2xl font-semibold">{status?.queueSize ?? 0}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6 space-y-4">
            {status?.inQueue ? (
              <>
                <div className="text-center space-y-1">
                  <p className="text-lg">Searching for a match...</p>
                  <p className="text-sm text-muted-foreground">
                    {elapsed}s elapsed
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Strict ELO band for the first 30s, then any opponents.
                  </p>
                </div>
                <Button
                  variant="outline"
                  onClick={handleLeave}
                  disabled={loading}
                  className="w-full"
                >
                  {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : "Leave Queue"}
                </Button>
              </>
            ) : onCooldown ? (
              <div className="text-center space-y-2">
                <p className="text-yellow-500">
                  Ranked cooldown — wait {cooldownSecsLeft}s
                </p>
                <p className="text-xs text-muted-foreground">
                  You left a recent ranked match before it finished.
                </p>
              </div>
            ) : (
              <Button
                onClick={handleJoin}
                disabled={loading}
                size="lg"
                className="w-full"
              >
                {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : "Join Ranked Queue"}
              </Button>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Leaderboard</CardTitle>
          </CardHeader>
          <CardContent>
            {leaderboard.length === 0 ? (
              <p className="text-center text-muted-foreground py-4">No ranked players yet.</p>
            ) : (
              <div className="space-y-1">
                {leaderboard.map((entry, i) => (
                  <div
                    key={entry.userId}
                    className="flex items-center justify-between p-2 rounded hover:bg-accent"
                  >
                    <div className="flex items-center gap-3">
                      <span className="w-6 text-muted-foreground text-sm tabular-nums">
                        {i + 1}
                      </span>
                      <span
                        className="cursor-pointer hover:underline"
                        onClick={() => navigate(`/profile/${entry.userId}`)}
                      >
                        {entry.username}
                      </span>
                    </div>
                    <span className="font-semibold tabular-nums">{entry.elo}</span>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
