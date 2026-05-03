import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Loader2 } from "lucide-react";
import { Button } from "./ui/button";
import { useGameStore } from "../store/gameStore";
import { useAuthStore } from "../store/authStore";
import { PhaseHeader } from "./game/PhaseHeader";
import { PlayerGrid } from "./game/PlayerGrid";
import { ActionPanel } from "./game/ActionPanel";
import { GameChat } from "./game/GameChat";
import { RoleCard } from "./game/RoleCard";
import { PhaseTransition } from "./game/PhaseTransition";
import { GameOverScreen } from "./game/GameOverScreen";

export function GamePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const gameId = Number(id);
  const user = useAuthStore((s) => s.user);

  const {
    gameState,
    phaseResult,
    gameOver,
    investigationResult,
    showRoleReveal,
    showPhaseTransition,
    chatMessages,
    loading,
    error,
    subscribeGame,
    unsubscribeGame,
    submitAction,
    sendChat,
    dismissRoleReveal,
    dismissPhaseTransition,
    dismissInvestigationResult,
  } = useGameStore();

  const [selectedTarget, setSelectedTarget] = useState<number | null>(null);
  const [actionSubmitted, setActionSubmitted] = useState(false);

  // Reset action state on phase change
  useEffect(() => {
    setSelectedTarget(null);
    setActionSubmitted(false);
  }, [gameState?.phase, gameState?.round]);

  useEffect(() => {
    if (!gameId || !user) return;
    subscribeGame(gameId, user.userId);
    return () => unsubscribeGame();
  }, [gameId, user, subscribeGame, unsubscribeGame]);

  const handleConfirmAction = (actionType: string) => {
    if (!selectedTarget || !gameState) return;
    submitAction(gameId, selectedTarget, actionType);
    setActionSubmitted(true);
    setSelectedTarget(null);
  };

  const handleSendChat = (channel: string, message: string) => {
    sendChat(gameId, channel, message);
  };

  if (!gameState && loading) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-4rem)]">
        <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!gameState && error) {
    return (
      <div className="flex flex-col items-center justify-center h-[calc(100vh-4rem)] gap-4">
        <p className="text-red-500">{error}</p>
        <Button onClick={() => navigate("/play")}>Back to Servers</Button>
      </div>
    );
  }

  if (!gameState) {
    return (
      <div className="flex flex-col items-center justify-center h-[calc(100vh-4rem)] gap-4">
        <p className="text-muted-foreground">Game not found</p>
        <Button onClick={() => navigate("/play")}>Back to Servers</Button>
      </div>
    );
  }

  const selectedTargetName = gameState.players?.find(
    (p) => p.userId === selectedTarget
  )?.username ?? null;

  return (
    <>
      {/* Overlays */}
      {showRoleReveal && gameState.yourRole && (
        <RoleCard role={gameState.yourRole} onDismiss={dismissRoleReveal} />
      )}
      {showPhaseTransition && phaseResult && (
        <PhaseTransition result={phaseResult} onDismiss={dismissPhaseTransition} />
      )}
      {investigationResult && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 cursor-pointer"
          onClick={dismissInvestigationResult}
        >
          <div className="bg-card border rounded-lg p-6 max-w-sm mx-4 text-center space-y-3">
            <p className="text-sm text-muted-foreground">Investigation Result</p>
            <p className="text-2xl font-bold">
              {investigationResult.target} is{" "}
              <span className={investigationResult.result === "MAFIA" ? "text-red-500" : "text-green-500"}>
                {investigationResult.result === "MAFIA" ? "MAFIA" : "NOT mafia"}
              </span>
            </p>
            <Button onClick={dismissInvestigationResult} className="w-full">Got it</Button>
          </div>
        </div>
      )}
      {gameOver && <GameOverScreen result={gameOver} />}

      {/* Main layout */}
      <div className="container mx-auto px-4 py-4 h-[calc(100vh-4rem)] flex flex-col gap-4">
        <PhaseHeader
          phase={gameState.phase}
          round={gameState.round}
          phaseEndTime={gameState.phaseEndTime}
        />

        <div className="flex-1 grid lg:grid-cols-3 gap-4 min-h-0">
          {/* Left: Players + Actions */}
          <div className="lg:col-span-2 flex flex-col gap-4">
            <Card className="flex-1">
              <CardHeader className="py-3">
                <CardTitle className="text-base">Players</CardTitle>
              </CardHeader>
              <CardContent>
                <PlayerGrid
                  players={gameState.players}
                  phase={gameState.phase}
                  yourRole={gameState.yourRole}
                  alive={gameState.alive}
                  selectedTarget={selectedTarget}
                  onSelectTarget={setSelectedTarget}
                />
              </CardContent>
            </Card>

            <Card>
              <CardContent className="py-3">
                <ActionPanel
                  phase={gameState.phase}
                  yourRole={gameState.yourRole}
                  alive={gameState.alive}
                  selectedTarget={selectedTarget}
                  selectedTargetName={selectedTargetName}
                  onConfirmAction={handleConfirmAction}
                  actionSubmitted={actionSubmitted}
                />
              </CardContent>
            </Card>
          </div>

          {/* Right: Chat */}
          <Card className="flex flex-col min-h-0">
            <CardHeader className="py-3">
              <CardTitle className="text-base">Chat</CardTitle>
            </CardHeader>
            <CardContent className="flex-1 min-h-0">
              <GameChat
                phase={gameState.phase}
                yourRole={gameState.yourRole}
                alive={gameState.alive}
                dayMessages={chatMessages.day}
                mafiaMessages={chatMessages.mafia}
                deadMessages={chatMessages.dead}
                onSendMessage={handleSendChat}
              />
            </CardContent>
          </Card>
        </div>
      </div>
    </>
  );
}
