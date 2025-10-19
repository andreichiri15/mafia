import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "./ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Input } from "./ui/input";
import { Badge } from "./ui/badge";
import { ScrollArea } from "./ui/scroll-area";
import { Users, Plus, Search } from "lucide-react";

interface Server {
  id: string;
  name: string;
  host: string;
  players: number;
  maxPlayers: number;
  status: "waiting" | "in-progress";
}

const mockServers: Server[] = [
  { id: "1", name: "Casual Game Night", host: "Player123", players: 5, maxPlayers: 10, status: "waiting" },
  { id: "2", name: "Competitive Mafia", host: "ProGamer", players: 8, maxPlayers: 12, status: "waiting" },
  { id: "3", name: "Beginner Friendly", host: "NewbieHost", players: 3, maxPlayers: 8, status: "waiting" },
  { id: "4", name: "Late Night Session", host: "NightOwl", players: 10, maxPlayers: 10, status: "in-progress" },
  { id: "5", name: "Quick Match", host: "FastPlayer", players: 6, maxPlayers: 10, status: "waiting" },
];

export function PlayPage() {
  const [view, setView] = useState<"choose" | "host" | "browse">("choose");
  const [serverName, setServerName] = useState("");
  const [maxPlayers, setMaxPlayers] = useState("10");
  const [searchQuery, setSearchQuery] = useState("");
  const navigate = useNavigate();
  
  const filteredServers = mockServers.filter(server =>
    server.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    server.host.toLowerCase().includes(searchQuery.toLowerCase())
  );
  
  const handleCreateServer = () => {
    // In a real app, this would create a server and redirect to lobby
    navigate("/lobby");
  };
  
  const handleJoinServer = (serverId: string) => {
    // In a real app, this would join the server and redirect to lobby
    navigate("/lobby");
  };
  
  if (view === "choose") {
    return (
      <div className="container mx-auto px-4 py-12">
        <div className="max-w-2xl mx-auto space-y-8">
          <div className="text-center space-y-2">
            <h2>Choose Your Action</h2>
            <p className="text-muted-foreground">Host a new game or join an existing server</p>
          </div>
          
          <div className="grid md:grid-cols-2 gap-6">
            <Card className="cursor-pointer hover:border-primary transition-colors" onClick={() => setView("host")}>
              <CardHeader>
                <Plus className="w-10 h-10 mb-2" />
                <CardTitle>Host Server</CardTitle>
              </CardHeader>
              <CardContent>
                <CardDescription>
                  Create your own game and invite friends to join
                </CardDescription>
              </CardContent>
            </Card>
            
            <Card className="cursor-pointer hover:border-primary transition-colors" onClick={() => setView("browse")}>
              <CardHeader>
                <Search className="w-10 h-10 mb-2" />
                <CardTitle>Browse Servers</CardTitle>
              </CardHeader>
              <CardContent>
                <CardDescription>
                  Find and join public games hosted by other players
                </CardDescription>
              </CardContent>
            </Card>
          </div>
          
          <div className="text-center">
            <Button variant="ghost" onClick={() => navigate("/")}>
              Back to Home
            </Button>
          </div>
        </div>
      </div>
    );
  }
  
  if (view === "host") {
    return (
      <div className="container mx-auto px-4 py-12">
        <div className="max-w-2xl mx-auto space-y-8">
          <div className="space-y-2">
            <h2>Host a Server</h2>
            <p className="text-muted-foreground">Configure your game settings</p>
          </div>
          
          <Card>
            <CardHeader>
              <CardTitle>Server Settings</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <label htmlFor="serverName">Server Name</label>
                <Input
                  id="serverName"
                  placeholder="Enter server name"
                  value={serverName}
                  onChange={(e) => setServerName(e.target.value)}
                />
              </div>
              
              <div className="space-y-2">
                <label htmlFor="maxPlayers">Max Players</label>
                <Input
                  id="maxPlayers"
                  type="number"
                  min="4"
                  max="20"
                  value={maxPlayers}
                  onChange={(e) => setMaxPlayers(e.target.value)}
                />
              </div>
              
              <div className="flex gap-2 pt-4">
                <Button 
                  onClick={handleCreateServer}
                  disabled={!serverName.trim()}
                  className="flex-1"
                >
                  Create Server
                </Button>
                <Button variant="outline" onClick={() => setView("choose")}>
                  Cancel
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }
  
  return (
    <div className="container mx-auto px-4 py-12">
      <div className="max-w-4xl mx-auto space-y-8">
        <div className="space-y-2">
          <h2>Browse Servers</h2>
          <p className="text-muted-foreground">Join an existing game</p>
        </div>
        
        <div className="space-y-4">
          <Input
            placeholder="Search servers..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="max-w-md"
          />
          
          <ScrollArea className="h-[500px]">
            <div className="space-y-3">
              {filteredServers.map((server) => (
                <Card key={server.id}>
                  <CardContent className="flex items-center justify-between p-6">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <h4>{server.name}</h4>
                        <Badge variant={server.status === "waiting" ? "default" : "secondary"}>
                          {server.status === "waiting" ? "Waiting" : "In Progress"}
                        </Badge>
                      </div>
                      <p className="text-muted-foreground">
                        Hosted by {server.host}
                      </p>
                    </div>
                    
                    <div className="flex items-center gap-4">
                      <div className="flex items-center gap-2 text-muted-foreground">
                        <Users className="w-4 h-4" />
                        <span>{server.players}/{server.maxPlayers}</span>
                      </div>
                      <Button
                        onClick={() => handleJoinServer(server.id)}
                        disabled={server.status === "in-progress" || server.players >= server.maxPlayers}
                      >
                        Join
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </ScrollArea>
          
          <Button variant="outline" onClick={() => setView("choose")}>
            Back
          </Button>
        </div>
      </div>
    </div>
  );
}
