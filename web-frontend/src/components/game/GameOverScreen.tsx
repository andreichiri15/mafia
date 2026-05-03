import { useNavigate } from "react-router-dom";
import { Button } from "../ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Badge } from "../ui/badge";
import { Avatar, AvatarFallback } from "../ui/avatar";
import type { GameOverEvent } from "../../lib/types";

interface GameOverScreenProps {
  result: GameOverEvent;
}

export function GameOverScreen({ result }: GameOverScreenProps) {
  const navigate = useNavigate();

  const winInfo = (() => {
    switch (result.winningTeam) {
      case "MAFIA_WIN":
        return {
          title: "Mafia Wins!",
          subtitle: "The mafia has taken over the town.",
          color: "text-red-500",
        };
      case "VILLAGER_WIN":
        return {
          title: "Villagers Win!",
          subtitle: "The town has eliminated all mafia members.",
          color: "text-green-500",
        };
      case "JESTER_WIN":
        return {
          title: "Jester Wins!",
          subtitle: "The jester convinced the town to vote them out.",
          color: "text-purple-500",
        };
    }
  })();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80">
      <Card className="w-full max-w-lg mx-4">
        <CardHeader className="text-center">
          <CardTitle className={`text-3xl ${winInfo.color}`}>
            {winInfo.title}
          </CardTitle>
          <p className="text-muted-foreground">{winInfo.subtitle}</p>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-2">
            {result.players.map((player) => (
              <div
                key={player.userId}
                className={`flex items-center gap-2 p-2 rounded-lg border ${
                  !player.alive ? "opacity-50" : ""
                }`}
              >
                <Avatar className="w-8 h-8">
                  <AvatarFallback>{player.username[0].toUpperCase()}</AvatarFallback>
                </Avatar>
                <div className="flex-1 min-w-0">
                  <p className={`text-sm font-medium truncate ${!player.alive ? "line-through" : ""}`}>
                    {player.username}
                  </p>
                </div>
                <Badge
                  variant={
                    player.role === "MAFIA" || player.role === "MUTILATOR"
                      ? "destructive"
                      : "secondary"
                  }
                  className="text-xs"
                >
                  {player.role}
                </Badge>
              </div>
            ))}
          </div>
          <Button onClick={() => navigate("/play")} className="w-full">
            Back to Servers
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
