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

export interface LobbyDetail {
  lobbyId: number;
  name: string;
  hostname: string;
  maxPlayers: number;
  currentPlayers: number;
  hasPassword: boolean;
  isLocked: boolean;
  publicLobby: boolean;
  players: PlayerInfo[];
  createdAt: string;
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
export type Role = "MAFIA" | "VILLAGER" | "SHERIFF";

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
  winningTeam: "MAFIA_WIN" | "VILLAGER_WIN";
  players: GamePlayerInfo[];
}
