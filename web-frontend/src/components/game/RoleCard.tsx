import { Button } from "../ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import type { Role } from "../../lib/types";

interface RoleCardProps {
  role: Role;
  onDismiss: () => void;
}

const roleInfo: Record<Role, { title: string; description: string; color: string }> = {
  MAFIA: {
    title: "Mafia",
    description: "Eliminate the villagers one by one. During the night, choose a target to kill. Stay hidden during the day.",
    color: "text-red-500",
  },
  VILLAGER: {
    title: "Villager",
    description: "Find and vote out the mafia members. Discuss during the day, vote during voting phase. Survive the night.",
    color: "text-blue-500",
  },
  SHERIFF: {
    title: "Sheriff",
    description: "You can investigate one player each night to learn if they are mafia. Use this knowledge to guide the town.",
    color: "text-yellow-500",
  },
  DOCTOR: {
    title: "Doctor",
    description: "Each night, choose a player to protect. If the mafia attacks them, they survive. You may have a limit on self-saves.",
    color: "text-emerald-500",
  },
  JESTER: {
    title: "Jester",
    description: "You win if the town votes you out. Cause chaos, act suspicious, but don't get killed by the mafia at night.",
    color: "text-purple-500",
  },
  MUTILATOR: {
    title: "Mutilator",
    description: "You side with the mafia. Each night, pick a player and either silence them for the day or revoke their vote.",
    color: "text-orange-500",
  },
};

export function RoleCard({ role, onDismiss }: RoleCardProps) {
  const info = roleInfo[role];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70">
      <Card className="w-full max-w-sm mx-4 animate-in fade-in zoom-in duration-300">
        <CardHeader className="text-center">
          <CardTitle className="text-sm text-muted-foreground">Your Role</CardTitle>
          <p className={`text-4xl font-bold ${info.color}`}>{info.title}</p>
        </CardHeader>
        <CardContent className="text-center space-y-4">
          <p className="text-muted-foreground">{info.description}</p>
          <Button onClick={onDismiss} className="w-full">
            Got it
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
