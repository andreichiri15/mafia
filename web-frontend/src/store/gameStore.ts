import { create } from "zustand";
import { api } from "../lib/api";
import { connectStomp, subscribe, sendMessage, disconnectStomp } from "../lib/websocket";
import type {
  GameState,
  PhaseResultEvent,
  GameOverEvent,
  GameStartEvent,
} from "../lib/types";
import type { StompSubscription } from "@stomp/stompjs";
import type { ChatMessage } from "./chatStore";

interface GameChatMessages {
  day: ChatMessage[];
  mafia: ChatMessage[];
  dead: ChatMessage[];
}

interface InvestigationResult {
  target: string;
  result: "MAFIA" | "NOT_MAFIA";
}

interface GameStore {
  gameState: GameState | null;
  phaseResult: PhaseResultEvent | null;
  gameOver: GameOverEvent | null;
  investigationResult: InvestigationResult | null;
  showRoleReveal: boolean;
  showPhaseTransition: boolean;
  chatMessages: GameChatMessages;
  subscriptions: StompSubscription[];
  loading: boolean;
  error: string | null;

  startGame: (lobbyId: number) => Promise<GameStartEvent>;
  fetchGameState: (gameId: number) => Promise<void>;
  subscribeGame: (gameId: number, userId: number) => Promise<void>;
  unsubscribeGame: () => void;
  submitAction: (gameId: number, targetUserId: number, actionType: string) => void;
  sendChat: (gameId: number, channel: string, message: string) => void;
  dismissRoleReveal: () => void;
  dismissPhaseTransition: () => void;
  dismissInvestigationResult: () => void;
}

export const useGameStore = create<GameStore>((set, get) => ({
  gameState: null,
  phaseResult: null,
  gameOver: null,
  investigationResult: null,
  showRoleReveal: false,
  showPhaseTransition: false,
  chatMessages: { day: [], mafia: [], dead: [] },
  subscriptions: [],
  loading: false,
  error: null,

  startGame: async (lobbyId: number) => {
    set({ loading: true, error: null });
    try {
      const event = await api.post<GameStartEvent>(`/api/lobbies/${lobbyId}/start`);
      set({ loading: false });
      return event;
    } catch (e) {
      set({ error: (e as Error).message, loading: false });
      throw e;
    }
  },

  fetchGameState: async (gameId: number) => {
    try {
      const state = await api.get<GameState>(`/api/games/${gameId}`);
      set({ gameState: state });
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  subscribeGame: async (gameId: number, userId: number) => {
    await get().fetchGameState(gameId);

    // Show role reveal on first load
    set({ showRoleReveal: true });

    try {
      await connectStomp();
    } catch {
      return;
    }

    const subs: StompSubscription[] = [];

    // Per-player state updates (includes role visibility)
    const stateSub = subscribe(`/topic/game/${gameId}/state/${userId}`, (msg) => {
      const state: GameState = JSON.parse(msg.body);
      set({ gameState: state });
    });
    if (stateSub) subs.push(stateSub);

    // Phase transitions
    const phaseSub = subscribe(`/topic/game/${gameId}/phase`, (msg) => {
      const result: PhaseResultEvent = JSON.parse(msg.body);
      set({ phaseResult: result, showPhaseTransition: true });
    });
    if (phaseSub) subs.push(phaseSub);

    // Game over
    const gameOverSub = subscribe(`/topic/game/${gameId}/game-over`, (msg) => {
      const over: GameOverEvent = JSON.parse(msg.body);
      set({ gameOver: over });
    });
    if (gameOverSub) subs.push(gameOverSub);

    // Private channel (e.g. investigation results)
    const privateSub = subscribe(`/topic/game/${gameId}/private/${userId}`, (msg) => {
      const data = JSON.parse(msg.body);
      if (data.type === "INVESTIGATION_RESULT") {
        set({ investigationResult: { target: data.target, result: data.result } });
      }
    });
    if (privateSub) subs.push(privateSub);

    // Chat channels
    const channels = ["day", "mafia", "dead"] as const;
    for (const channel of channels) {
      const chatSub = subscribe(`/topic/game/${gameId}/chat/${channel}`, (msg) => {
        const chatMsg: ChatMessage = JSON.parse(msg.body);
        set((state) => ({
          chatMessages: {
            ...state.chatMessages,
            [channel]: [...state.chatMessages[channel], chatMsg],
          },
        }));
      });
      if (chatSub) subs.push(chatSub);
    }

    set({ subscriptions: subs });
  },

  unsubscribeGame: () => {
    const { subscriptions } = get();
    subscriptions.forEach((sub) => sub.unsubscribe());
    set({
      subscriptions: [],
      gameState: null,
      phaseResult: null,
      gameOver: null,
      investigationResult: null,
      showRoleReveal: false,
      showPhaseTransition: false,
      chatMessages: { day: [], mafia: [], dead: [] },
    });
    disconnectStomp();
  },

  submitAction: (gameId: number, targetUserId: number, actionType: string) => {
    sendMessage(`/app/game/${gameId}/action`, {
      targetUserId: String(targetUserId),
      actionType,
    });
  },

  sendChat: (gameId: number, channel: string, message: string) => {
    sendMessage(`/app/game/${gameId}/chat/${channel}`, { message });
  },

  dismissRoleReveal: () => set({ showRoleReveal: false }),
  dismissPhaseTransition: () => set({ showPhaseTransition: false }),
  dismissInvestigationResult: () => set({ investigationResult: null }),
}));
