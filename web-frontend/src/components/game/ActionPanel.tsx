import { Button } from "../ui/button";
import type { GamePhase, Role } from "../../lib/types";

interface ActionPanelProps {
  phase: GamePhase;
  yourRole: Role;
  alive: boolean;
  selectedTarget: number | null;
  selectedTargetName: string | null;
  onConfirmAction: (actionType: string) => void;
  actionSubmitted: boolean;
}

export function ActionPanel({
  phase,
  yourRole,
  alive,
  selectedTarget,
  selectedTargetName,
  onConfirmAction,
  actionSubmitted,
}: ActionPanelProps) {
  if (!alive) {
    return (
      <div className="text-center py-4 text-muted-foreground">
        You are dead. Watch the game unfold...
      </div>
    );
  }

  if (actionSubmitted) {
    return (
      <div className="text-center py-4 text-muted-foreground">
        Action submitted. Waiting for others...
      </div>
    );
  }

  if (phase === "NIGHT") {
    if (yourRole === "MAFIA") {
      return (
        <div className="flex flex-col items-center gap-3 py-4">
          <p className="text-sm text-muted-foreground">Choose a player to eliminate</p>
          {selectedTarget && (
            <Button onClick={() => onConfirmAction("MAFIA_KILL")} variant="destructive">
              Kill {selectedTargetName}
            </Button>
          )}
        </div>
      );
    }
    if (yourRole === "SHERIFF") {
      return (
        <div className="flex flex-col items-center gap-3 py-4">
          <p className="text-sm text-muted-foreground">Choose a player to investigate</p>
          {selectedTarget && (
            <Button onClick={() => onConfirmAction("INVESTIGATE")}>
              Investigate {selectedTargetName}
            </Button>
          )}
        </div>
      );
    }
    if (yourRole === "DOCTOR") {
      return (
        <div className="flex flex-col items-center gap-3 py-4">
          <p className="text-sm text-muted-foreground">Choose a player to protect</p>
          {selectedTarget && (
            <Button
              onClick={() => onConfirmAction("HEALED")}
              className="bg-emerald-600 hover:bg-emerald-700"
            >
              Heal {selectedTargetName}
            </Button>
          )}
        </div>
      );
    }
    if (yourRole === "MUTILATOR") {
      return (
        <div className="flex flex-col items-center gap-3 py-4">
          <p className="text-sm text-muted-foreground">
            Choose a target, then pick an effect
          </p>
          {selectedTarget && (
            <div className="flex gap-2">
              <Button
                onClick={() => onConfirmAction("MUTE")}
                variant="outline"
                className="border-orange-500 text-orange-500 hover:bg-orange-500/10"
              >
                Silence {selectedTargetName}
              </Button>
              <Button
                onClick={() => onConfirmAction("REVOKE_VOTE")}
                variant="outline"
                className="border-orange-500 text-orange-500 hover:bg-orange-500/10"
              >
                Revoke vote
              </Button>
            </div>
          )}
        </div>
      );
    }
    // VILLAGER, JESTER — no night action
    return (
      <div className="text-center py-4 text-muted-foreground">
        Night time. The village sleeps...
      </div>
    );
  }

  if (phase === "DAY") {
    return (
      <div className="text-center py-4 text-muted-foreground">
        Discuss with the town. Voting begins soon.
      </div>
    );
  }

  if (phase === "VOTING") {
    return (
      <div className="flex flex-col items-center gap-3 py-4">
        <p className="text-sm text-muted-foreground">Vote for a player to eliminate</p>
        {selectedTarget && (
          <Button onClick={() => onConfirmAction("VOTE")} variant="destructive">
            Vote for {selectedTargetName}
          </Button>
        )}
      </div>
    );
  }

  return null;
}
