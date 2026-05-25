import { useEffect, useState } from "react";
import { LiveKitRoom, RoomAudioRenderer, useLocalParticipant } from "@livekit/components-react";
import { Button } from "../ui/button";
import { Mic, MicOff, Volume2, Loader2, AlertCircle } from "lucide-react";
import { toast } from "sonner";
import { api } from "../../lib/api";
import type { VoiceScope, VoiceTokenResponse } from "../../lib/types";

interface VoiceChatProps {
  scope: VoiceScope;
  id: number;
  /**
   * Bumped whenever the user's publish permission may have changed
   * (e.g. phase change, death) so the token gets refreshed and the
   * LiveKit server re-evaluates canPublish.
   */
  permissionsKey?: string;
  /** When false, the component renders nothing (used to fully detach from the room). */
  active?: boolean;
}

interface TokenState {
  token: string;
  url: string;
  roomName: string;
  canPublish: boolean;
}

/**
 * Generic voice-chat component. Fetches a LiveKit token for the given (scope, id),
 * connects to the room (audio-only), and exposes a mute toggle.
 *
 * The component re-fetches the token whenever (scope, id) changes — that's how we
 * cleanly switch a mafia player between the main and mafia rooms when the phase
 * flips. Re-mounting the inner LiveKitRoom by keying on scope+id ensures a clean
 * disconnect from the previous room.
 */
export function VoiceChat({ scope, id, permissionsKey, active = true }: VoiceChatProps) {
  const [tokenState, setTokenState] = useState<TokenState | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!active) {
      setTokenState(null);
      setError(null);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    api
      .post<VoiceTokenResponse>("/api/livekit/token", { scope, id })
      .then((data) => {
        if (cancelled) return;
        setTokenState({
          token: data.token,
          url: data.url,
          roomName: data.roomName,
          canPublish: data.canPublish,
        });
        setLoading(false);
      })
      .catch((e) => {
        if (cancelled) return;
        const msg = (e as Error).message || "Voice unavailable";
        setError(msg);
        setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [scope, id, active, permissionsKey]);

  if (!active) return null;

  if (loading) {
    return (
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <Loader2 className="w-3 h-3 animate-spin" />
        Connecting voice...
      </div>
    );
  }

  if (error || !tokenState) {
    return (
      <div className="flex items-center gap-2 text-xs text-muted-foreground" title={error ?? ""}>
        <AlertCircle className="w-3 h-3 text-yellow-500" />
        Voice unavailable
      </div>
    );
  }

  return (
    <LiveKitRoom
      key={`${scope}-${id}-${tokenState.roomName}`}
      token={tokenState.token}
      serverUrl={tokenState.url}
      audio={tokenState.canPublish}
      video={false}
      connect
      onError={(err) => {
        toast.error(`Voice error: ${err.message}`);
      }}
    >
      <RoomAudioRenderer />
      <VoiceControls canPublish={tokenState.canPublish} />
    </LiveKitRoom>
  );
}

interface VoiceControlsProps {
  canPublish: boolean;
}

function VoiceControls({ canPublish }: VoiceControlsProps) {
  const { localParticipant, isMicrophoneEnabled } = useLocalParticipant();
  const [busy, setBusy] = useState(false);

  // If the server says we can't publish, ensure mic is disabled
  useEffect(() => {
    if (!canPublish && isMicrophoneEnabled) {
      localParticipant.setMicrophoneEnabled(false).catch(() => {});
    }
  }, [canPublish, isMicrophoneEnabled, localParticipant]);

  const toggleMic = async () => {
    if (!canPublish || busy) return;
    setBusy(true);
    try {
      await localParticipant.setMicrophoneEnabled(!isMicrophoneEnabled);
    } catch (e) {
      toast.error(`Could not toggle microphone: ${(e as Error).message}`);
    } finally {
      setBusy(false);
    }
  };

  if (!canPublish) {
    return (
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <Volume2 className="w-3 h-3" />
        Listening only
      </div>
    );
  }

  return (
    <Button
      size="sm"
      variant={isMicrophoneEnabled ? "default" : "outline"}
      onClick={toggleMic}
      disabled={busy}
      className="h-8"
    >
      {isMicrophoneEnabled ? (
        <>
          <Mic className="w-4 h-4 mr-2" />
          Mic on
        </>
      ) : (
        <>
          <MicOff className="w-4 h-4 mr-2" />
          Mic off
        </>
      )}
    </Button>
  );
}
