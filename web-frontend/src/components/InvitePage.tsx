import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import { Loader2, Lock, Users } from "lucide-react";
import { useLobbyStore } from "../store/lobbyStore";
import { useAuthStore } from "../store/authStore";
import type { InviteResolution } from "../lib/types";

export function InvitePage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const { resolveInvite, joinLobby } = useLobbyStore();

  const [info, setInfo] = useState<InviteResolution | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [joining, setJoining] = useState(false);

  useEffect(() => {
    if (!token) return;
    if (!isLoggedIn) {
      // Bounce to sign-in, remembering where to come back
      navigate(`/signin?redirect=/invite/${token}`);
      return;
    }
    let cancelled = false;
    setLoading(true);
    resolveInvite(token)
      .then((data) => {
        if (!cancelled) {
          setInfo(data);
          setLoading(false);
        }
      })
      .catch((e) => {
        if (!cancelled) {
          setError((e as Error).message || "Invite link is invalid");
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [token, isLoggedIn, resolveInvite, navigate]);

  const handleJoin = async () => {
    if (!info) return;
    setJoining(true);
    try {
      await joinLobby(info.lobbyId);
      navigate(`/lobby/${info.lobbyId}`);
    } catch (e) {
      setError((e as Error).message || "Could not join lobby");
      setJoining(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-4rem)]">
        <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error || !info) {
    return (
      <div className="flex flex-col items-center justify-center h-[calc(100vh-4rem)] gap-4">
        <p className="text-red-500">{error ?? "Invite not found"}</p>
        <Button onClick={() => navigate("/play")}>Browse Servers</Button>
      </div>
    );
  }

  const isFull = info.currentPlayers >= info.maxPlayers;
  const cantJoin = info.inGame || isFull;

  return (
    <div className="container mx-auto px-4 py-12">
      <div className="max-w-md mx-auto">
        <Card>
          <CardHeader className="text-center">
            <CardTitle>You've been invited!</CardTitle>
            <p className="text-muted-foreground">Hosted by {info.hostname}</p>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="text-center space-y-2">
              <h3 className="text-2xl font-semibold">{info.name}</h3>
              <div className="flex items-center justify-center gap-3 text-sm text-muted-foreground">
                <span className="flex items-center gap-1">
                  <Users className="w-4 h-4" />
                  {info.currentPlayers}/{info.maxPlayers}
                </span>
                {info.isLocked && (
                  <span className="flex items-center gap-1">
                    <Lock className="w-4 h-4" />
                    Locked
                  </span>
                )}
              </div>
            </div>

            {info.inGame && (
              <p className="text-sm text-yellow-500 text-center">
                A game is already in progress in this lobby.
              </p>
            )}
            {isFull && !info.inGame && (
              <p className="text-sm text-yellow-500 text-center">This lobby is full.</p>
            )}

            <div className="flex gap-2">
              <Button variant="outline" onClick={() => navigate("/play")} className="flex-1">
                Cancel
              </Button>
              <Button onClick={handleJoin} disabled={cantJoin || joining} className="flex-1">
                {joining ? <Loader2 className="w-4 h-4 animate-spin" /> : "Join Lobby"}
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
