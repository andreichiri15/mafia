import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "./ui/tabs";
import { Avatar, AvatarFallback } from "./ui/avatar";
import { Badge } from "./ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "./ui/table";
import { ScrollArea } from "./ui/scroll-area";
import { Trophy, Users, Target, Clock } from "lucide-react";

interface GameHistory {
  id: string;
  date: Date;
  result: "win" | "loss";
  role: string;
  duration: string;
}

interface Friend {
  id: string;
  name: string;
  status: "online" | "offline" | "in-game";
}

const mockStats = {
  gamesPlayed: 147,
  winRate: 62,
  favoriteRole: "Detective",
  totalPlayTime: "48h 32m",
};

const mockGameHistory: GameHistory[] = [
  { id: "1", date: new Date(Date.now() - 86400000), result: "win", role: "Detective", duration: "25m" },
  { id: "2", date: new Date(Date.now() - 172800000), result: "loss", role: "Mafia", duration: "18m" },
  { id: "3", date: new Date(Date.now() - 259200000), result: "win", role: "Doctor", duration: "32m" },
  { id: "4", date: new Date(Date.now() - 345600000), result: "win", role: "Citizen", duration: "22m" },
  { id: "5", date: new Date(Date.now() - 432000000), result: "loss", role: "Mafia", duration: "28m" },
];

const mockFriends: Friend[] = [
  { id: "1", name: "CoolGamer", status: "online" },
  { id: "2", name: "MafiaFan", status: "in-game" },
  { id: "3", name: "Detective99", status: "offline" },
  { id: "4", name: "ProPlayer", status: "online" },
  { id: "5", name: "NightOwl", status: "offline" },
];

export function ProfilePage() {
  const playerName = "Player123";
  
  const getStatusColor = (status: Friend["status"]) => {
    switch (status) {
      case "online": return "bg-green-500";
      case "in-game": return "bg-yellow-500";
      case "offline": return "bg-gray-400";
    }
  };
  
  const getStatusText = (status: Friend["status"]) => {
    switch (status) {
      case "online": return "Online";
      case "in-game": return "In Game";
      case "offline": return "Offline";
    }
  };
  
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-6xl mx-auto space-y-8">
        {/* Profile Header */}
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-6">
              <Avatar className="w-24 h-24">
                <AvatarFallback className="text-2xl">{playerName[0].toUpperCase()}</AvatarFallback>
              </Avatar>
              <div className="flex-1">
                <h2>{playerName}</h2>
                <p className="text-muted-foreground">Member since October 2024</p>
              </div>
            </div>
          </CardContent>
        </Card>
        
        {/* Stats Overview */}
        <div className="grid md:grid-cols-4 gap-4">
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <Trophy className="w-8 h-8 text-yellow-500" />
                <div>
                  <p className="text-muted-foreground">Win Rate</p>
                  <p className="text-2xl">{mockStats.winRate}%</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <Users className="w-8 h-8 text-blue-500" />
                <div>
                  <p className="text-muted-foreground">Games Played</p>
                  <p className="text-2xl">{mockStats.gamesPlayed}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <Target className="w-8 h-8 text-purple-500" />
                <div>
                  <p className="text-muted-foreground">Favorite Role</p>
                  <p className="text-2xl">{mockStats.favoriteRole}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <Clock className="w-8 h-8 text-green-500" />
                <div>
                  <p className="text-muted-foreground">Play Time</p>
                  <p className="text-2xl">{mockStats.totalPlayTime}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
        
        {/* Tabs for Friends and History */}
        <Tabs defaultValue="friends" className="space-y-4">
          <TabsList>
            <TabsTrigger value="friends">Friends</TabsTrigger>
            <TabsTrigger value="history">Game History</TabsTrigger>
          </TabsList>
          
          <TabsContent value="friends">
            <Card>
              <CardHeader>
                <CardTitle>Friends ({mockFriends.length})</CardTitle>
              </CardHeader>
              <CardContent>
                <ScrollArea className="h-[400px]">
                  <div className="space-y-3">
                    {mockFriends.map((friend) => (
                      <div key={friend.id} className="flex items-center justify-between p-3 rounded-lg hover:bg-accent">
                        <div className="flex items-center gap-3">
                          <div className="relative">
                            <Avatar>
                              <AvatarFallback>{friend.name[0].toUpperCase()}</AvatarFallback>
                            </Avatar>
                            <div className={`absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-background ${getStatusColor(friend.status)}`} />
                          </div>
                          <div>
                            <p>{friend.name}</p>
                            <p className="text-muted-foreground">{getStatusText(friend.status)}</p>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </ScrollArea>
              </CardContent>
            </Card>
          </TabsContent>
          
          <TabsContent value="history">
            <Card>
              <CardHeader>
                <CardTitle>Recent Games</CardTitle>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Date</TableHead>
                      <TableHead>Role</TableHead>
                      <TableHead>Duration</TableHead>
                      <TableHead>Result</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {mockGameHistory.map((game) => (
                      <TableRow key={game.id}>
                        <TableCell>
                          {game.date.toLocaleDateString()}
                        </TableCell>
                        <TableCell>{game.role}</TableCell>
                        <TableCell>{game.duration}</TableCell>
                        <TableCell>
                          <Badge variant={game.result === "win" ? "default" : "destructive"}>
                            {game.result === "win" ? "Victory" : "Defeat"}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
