import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Avatar, AvatarFallback } from "./ui/avatar";
import { Badge } from "./ui/badge";
import { ScrollArea } from "./ui/scroll-area";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "./ui/accordion";
import { Trophy, Users, Target, Heart, Clock, Loader2, Swords } from "lucide-react";
import { api } from "../lib/api";
import { useAuthStore } from "../store/authStore";
import type { GameActionEntry, GamePhase, ProfileResponse, Role, WinningTeam } from "../lib/types";

const roleColors: Record<Role, string> = {
  MAFIA: "text-red-500",
  VILLAGER: "text-blue-500",
  SHERIFF: "text-yellow-500",
  DOCTOR: "text-emerald-500",
  JESTER: "text-purple-500",
  MUTILATOR: "text-orange-500",
};

const winLabel: Record<WinningTeam, string> = {
  MAFIA_WIN: "Mafia",
  VILLAGER_WIN: "Town",
  JESTER_WIN: "Jester",
};

function formatDuration(totalSeconds: number): string {
  if (!totalSeconds) return "—";
  const m = Math.floor(totalSeconds / 60);
  const s = Math.floor(totalSeconds % 60);
  return `${m}m ${s}s`;
}

function formatPercent(value: number): string {
  return `${Math.round(value * 100)}%`;
}

