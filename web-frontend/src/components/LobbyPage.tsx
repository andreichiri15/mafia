import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Button } from "./ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Input } from "./ui/input";
import { Avatar, AvatarFallback } from "./ui/avatar";
import { Badge } from "./ui/badge";
import { ScrollArea } from "./ui/scroll-area";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "./ui/dialog";
import { Separator } from "./ui/separator";
import { Users, Link as LinkIcon, Send, Copy, Check, LogOut, Loader2, CheckCircle } from "lucide-react";
import { useLobbyStore } from "../store/lobbyStore";
import { useChatStore } from "../store/chatStore";
import { useAuthStore } from "../store/authStore";
import { useGameStore } from "../store/gameStore";
import { sendMessage } from "../lib/websocket";
import { GameSettingsPanel } from "./lobby/GameSettingsPanel";

export function LobbyPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const lobbyId = Number(id);

  const user = useAuthStore((s) => s.user);
  const { currentLobby, loading, error, leaveLobby, toggleReady, subscribeLobby, unsubscribeLobby, startedGameId, clearStartedGameId, updateSettings } = useLobbyStore();
  const startGame = useGameStore((s) => s.startGame);
  const messages = useChatStore((s) => s.messages);

  const [messageInput, setMessageInput] = useState("");
  const [inviteLinkCopied, setInviteLinkCopied] = useState(false);
  const [starting, setStarting] = useState(false);

  const inviteLink = currentLobby?.inviteToken
    ? `${window.location.origin}/invite/${currentLobby.inviteToken}`
    : `${window.location.origin}/lobby/${lobbyId}`;

  useEffect(() => {
    if (!lobbyId) return;
    subscribeLobby(lobbyId);
    return () => unsubscribeLobby();
  }, [lobbyId, subscribeLobby, unsubscribeLobby]);

  // When the host starts the game, all subscribed players navigate
  useEffect(() => {
    if (startedGameId !== null) {
      const id = startedGameId;
      clearStartedGameId();
      navigate(`/game/${id}`);
    }
  }, [startedGameId, navigate, clearStartedGameId]);

  const isHost = currentLobby?.players.some(
    (p) => p.userId === user?.userId && p.isHost
  );

  const myPlayer = currentLobby?.players.find(
    (p) => p.userId === user?.userId
  );

  const handleSendMessage = () => {
    if (!messageInput.trim() || !user) return;
    sendMessage(`/app/lobby/${lobbyId}/chat`, { message: messageInput });
    setMessageInput("");
  };

  const handleLeaveLobby = async () => {
    await leaveLobby(lobbyId);
    navigate("/play");
  };

  const handleToggleReady = () => {
    toggleReady(lobbyId);
  };

  const handleCopyInviteLink = () => {
    navigator.clipboard.writeText(inviteLink);
    setInviteLinkCopied(true);
    setTimeout(() => setInviteLinkCopied(false), 2000);
  };

  if (!currentLobby && loading) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-4rem)]">
        <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!currentLobby && error) {
    return (
      <div className="flex flex-col items-center justify-center h-[calc(100vh-4rem)] gap-4">
        <p className="text-red-500">{error}</p>
        <Button onClick={() => navigate("/play")}>Back to Servers</Button>
      </div>
    );
  }

  if (!currentLobby) {
    return (
      <div className="flex flex-col items-center justify-center h-[calc(100vh-4rem)] gap-4">
        <p className="text-muted-foreground">Lobby not found</p>
        <Button onClick={() => navigate("/play")}>Back to Servers</Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 h-[calc(100vh-4rem)]">
      <div className="grid lg:grid-cols-4 gap-6 h-full">
        {/* Players Section */}
        <div className="lg:col-span-1">
          <Card className="h-full flex flex-col">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center gap-2">
                  <Users className="w-5 h-5" />
                  Players ({currentLobby.currentPlayers}/{currentLobby.maxPlayers})
                </CardTitle>
              </div>
            </CardHeader>
            <CardContent className="flex-1 flex flex-col gap-4">
              <div className="flex gap-2">
                <Dialog>
                  <DialogTrigger asChild>
                    <Button variant="outline" size="sm" className="flex-1">
                      <LinkIcon className="w-4 h-4 mr-2" />
                      Invite Link
                    </Button>
                  </DialogTrigger>
                  <DialogContent>
                    <DialogHeader>
                      <DialogTitle>Invite Players</DialogTitle>
                      <DialogDescription>
                        Share this link with friends to invite them to the game
                      </DialogDescription>
                    </DialogHeader>
                    <div className="flex gap-2">
                      <Input value={inviteLink} readOnly />
                      <Button onClick={handleCopyInviteLink} size="icon">
                        {inviteLinkCopied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                      </Button>
                    </div>
                  </DialogContent>
                </Dialog>

                <Button variant="outline" size="sm" className="flex-1" onClick={handleLeaveLobby}>
                  <LogOut className="w-4 h-4 mr-2" />
                  Leave
                </Button>
              </div>

              <Separator />

              <ScrollArea className="flex-1">
                <div className="space-y-3">
                  {currentLobby.players.map((player) => (
                    <div key={player.userId} className="flex items-center justify-between p-2 rounded-lg hover:bg-accent">
                      <div className="flex items-center gap-3">
                        <Avatar>
                          <AvatarFallback>{player.username[0].toUpperCase()}</AvatarFallback>
                        </Avatar>
                        <div>
                          <div className="flex items-center gap-2">
                            <span>{player.username}</span>
                            {player.isHost && <Badge variant="secondary">Host</Badge>}
                            {player.isReady && (
                              <CheckCircle className="w-4 h-4 text-green-500" />
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </ScrollArea>

              <div className="flex flex-col gap-2">
                {myPlayer && !isHost && (
                  <Button
                    variant={myPlayer.isReady ? "secondary" : "default"}
                    className="w-full"
                    onClick={handleToggleReady}
                  >
                    {myPlayer.isReady ? "Unready" : "Ready"}
                  </Button>
                )}

                {isHost && (
                  <Button
                    className="w-full"
                    disabled={starting || (currentLobby.currentPlayers < 4)}
                    onClick={async () => {
                      setStarting(true);
                      try {
                        const event = await startGame(lobbyId);
                        navigate(`/game/${event.gameId}`);
                      } catch {
                        setStarting(false);
                      }
                    }}
                  >
                    {starting ? "Starting..." : `Start Game${currentLobby.currentPlayers < 4 ? " (need 4+)" : ""}`}
                  </Button>
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Settings Section */}
        <div className="lg:col-span-1 overflow-y-auto">
          <GameSettingsPanel
            current={currentLobby.settings}
            maxPlayers={currentLobby.maxPlayers}
            isHost={!!isHost}
            onSave={(s) => updateSettings(lobbyId, s)}
          />
        </div>

        {/* Chat Section */}
        <div className="lg:col-span-2">
          <Card className="h-full flex flex-col">
            <CardHeader>
              <CardTitle>Lobby Chat</CardTitle>
            </CardHeader>
            <CardContent className="flex-1 flex flex-col gap-4">
              <ScrollArea className="flex-1 pr-4">
                <div className="space-y-4">
                  {messages.length === 0 && (
                    <p className="text-center text-muted-foreground py-8">
                      No messages yet. Say hello!
                    </p>
                  )}
                  {messages.map((msg) => (
                    <div key={msg.id} className="space-y-1">
                      <div className="flex items-baseline gap-2">
                        <span className={`font-medium ${msg.player === "System" ? "text-yellow-500" : ""}`}>
                          {msg.player}
                        </span>
                        <span className="text-muted-foreground text-xs">
                          {new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                      <p className="text-muted-foreground">{msg.message}</p>
                    </div>
                  ))}
                </div>
              </ScrollArea>

              <div className="flex gap-2">
                <Input
                  placeholder="Type a message..."
                  value={messageInput}
                  onChange={(e) => setMessageInput(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleSendMessage()}
                />
                <Button onClick={handleSendMessage}>
                  <Send className="w-4 h-4" />
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
