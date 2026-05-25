import { VoiceChat } from "./VoiceChat";

interface LobbyVoiceChatProps {
  lobbyId: number;
}

export function LobbyVoiceChat({ lobbyId }: LobbyVoiceChatProps) {
  return <VoiceChat scope="LOBBY" id={lobbyId} />;
}
