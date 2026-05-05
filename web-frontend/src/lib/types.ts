export interface LobbySummary {
  id: number;
  name: string;
  hostname: string;
  maxPlayers: number;
  currentPlayers: number;
  createdAt: string;
}

export interface PlayerInfo {
  userId: number;
  username: string;
  isHost: boolean;
  isReady: boolean;
}

export interface GameSettings {
  mafiaCount: number;
  includeSheriff: boolean;
  includeDoctor: boolean;
  includeJester: boolean;
  includeMutilator: boolean;
  doctorSelfSaveLimit: number; // -1 = unlimited
  sheriffInvestigationDelay: number;
}

export interface LobbyDetail {
  lobbyId: number;
  name: string;
  hostname: string;
  maxPlayers: number;
  currentPlayers: number;
  hasPassword: boolean;
  isLocked: boolean;
  publicLobby: boolean;
  inviteToken: string | null;
  players: PlayerInfo[];
  settings: GameSettings;
  createdAt: string;
}

export interface InviteResolution {
  lobbyId: number;
  name: string;
  hostname: string;
  currentPlayers: number;
  maxPlayers: number;
  isLocked: boolean;
  inGame: boolean;
}

export interface LobbyInvite {
  lobbyId: number;
  lobbyName: string;
  inviteToken: string;
  inviterUsername: string;
  sentAt: string;
}

// Profile

export interface RoleStats {
  played: number;
  won: number;
  winRate: number;
}

export interface MatchHistoryEntry {
  gameId: number;
  lobbyName: string;
  role: Role | null;
  winningTeam: WinningTeam | null;
  won: boolean;
  alive: boolean;
  deathCause: string | null;
  startedAt: string;
  endedAt: string;
  durationSeconds: number;
}

export interface ProfileResponse {
  userId: number;
  username: string;
  dateJoined: string;
  totalGames: number;
  totalWins: number;
  winRate: number;
  favoriteRole: Role | null;
  survivalRate: number;
  avgGameDurationSeconds: number;
  roleStats: Record<string, RoleStats>;
  matchHistory: MatchHistoryEntry[];
}

export interface CreateLobbyRequest {
  name: string;
  maxPlayers: number;
  password?: string;
  isLocked: boolean;
  publicLobby: boolean;
}

// Game types

export type GamePhase = "NIGHT" | "DAY" | "VOTING" | "GAME_OVER";
export type Role = "MAFIA" | "VILLAGER" | "SHERIFF" | "DOCTOR" | "JESTER" | "MUTILATOR";
export type WinningTeam = "MAFIA_WIN" | "VILLAGER_WIN" | "JESTER_WIN";

export interface GamePlayerInfo {
  userId: number;
  username: string;
  alive: boolean;
  role: Role | null;
}

export interface GameState {
  gameId: number;
  phase: GamePhase;
  round: number;
  phaseEndTime: number; // epoch ms
  yourRole: Role;
  alive: boolean;
  players: GamePlayerInfo[];
  events: string[];
}

export interface GameStartEvent {
  gameId: number;
  lobbyId: number;
}

export interface PhaseResultEvent {
  phase: GamePhase;
  round: number;
  eliminatedPlayer: string | null;
  events: string[];
}

export interface GameOverEvent {
  winningTeam: WinningTeam;
  players: GamePlayerInfo[];
}

// Friends + DM types

export type PresenceStatus = "ONLINE" | "IN_LOBBY" | "IN_GAME" | "OFFLINE";

export interface FriendInfo {
  userId: number;
  username: string;
  status: PresenceStatus;
}

export interface FriendRequestInfo {
  requestId: number;
  fromUserId: number;
  fromUsername: string;
  createdAt: string;
}

export interface UserSearchResult {
  userId: number;
  username: string;
  relationship: "NONE" | "PENDING_OUT" | "PENDING_IN" | "FRIENDS" | "SELF";
}

export interface DirectMessageInfo {
  id: number;
  senderId: number;
  senderUsername: string;
  receiverId: number;
  content: string;
  sentAt: string;
}
