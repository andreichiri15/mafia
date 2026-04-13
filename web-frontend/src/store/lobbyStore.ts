import { create } from "zustand";
import { api } from "../lib/api";
import { connectStomp, disconnectStomp, subscribe, getStompClient } from "../lib/websocket";
import { useChatStore } from "./chatStore";
import type { LobbySummary, LobbyDetail, CreateLobbyRequest } from "../lib/types";
import type { StompSubscription } from "@stomp/stompjs";

interface LobbyState {
  lobbies: LobbySummary[];
  currentLobby: LobbyDetail | null;
  loading: boolean;
  error: string | null;
  subscriptions: StompSubscription[];

  fetchLobbies: (searchName?: string) => Promise<void>;
  createLobby: (request: CreateLobbyRequest) => Promise<LobbyDetail>;
  fetchLobbyDetail: (lobbyId: number) => Promise<void>;
  joinLobby: (lobbyId: number, password?: string) => Promise<LobbyDetail>;
  leaveLobby: (lobbyId: number) => Promise<void>;
  toggleReady: (lobbyId: number) => Promise<void>;
  subscribeLobby: (lobbyId: number) => Promise<void>;
  unsubscribeLobby: () => void;
}

export const useLobbyStore = create<LobbyState>((set, get) => ({
  lobbies: [],
  currentLobby: null,
  loading: false,
  error: null,
  subscriptions: [],

  fetchLobbies: async (searchName?: string) => {
    set({ loading: true, error: null });
    try {
      const query = searchName ? `?searchName=${encodeURIComponent(searchName)}` : "";
      const lobbies = await api.get<LobbySummary[]>(`/api/lobbies${query}`);
      set({ lobbies, loading: false });
    } catch (e) {
      set({ error: (e as Error).message, loading: false });
    }
  },

  createLobby: async (request: CreateLobbyRequest) => {
    set({ loading: true, error: null });
    try {
      const lobby = await api.post<LobbyDetail>("/api/lobbies", request);
      set({ currentLobby: lobby, loading: false });
      return lobby;
    } catch (e) {
      set({ error: (e as Error).message, loading: false });
      throw e;
    }
  },

  fetchLobbyDetail: async (lobbyId: number) => {
    try {
      const lobby = await api.get<LobbyDetail>(`/api/lobbies/${lobbyId}`);
      set({ currentLobby: lobby, error: null });
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  joinLobby: async (lobbyId: number, password?: string) => {
    set({ loading: true, error: null });
    try {
      const lobby = await api.post<LobbyDetail>(`/api/lobbies/${lobbyId}/join`, { password });
      set({ currentLobby: lobby, loading: false });
      return lobby;
    } catch (e) {
      set({ error: (e as Error).message, loading: false });
      throw e;
    }
  },

  leaveLobby: async (lobbyId: number) => {
    try {
      await api.post(`/api/lobbies/${lobbyId}/leave`);
      get().unsubscribeLobby();
      set({ currentLobby: null });
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  toggleReady: async (lobbyId: number) => {
    try {
      const lobby = await api.put<LobbyDetail>(`/api/lobbies/${lobbyId}/ready`);
      set({ currentLobby: lobby });
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  subscribeLobby: async (lobbyId: number) => {
    // Fetch initial data
    await get().fetchLobbyDetail(lobbyId);

    try {
      await connectStomp();
    } catch {
      // Fall back to polling if WebSocket fails
      return;
    }

    const subs: StompSubscription[] = [];

    // Subscribe to player list updates
    const playerSub = subscribe(`/topic/lobby/${lobbyId}/players`, (message) => {
      const lobbyDetail: LobbyDetail = JSON.parse(message.body);
      set({ currentLobby: lobbyDetail });
    });
    if (playerSub) subs.push(playerSub);

    // Subscribe to chat messages
    const chatSub = subscribe(`/topic/lobby/${lobbyId}/chat`, (message) => {
      const chatMsg = JSON.parse(message.body);
      useChatStore.getState().addMessage(chatMsg);
    });
    if (chatSub) subs.push(chatSub);

    set({ subscriptions: subs });
  },

  unsubscribeLobby: () => {
    const { subscriptions } = get();
    subscriptions.forEach((sub) => sub.unsubscribe());
    set({ subscriptions: [] });
    useChatStore.getState().clearMessages();
    disconnectStomp();
  },
}));