export function ProfilePage() {
  const { userId: routeUserId } = useParams<{ userId: string }>();
  const me = useAuthStore((s) => s.user);

  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fall back to self when no :userId in URL
  const targetUserId = routeUserId ? Number(routeUserId) : me?.userId;
  const isSelf = !routeUserId || (me && Number(routeUserId) === me.userId);

  useEffect(() => {
    if (!targetUserId) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .get<ProfileResponse>(`/api/profile/${targetUserId}`)
      .then((data) => {
        if (!cancelled) {
          setProfile(data);
          setLoading(false);
        }
      })
      .catch((e) => {
        if (!cancelled) {
          setError((e as Error).message || "Could not load profile");
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [targetUserId]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-4rem)]">
        <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-4rem)]">
        <p className="text-red-500">{error ?? "Profile not found"}</p>
      </div>
    );
  }

  const memberSince = new Date(profile.dateJoined);

  // Order roles for display
  const orderedRoles: Role[] = ["MAFIA", "VILLAGER", "SHERIFF", "DOCTOR", "JESTER", "MUTILATOR"];

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-6xl mx-auto space-y-6">
        {/* Header */}
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-6">
              <Avatar className="w-24 h-24">
                <AvatarFallback className="text-2xl">
                  {profile.username[0].toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1">
                <h2 className="text-2xl font-semibold">{profile.username}</h2>
                <p className="text-muted-foreground">
                  Member since {memberSince.toLocaleDateString(undefined, { year: "numeric", month: "long" })}
                </p>
                {!isSelf && (
                  <p className="text-xs text-muted-foreground mt-1">Viewing another player's profile</p>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Top-level stats */}
        <div className="grid sm:grid-cols-2 md:grid-cols-6 gap-4">
          <StatCard
            icon={<Swords className="w-7 h-7 text-orange-500" />}
            label="Ranked ELO"
            value={(profile.elo ?? 1000).toString()}
          />
          <StatCard
            icon={<Trophy className="w-7 h-7 text-yellow-500" />}
            label="Win Rate"
            value={profile.totalGames > 0 ? formatPercent(profile.winRate) : "—"}
            sub={
              profile.totalGames > 0
                ? `${profile.totalWins}/${profile.totalGames} wins`
                : "No games yet"
            }
          />
          <StatCard
            icon={<Users className="w-7 h-7 text-blue-500" />}
            label="Games Played"
            value={profile.totalGames.toString()}
          />
          <StatCard
            icon={<Target className="w-7 h-7 text-purple-500" />}
            label="Favorite Role"
            value={profile.favoriteRole ?? "—"}
            valueClass={profile.favoriteRole ? roleColors[profile.favoriteRole] : ""}
          />
          <StatCard
            icon={<Heart className="w-7 h-7 text-pink-500" />}
            label="Survival Rate"
            value={profile.totalGames > 0 ? formatPercent(profile.survivalRate) : "—"}
            sub="Alive at game end"
          />
          <StatCard
            icon={<Clock className="w-7 h-7 text-green-500" />}
            label="Avg Game"
            value={profile.totalGames > 0 ? formatDuration(profile.avgGameDurationSeconds) : "—"}
          />
        </div>

        {/* Per-role winrate */}
        <Card>
          <CardHeader>
            <CardTitle>Win Rate by Role</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid sm:grid-cols-2 md:grid-cols-3 gap-3">
              {orderedRoles.map((role) => {
                const stats = profile.roleStats[role];
                const played = stats?.played ?? 0;
                const won = stats?.won ?? 0;
                const wr = stats?.winRate ?? 0;
                return (
                  <div
                    key={role}
                    className="flex flex-col gap-1 p-3 rounded-lg border bg-card/50"
                  >
                    <div className="flex items-center justify-between">
                      <span className={`font-semibold ${roleColors[role]}`}>{role}</span>
                      <span className="text-sm text-muted-foreground">
                        {played > 0 ? `${won}/${played}` : "0"}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="flex-1 h-2 bg-muted rounded overflow-hidden">
                        <div
                          className={`h-full ${roleColors[role].replace("text", "bg")}`}
                          style={{ width: `${wr * 100}%` }}
                        />
                      </div>
                      <span className="text-sm tabular-nums w-12 text-right">
                        {played > 0 ? formatPercent(wr) : "—"}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </CardContent>
        </Card>

        {/* Match history */}
        <Card>
          <CardHeader>
            <CardTitle>Recent Matches</CardTitle>
          </CardHeader>
          <CardContent>
            {profile.matchHistory.length === 0 ? (
              <p className="text-center text-muted-foreground py-8">No completed games yet.</p>
            ) : (
              <ScrollArea className="max-h-[600px]">
                <div className="hidden sm:grid grid-cols-[1.4fr_1.2fr_1fr_1fr_1.1fr_1fr_2rem] gap-3 px-3 pb-2 text-xs uppercase tracking-wide text-muted-foreground border-b">
                  <span>Date</span>
                  <span>Role</span>
                  <span>Winner</span>
                  <span>Duration</span>
                  <span>Survived</span>
                  <span>Result</span>
                  <span></span>
                </div>
                <Accordion type="multiple">
                  {profile.matchHistory.map((m) => (
                    <AccordionItem key={m.gameId} value={String(m.gameId)}>
                      <AccordionTrigger className="px-3 hover:no-underline">
                        <div className="grid sm:grid-cols-[1.4fr_1.2fr_1fr_1fr_1.1fr_1fr] grid-cols-2 gap-3 flex-1 items-center">
                          <span className="whitespace-nowrap text-sm">
                            {new Date(m.endedAt || m.startedAt).toLocaleDateString()}
                          </span>
                          <span className={`text-sm ${m.role ? roleColors[m.role] : ""}`}>
                            {m.role ?? "—"}
                          </span>
                          <span className="text-sm">{m.winningTeam ? winLabel[m.winningTeam] : "—"}</span>
                          <span className="text-sm">{formatDuration(m.durationSeconds)}</span>
                          <span className="text-sm">
                            {m.alive ? (
                              <span className="text-green-500">Yes</span>
                            ) : (
                              <span className="text-muted-foreground">
                                {m.deathCause ? m.deathCause.replace("_", " ").toLowerCase() : "no"}
                              </span>
                            )}
                          </span>
                          <span className="text-sm">
                            <Badge variant={m.won ? "default" : "destructive"}>
                              {m.won ? "Win" : "Loss"}
                            </Badge>
                          </span>
                        </div>
                      </AccordionTrigger>
                      <AccordionContent className="px-3">
                        <GameActionHistory gameId={m.gameId} />
                      </AccordionContent>
                    </AccordionItem>
                  ))}
                </Accordion>
              </ScrollArea>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

interface StatCardProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  sub?: string;
  valueClass?: string;
}

function StatCard({ icon, label, value, sub, valueClass }: StatCardProps) {
  return (
    <Card>
      <CardContent className="pt-6">
        <div className="flex items-center gap-3">
          {icon}
          <div className="flex-1 min-w-0">
            <p className="text-muted-foreground text-sm truncate">{label}</p>
            <p className={`text-xl font-semibold ${valueClass ?? ""}`}>{value}</p>
            {sub && <p className="text-xs text-muted-foreground truncate">{sub}</p>}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

const phaseLabel: Record<GamePhase, string> = {
  NIGHT: "Night",
  DAY: "Day",
  VOTING: "Voting",
  GAME_OVER: "End",
};

function describeAction(a: GameActionEntry): string {
  const actor = a.actorUsername ?? "?";
  const target = a.targetUsername ?? "?";
  switch (a.actionType) {
    case "VOTE":
      return `${actor} voted for ${target}`;
    case "MAFIA_KILL":
      return `${actor} targeted ${target} for the mafia kill`;
    case "HEALED":
      return `${actor} (doctor) protected ${target}`;
    case "INVESTIGATE":
      return `${actor} (sheriff) investigated ${target}${a.result ? ` → ${a.result === "MAFIA" ? "mafia" : "not mafia"}` : ""}`;
    case "MUTE":
      return `${actor} (mutilator) silenced ${target}`;
    case "REVOKE_VOTE":
      return `${actor} (mutilator) revoked ${target}'s vote`;
  }
}

function GameActionHistory({ gameId }: { gameId: number }) {
  const [actions, setActions] = useState<GameActionEntry[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .get<GameActionEntry[]>(`/api/games/${gameId}/actions`)
      .then((data) => {
        if (!cancelled) {
          setActions(data);
          setLoading(false);
        }
      })
      .catch((e) => {
        if (!cancelled) {
          setError((e as Error).message || "Could not load actions");
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [gameId]);

  if (loading) {
    return (
      <div className="flex items-center gap-2 text-sm text-muted-foreground py-2">
        <Loader2 className="w-4 h-4 animate-spin" /> Loading actions…
      </div>
    );
  }
  if (error) {
    return <p className="text-sm text-red-500 py-2">{error}</p>;
  }
  if (!actions || actions.length === 0) {
    return <p className="text-sm text-muted-foreground py-2">No actions were recorded.</p>;
  }

  // Group by round, then by phase, preserving insertion order
  const grouped = new Map<number, Map<GamePhase, GameActionEntry[]>>();
  for (const a of actions) {
    if (!grouped.has(a.round)) grouped.set(a.round, new Map());
    const byPhase = grouped.get(a.round)!;
    if (!byPhase.has(a.phase)) byPhase.set(a.phase, []);
    byPhase.get(a.phase)!.push(a);
  }

  return (
    <div className="space-y-3 py-1">
      {Array.from(grouped.entries()).map(([round, byPhase]) => (
        <div key={round} className="border-l-2 border-muted pl-3 space-y-2">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
            Round {round}
          </p>
          {Array.from(byPhase.entries()).map(([phase, items]) => (
            <div key={phase} className="space-y-1">
              <p className="text-xs text-muted-foreground">{phaseLabel[phase]}</p>
              <ul className="text-sm space-y-0.5">
                {items.map((a) => (
                  <li key={a.id} className="text-foreground/90">
                    {describeAction(a)}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}
