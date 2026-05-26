import { create } from "zustand";

interface VoiceStore {
  /** User IDs of participants currently speaking (set by SpeakingTracker inside LiveKit). */
  speakingUserIds: Set<number>;
  setSpeakingUserIds: (ids: Set<number>) => void;

  /**
   * The user's manual mute preference. Persists across phase changes and room
   * switches (e.g. mafia going from main to mafia room), so they stay muted
   * until they explicitly choose to unmute.
   */
  userWantsMuted: boolean;
  setUserWantsMuted: (muted: boolean) => void;
}

export const useVoiceStore = create<VoiceStore>((set) => ({
  speakingUserIds: new Set(),
  setSpeakingUserIds: (ids) => set({ speakingUserIds: ids }),

  userWantsMuted: false,
  setUserWantsMuted: (muted) => set({ userWantsMuted: muted }),
}));
