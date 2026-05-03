import { useEffect, useRef, useState } from "react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Avatar, AvatarFallback } from "../ui/avatar";
import { ScrollArea } from "../ui/scroll-area";
import { ArrowLeft, Send } from "lucide-react";
import { useDmStore } from "../../store/dmStore";
import { useAuthStore } from "../../store/authStore";
import type { FriendInfo } from "../../lib/types";

interface FriendChatPanelProps {
  friend: FriendInfo;
  onBack: () => void;
}

// Module-scoped constants so Zustand selectors return a stable reference
// when the conversation hasn't been loaded yet (otherwise [] would be a new
// array on every render, causing an infinite update loop).
const EMPTY_MESSAGES: never[] = [];

export function FriendChatPanel({ friend, onBack }: FriendChatPanelProps) {
  const [input, setInput] = useState("");
  const me = useAuthStore((s) => s.user);
  const messages = useDmStore((s) => s.conversations[friend.userId] ?? EMPTY_MESSAGES);
  const loaded = useDmStore((s) => s.loaded[friend.userId] ?? false);
  const loadConversation = useDmStore((s) => s.loadConversation);
  const sendDm = useDmStore((s) => s.sendDm);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!loaded) {
      loadConversation(friend.userId);
    }
  }, [friend.userId, loaded, loadConversation]);

  useEffect(() => {
    // Auto-scroll to bottom on new messages
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages.length]);

  const handleSend = () => {
    if (!input.trim()) return;
    sendDm(friend.userId, input);
    setInput("");
  };

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center gap-3 p-4 border-b">
        <Button variant="ghost" size="icon" onClick={onBack}>
          <ArrowLeft className="w-4 h-4" />
        </Button>
        <Avatar>
          <AvatarFallback>{friend.username[0].toUpperCase()}</AvatarFallback>
        </Avatar>
        <div className="flex-1 min-w-0">
          <p className="font-medium truncate">{friend.username}</p>
          <p className="text-xs text-muted-foreground">{friendStatusLabel(friend.status)}</p>
        </div>
      </div>

      {/* Messages */}
      <ScrollArea className="flex-1 px-4">
        <div className="py-4 space-y-3" ref={scrollRef}>
          {messages.length === 0 && (
            <p className="text-center text-sm text-muted-foreground py-8">
              No messages yet. Say hi!
            </p>
          )}
          {messages.map((msg) => {
            const mine = msg.senderId === me?.userId;
            return (
              <div key={msg.id} className={`flex ${mine ? "justify-end" : "justify-start"}`}>
                <div
                  className={`max-w-[75%] rounded-lg px-3 py-2 text-sm ${
                    mine
                      ? "bg-primary text-primary-foreground"
                      : "bg-muted text-foreground"
                  }`}
                >
                  <p className="whitespace-pre-wrap break-words">{msg.content}</p>
                  <p className={`text-[10px] mt-1 ${mine ? "text-primary-foreground/70" : "text-muted-foreground"}`}>
                    {new Date(msg.sentAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </ScrollArea>

      {/* Input */}
      <div className="p-3 border-t flex gap-2">
        <Input
          placeholder="Type a message..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSend()}
        />
        <Button size="icon" onClick={handleSend} disabled={!input.trim()}>
          <Send className="w-4 h-4" />
        </Button>
      </div>
    </div>
  );
}

function friendStatusLabel(status: FriendInfo["status"]): string {
  switch (status) {
    case "ONLINE":
      return "Online";
    case "IN_LOBBY":
      return "In a lobby";
    case "IN_GAME":
      return "In a game";
    case "OFFLINE":
    default:
      return "Offline";
  }
}
