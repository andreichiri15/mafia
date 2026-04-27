import { useEffect, useState } from "react";
import { Moon, Sun, Vote } from "lucide-react";
import type { GamePhase } from "../../lib/types";

interface PhaseHeaderProps {
  phase: GamePhase;
  round: number;
  phaseEndTime: number;
}

const phaseConfig: Record<string, { label: string; icon: typeof Moon; bg: string }> = {
  NIGHT: { label: "Night", icon: Moon, bg: "bg-slate-900 text-slate-100" },
  DAY: { label: "Day", icon: Sun, bg: "bg-amber-100 text-amber-900" },
  VOTING: { label: "Voting", icon: Vote, bg: "bg-red-100 text-red-900" },
  GAME_OVER: { label: "Game Over", icon: Sun, bg: "bg-gray-200 text-gray-800" },
};

export function PhaseHeader({ phase, round, phaseEndTime }: PhaseHeaderProps) {
  const [timeLeft, setTimeLeft] = useState(0);
  const config = phaseConfig[phase] || phaseConfig.DAY;
  const Icon = config.icon;

  useEffect(() => {
    if (!phaseEndTime || phase === "GAME_OVER") {
      setTimeLeft(0);
      return;
    }

    const tick = () => {
      const remaining = Math.max(0, Math.floor((phaseEndTime - Date.now()) / 1000));
      setTimeLeft(remaining);
    };

    tick();
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
  }, [phaseEndTime, phase]);

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;

  return (
    <div className={`flex items-center justify-between px-6 py-3 rounded-lg ${config.bg}`}>
      <div className="flex items-center gap-3">
        <Icon className="w-5 h-5" />
        <span className="font-semibold text-lg">{config.label}</span>
        <span className="opacity-70">Round {round}</span>
      </div>
      {phase !== "GAME_OVER" && (
        <span className="font-mono text-xl font-bold">
          {minutes}:{seconds.toString().padStart(2, "0")}
        </span>
      )}
    </div>
  );
}
