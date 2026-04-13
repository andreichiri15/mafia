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
