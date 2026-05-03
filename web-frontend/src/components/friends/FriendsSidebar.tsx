import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "../ui/sheet";
import { Button } from "../ui/button";
import { Avatar, AvatarFallback } from "../ui/avatar";
import { Separator } from "../ui/separator";
import { ScrollArea } from "../ui/scroll-area";
import { UserPlus, MessageSquare, User as UserIcon, Check, X, Trash2, Send } from "lucide-react";
import { toast } from "sonner";
import { useFriendStore } from "../../store/friendStore";
import { useLobbyStore } from "../../store/lobbyStore";
import { AddFriendModal } from "./AddFriendModal";
import { FriendChatPanel } from "./FriendChatPanel";
import type { FriendInfo, PresenceStatus } from "../../lib/types";

interface FriendsSidebarProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const statusColor: Record<PresenceStatus, string> = {
  ONLINE: "bg-green-500",
  IN_LOBBY: "bg-blue-500",
  IN_GAME: "bg-yellow-500",
  OFFLINE: "bg-gray-400",
};

const statusLabel: Record<PresenceStatus, string> = {
  ONLINE: "Online",
  IN_LOBBY: "In a lobby",
  IN_GAME: "In a game",
  OFFLINE: "Offline",
};

export function FriendsSidebar({ open, onOpenChange }: FriendsSidebarProps) {
  const navigate = useNavigate();
  const friends = useFriendStore((s) => s.friends);
  const requests = useFriendStore((s) => s.requests);
  const acceptRequest = useFriendStore((s) => s.acceptRequest);
  const declineRequest = useFriendStore((s) => s.declineRequest);
  const removeFriend = useFriendStore((s) => s.removeFriend);
  const currentLobby = useLobbyStore((s) => s.currentLobby);
  const inviteFriend = useLobbyStore((s) => s.inviteFriend);

  const [addOpen, setAddOpen] = useState(false);
  const [expandedFriendId, setExpandedFriendId] = useState<number | null>(null);
  const [openChatFriend, setOpenChatFriend] = useState<FriendInfo | null>(null);

  const handleInviteToLobby = async (friend: FriendInfo) => {
    if (!currentLobby) return;
    try {
      await inviteFriend(currentLobby.lobbyId, friend.userId);
      toast.success(`Invite sent to ${friend.username}`);
      setExpandedFriendId(null);
    } catch (e) {
      toast.error((e as Error).message || "Couldn't send invite");
    }
  };

  const handleProfile = () => {
    if (!expandedFriendId) return;
    onOpenChange(false);
    setExpandedFriendId(null);
    navigate("/profile"); // for now profile only shows the current user
  };

  const handleOpenChat = (friend: FriendInfo) => {
    setExpandedFriendId(null);
    setOpenChatFriend(friend);
  };

  // Sort friends by status (online first, offline last) then alphabetically
  const sortedFriends = [...friends].sort((a, b) => {
    const order: Record<PresenceStatus, number> = { IN_GAME: 0, IN_LOBBY: 1, ONLINE: 2, OFFLINE: 3 };
    const diff = order[a.status] - order[b.status];
    return diff !== 0 ? diff : a.username.localeCompare(b.username);
  });

  return (
    <>
      <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent side="right" className="w-[380px] sm:max-w-md p-0 flex flex-col">
          {openChatFriend ? (
            <FriendChatPanel
              friend={openChatFriend}
              onBack={() => setOpenChatFriend(null)}
            />
          ) : (
            <>
              <SheetHeader className="p-4 border-b">
                <div className="flex items-center justify-between">
                  <SheetTitle>Friends</SheetTitle>
                  <Button size="sm" onClick={() => setAddOpen(true)}>
                    <UserPlus className="w-4 h-4 mr-1" />
                    Add
                  </Button>
                </div>
              </SheetHeader>

              <ScrollArea className="flex-1">
                <div className="p-4 space-y-4">
                  {/* Pending requests */}
                  {requests.length > 0 && (
                    <div className="space-y-2">
                      <p className="text-xs uppercase tracking-wide text-muted-foreground">
                        Pending Requests ({requests.length})
                      </p>
                      {requests.map((req) => (
                        <div key={req.requestId} className="flex items-center gap-2 p-2 rounded-lg bg-muted">
                          <Avatar className="w-8 h-8">
                            <AvatarFallback>{req.fromUsername[0].toUpperCase()}</AvatarFallback>
                          </Avatar>
                          <span className="flex-1 text-sm">{req.fromUsername}</span>
                          <Button
                            size="icon"
                            variant="ghost"
                            className="text-green-500 h-8 w-8"
                            onClick={() => acceptRequest(req.requestId)}
                          >
                            <Check className="w-4 h-4" />
                          </Button>
                          <Button
                            size="icon"
                            variant="ghost"
                            className="text-red-500 h-8 w-8"
                            onClick={() => declineRequest(req.requestId)}
                          >
                            <X className="w-4 h-4" />
                          </Button>
                        </div>
                      ))}
                      <Separator />
                    </div>
                  )}

                  {/* Friends list */}
                  <div className="space-y-1">
                    <p className="text-xs uppercase tracking-wide text-muted-foreground">
                      Friends ({friends.length})
                    </p>
                    {sortedFriends.length === 0 && (
                      <p className="text-sm text-muted-foreground py-4 text-center">
                        No friends yet. Add one to get started!
                      </p>
                    )}
                    {sortedFriends.map((friend) => {
                      const isExpanded = expandedFriendId === friend.userId;
                      return (
                        <div key={friend.userId} className="rounded-lg overflow-hidden">
                          <button
                            onClick={() =>
                              setExpandedFriendId(isExpanded ? null : friend.userId)
                            }
                            className="w-full flex items-center gap-3 p-2 hover:bg-accent text-left transition-colors"
                          >
                            <div className="relative">
                              <Avatar>
                                <AvatarFallback>
                                  {friend.username[0].toUpperCase()}
                                </AvatarFallback>
                              </Avatar>
                              <div
                                className={`absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-background ${statusColor[friend.status]}`}
                                title={statusLabel[friend.status]}
                              />
                            </div>
                            <div className="flex-1 min-w-0">
                              <p className="text-sm font-medium truncate">{friend.username}</p>
                              <p className="text-xs text-muted-foreground">
                                {statusLabel[friend.status]}
                              </p>
                            </div>
                          </button>

                          {isExpanded && (
                            <div className="bg-muted/50 px-2 py-2 space-y-1">
                              <div className="flex gap-1 flex-wrap">
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  className="flex-1"
                                  onClick={handleProfile}
                                >
                                  <UserIcon className="w-4 h-4 mr-1" />
                                  Profile
                                </Button>
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  className="flex-1"
                                  onClick={() => handleOpenChat(friend)}
                                >
                                  <MessageSquare className="w-4 h-4 mr-1" />
                                  Chat
                                </Button>
                                <Button
                                  size="icon"
                                  variant="ghost"
                                  className="text-red-500 h-8 w-8"
                                  onClick={() => {
                                    if (confirm(`Remove ${friend.username} from friends?`)) {
                                      removeFriend(friend.userId);
                                      setExpandedFriendId(null);
                                    }
                                  }}
                                  title="Remove friend"
                                >
                                  <Trash2 className="w-4 h-4" />
                                </Button>
                              </div>
                              {currentLobby && (
                                <Button
                                  size="sm"
                                  variant="outline"
                                  className="w-full"
                                  onClick={() => handleInviteToLobby(friend)}
                                >
                                  <Send className="w-4 h-4 mr-1" />
                                  Invite to lobby
                                </Button>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              </ScrollArea>
            </>
          )}
        </SheetContent>
      </Sheet>

      <AddFriendModal open={addOpen} onOpenChange={setAddOpen} />
    </>
  );
}
