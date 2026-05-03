import { Client, IMessage, StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";

let client: Client | null = null;
let connectPromise: Promise<void> | null = null;
let refCount = 0;

function getToken(): string | null {
  const match = document.cookie.match(/(^| )jwt=([^;]+)/);
  return match ? decodeURIComponent(match[2]) : null;
}

export function getStompClient(): Client {
  if (!client) {
    client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      connectHeaders: {
        Authorization: `Bearer ${getToken()}`,
      },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });
  }
  return client;
}

/**
 * Acquire a connection to the STOMP broker. Each caller must release
 * with `releaseStomp()` when done. The actual socket only closes when
 * the reference count reaches zero.
 */
export function acquireStomp(): Promise<void> {
  refCount++;
  return connectStompInternal();
}

export function releaseStomp(): void {
  refCount = Math.max(0, refCount - 1);
  if (refCount === 0 && client) {
    client.deactivate();
    client = null;
    connectPromise = null;
  }
}

/** Force disconnect regardless of refcount (used on logout). */
export function forceDisconnectStomp(): void {
  refCount = 0;
  if (client) {
    client.deactivate();
    client = null;
    connectPromise = null;
  }
}

function connectStompInternal(): Promise<void> {
  if (connectPromise) return connectPromise;

  const stomp = getStompClient();

  stomp.connectHeaders = {
    Authorization: `Bearer ${getToken()}`,
  };

  if (stomp.connected) {
    return Promise.resolve();
  }

  connectPromise = new Promise<void>((resolve, reject) => {
    stomp.onConnect = () => {
      connectPromise = null;
      resolve();
    };
    stomp.onStompError = (frame) => {
      connectPromise = null;
      reject(new Error(frame.headers["message"] || "STOMP connection error"));
    };
    stomp.activate();
  });

  return connectPromise;
}

// Backwards-compatible aliases (old call sites still use these names)
export const connectStomp = acquireStomp;
export const disconnectStomp = releaseStomp;

export function subscribe(
  destination: string,
  callback: (message: IMessage) => void
): StompSubscription | null {
  const stomp = getStompClient();
  if (!stomp.connected) return null;
  return stomp.subscribe(destination, callback);
}

export function sendMessage(destination: string, body: Record<string, unknown>): void {
  const stomp = getStompClient();
  if (!stomp.connected) return;
  stomp.publish({
    destination,
    body: JSON.stringify(body),
  });
}
