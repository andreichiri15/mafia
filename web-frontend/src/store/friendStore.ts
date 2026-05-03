import { create } from "zustand";
import { api } from "../lib/api";
import { subscribe } from "../lib/websocket";
import type { FriendInfo, FriendRequestInfo, UserSearchResult } from "../lib/types";
import type { StompSubscription } from "@stomp/stompjs";

interface FriendStore {
  friends: FriendInfo[];
  requests: FriendRequestInfo[];
  loading: boolean;
  error: string | null;
  refreshSubscription: StompSubscription | null;
  pollIntervalId: ReturnType<typeof setInterval> | null;

  fetchFriends: () => Promise<void>;
  fetchRequests: () => Promise<void>;
  searchUsers: (query: string) => Promise<UserSearchResult[]>;
  sendRequest: (username: string) => Promise<void>;
  acceptRequest: (requestId: number) => Promise<void>;
  declineRequest: (requestId: number) => Promise<void>;
  removeFriend: (friendUserId: number) => Promise<void>;

  /** Subscribe to friend-update events + start status polling. Call once after STOMP is connected. */
  start: (userId: number) => void;
  stop: () => void;
}

export const useFriendStore = create<FriendStore>((set, get) => ({
  friends: [],
  requests: [],
  loading: false,
  error: null,
  refreshSubscription: null,
  pollIntervalId: null,

  fetchFriends: async () => {
    try {
      const friends = await api.get<FriendInfo[]>("/api/friends");
      set({ friends });
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  fetchRequests: async () => {
    try {
      const requests = await api.get<FriendRequestInfo[]>("/api/friends/requests");
      set({ requests });
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  searchUsers: async (query: string) => {
    if (!query.trim()) return [];
    try {
      return await api.get<UserSearchResult[]>(`/api/users/search?q=${encodeURIComponent(query)}`);
    } catch {
      return [];
    }
  },

  sendRequest: async (username: string) => {
    await api.post("/api/friends/request", { username });
    await get().fetchFriends();
  },

  acceptRequest: async (requestId: number) => {
    await api.post(`/api/friends/${requestId}/accept`);
    await Promise.all([get().fetchFriends(), get().fetchRequests()]);
  },

  declineRequest: async (requestId: number) => {
    await api.post(`/api/friends/${requestId}/decline`);
    await get().fetchRequests();
  },

  removeFriend: async (friendUserId: number) => {
    await api.delete(`/api/friends/${friendUserId}`);
    await get().fetchFriends();
  },

  start: (userId: number) => {
    const { stop, fetchFriends, fetchRequests } = get();
    stop();

    // Initial load
    fetchFriends();
    fetchRequests();

    // Subscribe to refresh notifications (incoming/accepted requests etc.)
    const sub = subscribe(`/topic/user/${userId}/friends-update`, () => {
      fetchFriends();
      fetchRequests();
    });

    // Poll for status changes (cheap and good enough)
    const pollId = setInterval(() => {
      fetchFriends();
    }, 8000);

    set({ refreshSubscription: sub, pollIntervalId: pollId });
  },

  stop: () => {
    const { refreshSubscription, pollIntervalId } = get();
    refreshSubscription?.unsubscribe();
    if (pollIntervalId) clearInterval(pollIntervalId);
    set({
      refreshSubscription: null,
      pollIntervalId: null,
      friends: [],
      requests: [],
    });
  },
}));
