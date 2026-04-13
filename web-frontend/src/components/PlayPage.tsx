import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "./ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Input } from "./ui/input";
import { Badge } from "./ui/badge";
import { ScrollArea } from "./ui/scroll-area";
import { Users, Plus, Search, Loader2 } from "lucide-react";
import { useLobbyStore } from "../store/lobbyStore";
import { useAuthStore } from "../store/authStore";

export function PlayPage() {
  const [view, setView] = useState<"choose" | "host" | "browse">("choose");
  const [serverName, setServerName] = useState("");
  const [maxPlayers, setMaxPlayers] = useState("10");
  const [searchQuery, setSearchQuery] = useState("");
  const [creating, setCreating] = useState(false);
  const navigate = useNavigate();

  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const { lobbies, loading, error, fetchLobbies, createLobby, joinLobby } = useLobbyStore();

  useEffect(() => {
    if (view === "browse") {
      fetchLobbies();
    }
  }, [view, fetchLobbies]);

  useEffect(() => {
    if (view === "browse") {
      const debounce = setTimeout(() => {
        fetchLobbies(searchQuery || undefined);
      }, 300);
      return () => clearTimeout(debounce);
    }
  }, [searchQuery, view, fetchLobbies]);

  const handleCreateServer = async () => {
    if (!isLoggedIn) {
      navigate("/signin");
      return;
    }
    setCreating(true);
    try {
      const lobby = await createLobby({
        name: serverName,
        maxPlayers: parseInt(maxPlayers) || 10,
        isLocked: false,
        publicLobby: true,
      });
      navigate(`/lobby/${lobby.lobbyId}`);
    } catch {
      // error is set in the store
    } finally {
      setCreating(false);
    }
  };

  const handleJoinServer = async (serverId: number) => {
    if (!isLoggedIn) {
      navigate("/signin");
      return;
    }
    try {
      const lobby = await joinLobby(serverId);
      navigate(`/lobby/${lobby.lobbyId}`);
    } catch {
      // error is set in the store
    }
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

              {error && (
                <p className="text-red-500 text-sm">{error}</p>
              )}

              <div className="flex gap-2 pt-4">
                <Button
                  onClick={handleCreateServer}
                  disabled={!serverName.trim() || creating}
                  className="flex-1"
                >
                  {creating ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      Creating...
                    </>
                  ) : (
                    "Create Server"
                  )}
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

          {error && (
            <p className="text-red-500 text-sm">{error}</p>
          )}

          <ScrollArea className="h-[500px]">
            <div className="space-y-3">
              {loading && lobbies.length === 0 ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
                </div>
              ) : lobbies.length === 0 ? (
                <p className="text-center py-12 text-muted-foreground">No servers found</p>
              ) : (
                lobbies.map((server) => (
                  <Card key={server.id}>
                    <CardContent className="flex items-center justify-between p-6">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <h4>{server.name}</h4>
                          <Badge variant={server.currentPlayers < server.maxPlayers ? "default" : "secondary"}>
                            {server.currentPlayers < server.maxPlayers ? "Waiting" : "Full"}
                          </Badge>
                        </div>
                        <p className="text-muted-foreground">
                          Hosted by {server.hostname}
                        </p>
                      </div>

                      <div className="flex items-center gap-4">
                        <div className="flex items-center gap-2 text-muted-foreground">
                          <Users className="w-4 h-4" />
                          <span>{server.currentPlayers}/{server.maxPlayers}</span>
                        </div>
                        <Button
                          onClick={() => handleJoinServer(server.id)}
                          disabled={server.currentPlayers >= server.maxPlayers}
                        >
                          Join
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                ))
              )}
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
