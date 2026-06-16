import { VoiceChat } from "./VoiceChat";
import { useGameStore } from "../../store/gameStore";

interface GameVoiceChatProps {
  gameId: number;
}

/**
 * Game-specific voice wrapper. Reads the current phase and the player's role
 * from gameStore to decide which LiveKit room to join:
 *
 *  - DAY / VOTING  → main game room ({gameId})
 *  - NIGHT + MAFIA → mafia room ({gameId}-mafia)
 *  - NIGHT, other  → main room (mics off effectively, since villagers don't talk at night)
 *
 * Dead players still connect to the main room as listeners — the backend
 * returns canPublish=false in their token, and VoiceChat auto-disables the mic.
 */
export function GameVoiceChat({ gameId }: GameVoiceChatProps) {
  const gameState = useGameStore((s) => s.gameState);
  const gameOver = useGameStore((s) => s.gameOver);

  if (!gameState) return null;
  if (gameOver) return null;

  const useMafiaRoom =
    gameState.phase === "NIGHT" && gameState.yourRole === "MAFIA";

  const permissionsKey = `${gameState.phase}-${gameState.alive ? "alive" : "dead"}`;

  return (
    <VoiceChat
      scope={useMafiaRoom ? "GAME_MAFIA" : "GAME_MAIN"}
      id={gameId}
      permissionsKey={permissionsKey}
    />
  );
}
