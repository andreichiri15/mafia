import { useEffect, useState } from "react";
import {
  LiveKitRoom,
  RoomAudioRenderer,
  useLocalParticipant,
  useSpeakingParticipants,
} from "@livekit/components-react";
import { Button } from "../ui/button";
import { Mic, MicOff, Volume2, Loader2, AlertCircle } from "lucide-react";
import { toast } from "sonner";
import { api } from "../../lib/api";
import { useVoiceStore } from "../../store/voiceStore";
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
      <SpeakingTracker />
      <VoiceControls canPublish={tokenState.canPublish} />
    </LiveKitRoom>
  );
}

/**
 * Lives inside the LiveKit context; publishes speaking-state into voiceStore
 * so player UIs can render a ring around speaking avatars.
 */
function SpeakingTracker() {
  const speakers = useSpeakingParticipants();
  const setSpeakingUserIds = useVoiceStore((s) => s.setSpeakingUserIds);

  useEffect(() => {
    const ids = new Set<number>();
    for (const p of speakers) {
      const id = parseInt(p.identity, 10);
      if (!Number.isNaN(id)) ids.add(id);
    }
    setSpeakingUserIds(ids);
  }, [speakers, setSpeakingUserIds]);

  // Clear when the voice component unmounts (e.g. user leaves the page)
  useEffect(() => {
    return () => {
      setSpeakingUserIds(new Set());
    };
  }, [setSpeakingUserIds]);

  return null;
}

interface VoiceControlsProps {
  canPublish: boolean;
}

function VoiceControls({ canPublish }: VoiceControlsProps) {
  const { localParticipant, isMicrophoneEnabled } = useLocalParticipant();
  const userWantsMuted = useVoiceStore((s) => s.userWantsMuted);
  const setUserWantsMuted = useVoiceStore((s) => s.setUserWantsMuted);

  // Single source of truth: actual mic state = canPublish AND user wants it on.
  // This effect reconciles after any of: room (re)connect, permission change, user toggle.
  useEffect(() => {
    const shouldBeOn = canPublish && !userWantsMuted;
    if (isMicrophoneEnabled !== shouldBeOn) {
      localParticipant.setMicrophoneEnabled(shouldBeOn).catch(() => {});
    }
  }, [canPublish, userWantsMuted, isMicrophoneEnabled, localParticipant]);

  const toggleMic = () => {
    if (!canPublish) return; // server forbids publishing — can't unmute
    setUserWantsMuted(!userWantsMuted);
  };

  if (!canPublish) {
    return (
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <Volume2 className="w-3 h-3" />
        Listening only
      </div>
    );
  }

  const micOn = !userWantsMuted;

  return (
    <Button
      size="sm"
      variant={micOn ? "default" : "outline"}
      onClick={toggleMic}
      className="h-8"
    >
      {micOn ? (
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
