import { useEffect } from "react";
import type { PhaseResultEvent } from "../../lib/types";

interface PhaseTransitionProps {
  result: PhaseResultEvent;
  onDismiss: () => void;
}

const phaseLabels: Record<string, string> = {
  NIGHT: "Night falls...",
  DAY: "A new day dawns",
  VOTING: "Time to vote!",
  GAME_OVER: "Game Over",
};

export function PhaseTransition({ result, onDismiss }: PhaseTransitionProps) {
  useEffect(() => {
    const timer = setTimeout(onDismiss, 4000);
    return () => clearTimeout(timer);
  }, [onDismiss]);

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center bg-black/60 cursor-pointer animate-in fade-in duration-300"
      onClick={onDismiss}
    >
      <div className="text-center space-y-4 max-w-md mx-4">
        <h2 className="text-3xl font-bold text-white">
          {phaseLabels[result.phase] || result.phase}
        </h2>
        <p className="text-white/80 text-sm">Round {result.round}</p>
        {result.events
          .filter((e) => !e.startsWith("ELIMINATED:"))
          .map((event, i) => (
            <p key={i} className="text-white/90 text-lg">
              {event}
            </p>
          ))}
      </div>
    </div>
  );
}
