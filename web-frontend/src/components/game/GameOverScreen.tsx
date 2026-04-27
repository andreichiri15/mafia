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
  const isMafiaWin = result.winningTeam === "MAFIA_WIN";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80">
      <Card className="w-full max-w-lg mx-4">
        <CardHeader className="text-center">
          <CardTitle className={`text-3xl ${isMafiaWin ? "text-red-500" : "text-green-500"}`}>
            {isMafiaWin ? "Mafia Wins!" : "Villagers Win!"}
          </CardTitle>
          <p className="text-muted-foreground">
            {isMafiaWin
              ? "The mafia has taken over the town."
              : "The town has eliminated all mafia members."}
          </p>
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
                  variant={player.role === "MAFIA" ? "destructive" : "secondary"}
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
