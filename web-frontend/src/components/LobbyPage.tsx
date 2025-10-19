import { useState } from "react";
import { Button } from "./ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Input } from "./ui/input";
import { Avatar, AvatarFallback } from "./ui/avatar";
import { Badge } from "./ui/badge";
import { ScrollArea } from "./ui/scroll-area";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "./ui/dialog";
import { Separator } from "./ui/separator";
import { Users, Link as LinkIcon, UserPlus, Volume2, VolumeX, UserMinus, Send, Copy, Check } from "lucide-react";

interface Player {
  id: string;
  name: string;
  isHost: boolean;
  isMuted: boolean;
}

interface ChatMessage {
  id: string;
  player: string;
  message: string;
  timestamp: Date;
}

const mockPlayers: Player[] = [
  { id: "1", name: "Player123", isHost: true, isMuted: false },
  { id: "2", name: "CoolGamer", isHost: false, isMuted: false },
  { id: "3", name: "MafiaFan", isHost: false, isMuted: false },
  { id: "4", name: "Detective99", isHost: false, isMuted: false },
];

const mockMessages: ChatMessage[] = [
  { id: "1", player: "Player123", message: "Welcome to the lobby!", timestamp: new Date(Date.now() - 120000) },
  { id: "2", player: "CoolGamer", message: "Hey everyone!", timestamp: new Date(Date.now() - 90000) },
  { id: "3", player: "MafiaFan", message: "Ready to play!", timestamp: new Date(Date.now() - 60000) },
];

export function LobbyPage() {
  const [players, setPlayers] = useState<Player[]>(mockPlayers);
  const [messages, setMessages] = useState<ChatMessage[]>(mockMessages);
  const [messageInput, setMessageInput] = useState("");
  const [inviteLinkCopied, setInviteLinkCopied] = useState(false);
  const inviteLink = "https://mafiagame.com/join/abc123";
  
  const currentPlayer = players[0]; // Assuming first player is current user
  
  const handleSendMessage = () => {
    if (!messageInput.trim()) return;
    
    const newMessage: ChatMessage = {
      id: String(messages.length + 1),
      player: currentPlayer.name,
      message: messageInput,
      timestamp: new Date(),
    };
    
    setMessages([...messages, newMessage]);
    setMessageInput("");
  };
  
  const handleKickPlayer = (playerId: string) => {
    setPlayers(players.filter(p => p.id !== playerId));
  };
  
  const handleMutePlayer = (playerId: string) => {
    setPlayers(players.map(p => 
      p.id === playerId ? { ...p, isMuted: !p.isMuted } : p
    ));
  };
  
  const handleCopyInviteLink = () => {
    navigator.clipboard.writeText(inviteLink);
    setInviteLinkCopied(true);
    setTimeout(() => setInviteLinkCopied(false), 2000);
  };
  
  return (
    <div className="container mx-auto px-4 py-8 h-[calc(100vh-4rem)]">
      <div className="grid lg:grid-cols-3 gap-6 h-full">
        {/* Players Section */}
        <div className="lg:col-span-1">
          <Card className="h-full flex flex-col">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center gap-2">
                  <Users className="w-5 h-5" />
                  Players ({players.length}/10)
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
                
                <Button variant="outline" size="sm" className="flex-1">
                  <UserPlus className="w-4 h-4 mr-2" />
                  Invite Friends
                </Button>
              </div>
              
              <Separator />
              
              <ScrollArea className="flex-1">
                <div className="space-y-3">
                  {players.map((player) => (
                    <div key={player.id} className="flex items-center justify-between p-2 rounded-lg hover:bg-accent">
                      <div className="flex items-center gap-3">
                        <Avatar>
                          <AvatarFallback>{player.name[0].toUpperCase()}</AvatarFallback>
                        </Avatar>
                        <div>
                          <div className="flex items-center gap-2">
                            <span>{player.name}</span>
                            {player.isHost && <Badge variant="secondary">Host</Badge>}
                            {player.isMuted && <Badge variant="destructive">Muted</Badge>}
                          </div>
                        </div>
                      </div>
                      
                      {currentPlayer.isHost && !player.isHost && (
                        <div className="flex gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleMutePlayer(player.id)}
                          >
                            {player.isMuted ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleKickPlayer(player.id)}
                          >
                            <UserMinus className="w-4 h-4" />
                          </Button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </ScrollArea>
              
              {currentPlayer.isHost && (
                <Button className="w-full">Start Game</Button>
              )}
            </CardContent>
          </Card>
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
                  {messages.map((msg) => (
                    <div key={msg.id} className="space-y-1">
                      <div className="flex items-baseline gap-2">
                        <span className="font-medium">{msg.player}</span>
                        <span className="text-muted-foreground">
                          {msg.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
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
                  onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
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
