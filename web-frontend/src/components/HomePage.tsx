import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "./ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Users, Shield, Eye, Play, DoorOpen } from "lucide-react";
import { api } from "../lib/api";
import { useAuthStore } from "../store/authStore";
import type { SessionInfo } from "../lib/types";

export function HomePage() {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const navigate = useNavigate();
  const [session, setSession] = useState<SessionInfo | null>(null);

  useEffect(() => {
    if (!isLoggedIn) {
      setSession(null);
      return;
    }
    let cancelled = false;
    api
      .get<SessionInfo | null>("/api/sessions/me")
      .then((data) => {
        if (!cancelled) setSession(data);
      })
      .catch(() => {
        if (!cancelled) setSession(null);
      });
    return () => {
      cancelled = true;
    };
  }, [isLoggedIn]);

  const inGame = session?.gameId != null;
  const phaseLabel = session?.gamePhase
    ? session.gamePhase.charAt(0) + session.gamePhase.slice(1).toLowerCase()
    : null;

  return (
    <div className="container mx-auto px-4 py-12">
      <div className="max-w-4xl mx-auto text-center space-y-8">
        <div className="space-y-4">
          <h1>Welcome to Mafia</h1>
          <p className="text-muted-foreground">
            The classic social deduction game. Join friends, uncover the mafia, and survive the night.
          </p>
        </div>

        {session && (
          <Card className="border-primary/40 bg-primary/5 text-left max-w-xl mx-auto">
            <CardContent className="pt-6 flex items-center gap-4">
              <div className={`p-3 rounded-full ${inGame ? "bg-yellow-500/20" : "bg-blue-500/20"}`}>
                {inGame ? (
                  <Play className="w-6 h-6 text-yellow-500" />
                ) : (
                  <DoorOpen className="w-6 h-6 text-blue-500" />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-semibold">
                  {inGame ? "You're in an ongoing game" : "You're in a lobby"}
                </p>
                <p className="text-sm text-muted-foreground truncate">
                  {session.lobbyName}
                  {inGame && phaseLabel && ` · ${phaseLabel} phase`}
                  {inGame && !session.alive && " · spectating"}
                </p>
              </div>
              <Button
                onClick={() =>
                  inGame
                    ? navigate(`/game/${session.gameId}`)
                    : navigate(`/lobby/${session.lobbyId}`)
                }
              >
                Rejoin
              </Button>
            </CardContent>
          </Card>
        )}

        <Button asChild size="lg">
          <Link to="/play">Start Playing</Link>
        </Button>

        <div className="grid md:grid-cols-3 gap-6 mt-12">
          <Card>
            <CardHeader>
              <Users className="w-8 h-8 mb-2 text-primary" />
              <CardTitle>Social Deduction</CardTitle>
            </CardHeader>
            <CardContent>
              <CardDescription>
                Work with your team to identify the mafia members before it's too late.
              </CardDescription>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <Shield className="w-8 h-8 mb-2 text-primary" />
              <CardTitle>Strategic Gameplay</CardTitle>
            </CardHeader>
            <CardContent>
              <CardDescription>
                Use your role's unique abilities to protect the innocent or eliminate threats.
              </CardDescription>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <Eye className="w-8 h-8 mb-2 text-primary" />
              <CardTitle>Mystery & Intrigue</CardTitle>
            </CardHeader>
            <CardContent>
              <CardDescription>
                Every game is different. Read between the lines and trust no one.
              </CardDescription>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
