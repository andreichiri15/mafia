import { create } from "zustand";
import { api } from "../lib/api";
import { sendMessage, subscribe } from "../lib/websocket";
import type { DirectMessageInfo } from "../lib/types";
import type { StompSubscription } from "@stomp/stompjs";

interface DmStore {
  /** Conversations keyed by the other user's id */
  conversations: Record<number, DirectMessageInfo[]>;
  loaded: Record<number, boolean>;
  subscription: StompSubscription | null;

  loadConversation: (otherUserId: number) => Promise<void>;
  sendDm: (receiverId: number, content: string) => void;

  /** Subscribe to incoming DM stream for the current user. Call once after STOMP is connected. */
  start: (currentUserId: number) => void;
  stop: () => void;
}

export const useDmStore = create<DmStore>((set, get) => ({
  conversations: {},
  loaded: {},
  subscription: null,

  loadConversation: async (otherUserId: number) => {
    try {
      const messages = await api.get<DirectMessageInfo[]>(`/api/dm/${otherUserId}`);
      set((state) => ({
        conversations: { ...state.conversations, [otherUserId]: messages },
        loaded: { ...state.loaded, [otherUserId]: true },
      }));
    } catch {
      // ignore
    }
  },

  sendDm: (receiverId: number, content: string) => {
    sendMessage("/app/dm", { receiverId, content });
  },

  start: (currentUserId: number) => {
    const { stop } = get();
    stop();

    const sub = subscribe(`/topic/user/${currentUserId}/dm`, (msg) => {
      const dm: DirectMessageInfo = JSON.parse(msg.body);
      const otherUserId = dm.senderId === currentUserId ? dm.receiverId : dm.senderId;

      set((state) => {
        const existing = state.conversations[otherUserId] || [];
        // Avoid duplicates if both broadcasts hit the same client (sender == receiver impossible in our flow)
        if (existing.some((m) => m.id === dm.id)) return state;
        return {
          conversations: {
            ...state.conversations,
            [otherUserId]: [...existing, dm],
          },
        };
      });
    });

    set({ subscription: sub });
  },

  stop: () => {
    const { subscription } = get();
    subscription?.unsubscribe();
    set({ subscription: null, conversations: {}, loaded: {} });
  },
}));
