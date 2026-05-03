import { Avatar, AvatarFallback } from "../ui/avatar";
import { Badge } from "../ui/badge";
import type { GamePlayerInfo, GamePhase, Role } from "../../lib/types";

interface PlayerGridProps {
  players: GamePlayerInfo[];
  phase: GamePhase;
  yourRole: Role;
  alive: boolean;
  selectedTarget: number | null;
  onSelectTarget: (userId: number) => void;
}

export function PlayerGrid({ players, phase, yourRole, alive, selectedTarget, onSelectTarget }: PlayerGridProps) {
  const canTarget = alive && (
    (phase === "NIGHT" && (
      yourRole === "MAFIA" ||
      yourRole === "SHERIFF" ||
      yourRole === "DOCTOR" ||
      yourRole === "MUTILATOR"
    )) ||
    phase === "VOTING"
  );

  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
      {players.map((player) => {
        const isTargetable = canTarget && player.alive;
        const isSelected = selectedTarget === player.userId;
        const isDead = !player.alive;

        return (
          <button
            key={player.userId}
            onClick={() => isTargetable && onSelectTarget(player.userId)}
            disabled={!isTargetable || isDead}
            className={`
              flex flex-col items-center gap-2 p-3 rounded-lg border transition-all
              ${isDead ? "opacity-40 cursor-default" : ""}
              ${isTargetable && !isDead ? "cursor-pointer hover:border-primary hover:bg-accent" : ""}
              ${isSelected ? "border-primary bg-primary/10 ring-2 ring-primary" : "border-border"}
            `}
          >
            <div className="relative">
              <Avatar className="w-12 h-12">
                <AvatarFallback className={isDead ? "bg-muted" : ""}>
                  {player.username[0].toUpperCase()}
                </AvatarFallback>
              </Avatar>
              {isDead && (
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-2xl">&#x2620;</span>
                </div>
              )}
            </div>
            <span className={`text-sm font-medium truncate w-full text-center ${isDead ? "line-through" : ""}`}>
              {player.username}
            </span>
            {player.role && (
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
            )}
          </button>
        );
      })}
    </div>
  );
}
