import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { useAuthStore } from "../store/authStore";
import { acquireStomp, releaseStomp, subscribe } from "../lib/websocket";
import type { LobbyInvite } from "../lib/types";
import type { StompSubscription } from "@stomp/stompjs";

/**
 * Mounted at the top of the app while the user is logged in.
 * Subscribes to the user's lobby-invite topic and shows a toast for each
 * incoming invite, with an action to join the lobby.
 */
export function InviteListener() {
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();

  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    let sub: StompSubscription | null = null;

    acquireStomp()
      .then(() => {
        if (cancelled) return;
        sub = subscribe(`/topic/user/${user.userId}/lobby-invite`, (msg) => {
          const invite: LobbyInvite = JSON.parse(msg.body);
          toast(`${invite.inviterUsername} invited you to "${invite.lobbyName}"`, {
            action: {
              label: "Join",
              onClick: () => {
                const target = invite.inviteToken
                  ? `/invite/${invite.inviteToken}`
                  : `/lobby/${invite.lobbyId}`;
                navigate(target);
              },
            },
            duration: 15000,
          });
        });
      })
      .catch(() => {
        // STOMP failed to connect; silently skip
      });

    return () => {
      cancelled = true;
      sub?.unsubscribe();
      releaseStomp();
    };
  }, [user, navigate]);

  return null;
}
