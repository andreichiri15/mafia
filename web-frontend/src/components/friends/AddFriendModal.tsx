import { useEffect, useState } from "react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "../ui/dialog";
import { Avatar, AvatarFallback } from "../ui/avatar";
import { Loader2, UserPlus, Check, Clock } from "lucide-react";
import { useFriendStore } from "../../store/friendStore";
import type { UserSearchResult } from "../../lib/types";

interface AddFriendModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function AddFriendModal({ open, onOpenChange }: AddFriendModalProps) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<UserSearchResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [pending, setPending] = useState<Record<number, boolean>>({});
  const [error, setError] = useState<string | null>(null);
  const searchUsers = useFriendStore((s) => s.searchUsers);
  const sendRequest = useFriendStore((s) => s.sendRequest);

  useEffect(() => {
    if (!open) {
      setQuery("");
      setResults([]);
      setError(null);
      return;
    }
  }, [open]);

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      return;
    }
    let cancelled = false;
    setSearching(true);
    const timer = setTimeout(async () => {
      const r = await searchUsers(query);
      if (!cancelled) {
        setResults(r);
        setSearching(false);
      }
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [query, searchUsers]);

  const handleSend = async (user: UserSearchResult) => {
    setPending((p) => ({ ...p, [user.userId]: true }));
    setError(null);
    try {
      await sendRequest(user.username);
      // Reflect in the local list
      setResults((rs) =>
        rs.map((r) => (r.userId === user.userId ? { ...r, relationship: "PENDING_OUT" } : r))
      );
    } catch (e) {
      setError((e as Error).message || "Failed to send request");
    } finally {
      setPending((p) => ({ ...p, [user.userId]: false }));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add Friend</DialogTitle>
          <DialogDescription>Search for a user by username to send them a friend request.</DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          <Input
            placeholder="Search username..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            autoFocus
          />
          {error && <p className="text-sm text-red-500">{error}</p>}
          <div className="max-h-72 overflow-y-auto space-y-2">
            {searching && (
              <div className="flex justify-center py-4">
                <Loader2 className="w-5 h-5 animate-spin text-muted-foreground" />
              </div>
            )}
            {!searching && query && results.length === 0 && (
              <p className="text-sm text-center text-muted-foreground py-4">No users found</p>
            )}
            {results.map((user) => (
              <div key={user.userId} className="flex items-center justify-between p-2 rounded-lg hover:bg-accent">
                <div className="flex items-center gap-3">
                  <Avatar className="w-8 h-8">
                    <AvatarFallback>{user.username[0].toUpperCase()}</AvatarFallback>
                  </Avatar>
                  <span className="text-sm">{user.username}</span>
                </div>
                {user.relationship === "SELF" ? (
                  <span className="text-xs text-muted-foreground">You</span>
                ) : user.relationship === "FRIENDS" ? (
                  <span className="text-xs text-green-500 flex items-center gap-1">
                    <Check className="w-3 h-3" /> Friends
                  </span>
                ) : user.relationship === "PENDING_OUT" ? (
                  <span className="text-xs text-muted-foreground flex items-center gap-1">
                    <Clock className="w-3 h-3" /> Requested
                  </span>
                ) : user.relationship === "PENDING_IN" ? (
                  <span className="text-xs text-muted-foreground">Sent you a request</span>
                ) : (
                  <Button
                    size="sm"
                    onClick={() => handleSend(user)}
                    disabled={pending[user.userId]}
                  >
                    {pending[user.userId] ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        <UserPlus className="w-4 h-4 mr-1" /> Add
                      </>
                    )}
                  </Button>
                )}
              </div>
            ))}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
