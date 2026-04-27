import { useState } from "react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { ScrollArea } from "../ui/scroll-area";
import { Send } from "lucide-react";
import type { ChatMessage } from "../../store/chatStore";
import type { GamePhase, Role } from "../../lib/types";

interface GameChatProps {
  phase: GamePhase;
  yourRole: Role;
  alive: boolean;
  dayMessages: ChatMessage[];
  mafiaMessages: ChatMessage[];
  deadMessages: ChatMessage[];
  onSendMessage: (channel: string, message: string) => void;
}

type ChatTab = "day" | "mafia" | "dead";

export function GameChat({
  phase,
  yourRole,
  alive,
  dayMessages,
  mafiaMessages,
  deadMessages,
  onSendMessage,
}: GameChatProps) {
  const [messageInput, setMessageInput] = useState("");

  // Determine available tabs
  const tabs: { key: ChatTab; label: string; available: boolean }[] = [
    {
      key: "day",
      label: "Town",
      available: (phase === "DAY" || phase === "VOTING") && alive,
    },
    {
      key: "mafia",
      label: "Mafia",
      available: yourRole === "MAFIA" && phase === "NIGHT" && alive,
    },
    {
      key: "dead",
      label: "Dead",
      available: !alive,
    },
  ];

  const availableTabs = tabs.filter((t) => t.available);
  const defaultTab = availableTabs[0]?.key || "day";
  const [activeTab, setActiveTab] = useState<ChatTab>(defaultTab);

  // Reset tab when available tabs change
  const currentTabAvailable = availableTabs.some((t) => t.key === activeTab);
  const effectiveTab = currentTabAvailable ? activeTab : defaultTab;

  const messagesMap: Record<ChatTab, ChatMessage[]> = {
    day: dayMessages,
    mafia: mafiaMessages,
    dead: deadMessages,
  };

  const currentMessages = messagesMap[effectiveTab] || [];
  const canSend = availableTabs.some((t) => t.key === effectiveTab);

  const handleSend = () => {
    if (!messageInput.trim() || !canSend) return;
    onSendMessage(effectiveTab, messageInput);
    setMessageInput("");
  };

  return (
    <div className="flex flex-col h-full">
      {/* Tab bar */}
      <div className="flex gap-1 mb-3">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => tab.available && setActiveTab(tab.key)}
            className={`
              px-3 py-1.5 rounded text-sm font-medium transition-colors
              ${effectiveTab === tab.key ? "bg-primary text-primary-foreground" : ""}
              ${tab.available ? "hover:bg-accent cursor-pointer" : "opacity-30 cursor-not-allowed"}
            `}
            disabled={!tab.available}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Messages */}
      <ScrollArea className="flex-1 pr-4 mb-3">
        <div className="space-y-3">
          {currentMessages.length === 0 && (
            <p className="text-center text-muted-foreground py-4 text-sm">No messages</p>
          )}
          {currentMessages.map((msg) => (
            <div key={msg.id} className="space-y-0.5">
              <div className="flex items-baseline gap-2">
                <span className={`font-medium text-sm ${msg.player === "System" ? "text-yellow-500" : ""}`}>
                  {msg.player}
                </span>
                <span className="text-muted-foreground text-xs">
                  {new Date(msg.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                </span>
              </div>
              <p className="text-sm text-muted-foreground">{msg.message}</p>
            </div>
          ))}
        </div>
      </ScrollArea>

      {/* Input */}
      {canSend && (
        <div className="flex gap-2">
          <Input
            placeholder="Type a message..."
            value={messageInput}
            onChange={(e) => setMessageInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSend()}
            className="text-sm"
          />
          <Button size="icon" onClick={handleSend}>
            <Send className="w-4 h-4" />
          </Button>
        </div>
      )}
    </div>
  );
}
