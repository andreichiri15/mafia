import { create } from "zustand";

export interface ChatMessage {
  id: string | number;
  player: string;
  message: string;
  timestamp: string;
  channel?: string;
}

interface ChatState {
  messages: ChatMessage[];
  addMessage: (msg: ChatMessage) => void;
  setMessages: (msgs: ChatMessage[]) => void;
  clearMessages: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],

  addMessage: (msg) =>
    set((state) => {
      // Dedup by id — historical fetch + live broadcast can deliver the same message
      if (state.messages.some((m) => String(m.id) === String(msg.id))) return state;
      // Insert in timestamp order to keep chronological display when history arrives after live messages
      const next = [...state.messages, msg].sort((a, b) =>
        a.timestamp < b.timestamp ? -1 : a.timestamp > b.timestamp ? 1 : 0
      );
      return { messages: next };
    }),

  setMessages: (msgs) => set({ messages: msgs }),

  clearMessages: () => set({ messages: [] }),
}));
