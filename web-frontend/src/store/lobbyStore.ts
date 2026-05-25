import { create } from "zustand";
import { api } from "../lib/api";
import { connectStomp, disconnectStomp, subscribe } from "../lib/websocket";
import { useChatStore, type ChatMessage } from "./chatStore";
import type { LobbySummary, LobbyDetail, CreateLobbyRequest, GameSettings, InviteResolution } from "../lib/types";
import type { StompSubscription } from "@stomp/stompjs";

interface LobbyState {
  lobbies: LobbySummary[];
  currentLobby: LobbyDetail | null;
  loading: boolean;
  error: string | null;
  subscriptions: StompSubscription[];
  startedGameId: number | null;
  closed: boolean;

  fetchLobbies: (searchName?: string) => Promise<void>;
  createLobby: (request: CreateLobbyRequest) => Promise<LobbyDetail>;
  fetchLobbyDetail: (lobbyId: number) => Promise<void>;
  joinLobby: (lobbyId: number, password?: string) => Promise<LobbyDetail>;
  leaveLobby: (lobbyId: number) => Promise<void>;
  toggleReady: (lobbyId: number) => Promise<void>;
  updateSettings: (lobbyId: number, settings: GameSettings) => Promise<void>;
  resolveInvite: (token: string) => Promise<InviteResolution>;
  inviteFriend: (lobbyId: number, friendUserId: number) => Promise<void>;
  addBot: (lobbyId: number) => Promise<void>;
  removeBot: (lobbyId: number, botUserId: number) => Promise<void>;
  subscribeLobby: (lobbyId: number) => Promise<void>;
  unsubscribeLobby: () => void;
  clearStartedGameId: () => void;
}

export const useLobbyStore = create<LobbyState>((set, get) => ({
  lobbies: [],
  currentLobby: null,
  loading: false,
  error: null,
  subscriptions: [],
  startedGameId: null,
  closed: false,

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

  updateSettings: async (lobbyId: number, settings: GameSettings) => {
    try {
      const lobby = await api.put<LobbyDetail>(`/api/lobbies/${lobbyId}/settings`, settings);
      set({ currentLobby: lobby });
    } catch (e) {
      set({ error: (e as Error).message });
      throw e;
    }
  },

  resolveInvite: async (token: string) => {
    return api.get<InviteResolution>(`/api/lobbies/invite/${encodeURIComponent(token)}`);
  },

  inviteFriend: async (lobbyId: number, friendUserId: number) => {
    await api.post(`/api/lobbies/${lobbyId}/invite/${friendUserId}`);
  },

  addBot: async (lobbyId: number) => {
    try {
      const lobby = await api.post<LobbyDetail>(`/api/lobbies/${lobbyId}/bots`);
      set({ currentLobby: lobby });
    } catch (e) {
      set({ error: (e as Error).message });
      throw e;
    }
  },

  removeBot: async (lobbyId: number, botUserId: number) => {
    try {
      const lobby = await api.delete<LobbyDetail>(`/api/lobbies/${lobbyId}/bots/${botUserId}`);
      set({ currentLobby: lobby });
    } catch (e) {
      set({ error: (e as Error).message });
      throw e;
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

    // Subscribe to chat messages BEFORE fetching history, so live messages
    // arriving during the fetch aren't missed (dedup handles overlap)
    const chatSub = subscribe(`/topic/lobby/${lobbyId}/chat`, (message) => {
      const chatMsg = JSON.parse(message.body);
      useChatStore.getState().addMessage(chatMsg);
    });
    if (chatSub) subs.push(chatSub);

    // Hydrate chat with persisted history
    api
      .get<ChatMessage[]>(`/api/lobbies/${lobbyId}/messages`)
      .then((history) => {
        history.forEach((m) => useChatStore.getState().addMessage(m));
      })
      .catch(() => {
        // History fetch failed; live messages will still flow
      });

    // Subscribe to game-start so all players can navigate when host starts
    const gameStartSub = subscribe(`/topic/lobby/${lobbyId}/game-start`, (message) => {
      const event = JSON.parse(message.body);
      set({ startedGameId: event.gameId });
    });
    if (gameStartSub) subs.push(gameStartSub);

    // Subscribe to lobby-closed (host left) so we can redirect remaining players
    const closedSub = subscribe(`/topic/lobby/${lobbyId}/closed`, () => {
      set({ closed: true });
    });
    if (closedSub) subs.push(closedSub);

    set({ subscriptions: subs });
  },

  unsubscribeLobby: () => {
    const { subscriptions } = get();
    subscriptions.forEach((sub) => sub.unsubscribe());
    set({ subscriptions: [], startedGameId: null, closed: false, currentLobby: null });
    useChatStore.getState().clearMessages();
    disconnectStomp();
  },

  clearStartedGameId: () => set({ startedGameId: null }),
}));
