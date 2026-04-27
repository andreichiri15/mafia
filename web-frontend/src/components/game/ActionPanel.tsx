import { Button } from "../ui/button";
import type { GamePhase, Role } from "../../lib/types";

interface ActionPanelProps {
  phase: GamePhase;
  yourRole: Role;
  alive: boolean;
  selectedTarget: number | null;
  selectedTargetName: string | null;
  onConfirmAction: () => void;
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
          <p className="text-sm text-muted-foreground">
            Choose a player to eliminate
          </p>
          {selectedTarget && (
            <Button onClick={onConfirmAction} variant="destructive">
              Kill {selectedTargetName}
            </Button>
          )}
        </div>
      );
    }
    if (yourRole === "SHERIFF") {
      return (
        <div className="flex flex-col items-center gap-3 py-4">
          <p className="text-sm text-muted-foreground">
            Choose a player to investigate
          </p>
          {selectedTarget && (
            <Button onClick={onConfirmAction}>
              Investigate {selectedTargetName}
            </Button>
          )}
        </div>
      );
    }
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
        <p className="text-sm text-muted-foreground">
          Vote for a player to eliminate
        </p>
        {selectedTarget && (
          <Button onClick={onConfirmAction} variant="destructive">
            Vote for {selectedTargetName}
          </Button>
        )}
      </div>
    );
  }

  return null;
}
