import { API_URL } from "@/lib/config";
import type { ChatMessage, PresenceEvent } from "@/types/chat";

type StompHandler = (payload: string) => void;
type ConnectionHandler = (connected: boolean) => void;

interface Subscription {
  destination: string;
  handler: StompHandler;
}

const baseUrl = API_URL.replace(/\/api\/?$/, "");
const WS_URL =
  import.meta.env.VITE_WS_URL ??
  baseUrl.replace(/^http/, "ws").replace(/\/$/, "") + "/ws";

export const encodeStompFrame = (
  command: string,
  headers: Record<string, string>,
  body = "",
) => {
  const headerLines = Object.entries(headers).map(
    ([key, value]) => `${key}:${value}`,
  );

  return `${command}\n${headerLines.join("\n")}\n\n${body}\0`;
};

export const parseStompFrame = (frame: string) => {
  const separatorIndex = frame.indexOf("\n\n");
  const rawHeaders =
    separatorIndex === -1 ? frame : frame.slice(0, separatorIndex);
  const body =
    separatorIndex === -1 ? "" : frame.slice(separatorIndex + 2);
  const [command, ...headerRows] = rawHeaders.split("\n");
  const headers = Object.fromEntries(
    headerRows
      .map((row) => row.split(/:(.*)/s).slice(0, 2))
      .filter(([key]) => key),
  );

  return { command, headers, body };
};

export class ChatRealtimeClient {
  private socket: WebSocket | null = null;
  private connected = false;
  private subscriptionId = 0;
  private reconnectTimer: number | null = null;
  private heartbeatTimer: number | null = null;
  private reconnectAttempt = 0;
  private accessToken: string | null = null;
  private shouldReconnect = false;
  private readonly subscriptions = new Map<string, Subscription>();
  private readonly connectionHandlers = new Set<ConnectionHandler>();

  connect(accessToken: string) {
    this.accessToken = accessToken;
    this.shouldReconnect = true;
    this.openSocket();
  }

  onConnectionChange(handler: ConnectionHandler) {
    this.connectionHandlers.add(handler);
    handler(this.connected);

    return () => {
      this.connectionHandlers.delete(handler);
    };
  }

  private openSocket() {
    if (!this.accessToken || !this.shouldReconnect) {
      return;
    }

    this.clearReconnectTimer();
    this.closeSocket();

    const socket = new WebSocket(WS_URL);
    this.socket = socket;

    socket.addEventListener("open", () => {
      socket.send(
        encodeStompFrame("CONNECT", {
          "accept-version": "1.2",
          Authorization: `Bearer ${this.accessToken}`,
          "heart-beat": "10000,10000",
        }),
      );
    });

    socket.addEventListener("message", (event) => {
      this.handleFrame(String(event.data));
    });

    socket.addEventListener("close", () => {
      if (this.socket !== socket) {
        return;
      }

      this.socket = null;
      this.setConnected(false);
      this.stopHeartbeat();
      this.scheduleReconnect();
    });

    socket.addEventListener("error", () => {
      socket.close();
    });
  }

  disconnect() {
    this.shouldReconnect = false;
    this.accessToken = null;
    this.reconnectAttempt = 0;
    this.clearReconnectTimer();
    this.stopHeartbeat();

    if (this.socket?.readyState === WebSocket.OPEN && this.connected) {
      this.socket.send(encodeStompFrame("DISCONNECT", {}, ""));
    }

    this.closeSocket();
    this.setConnected(false);
    this.subscriptions.clear();
  }

  subscribe(destination: string, handler: StompHandler) {
    const id = `sub-${++this.subscriptionId}`;
    this.subscriptions.set(id, { destination, handler });

    if (this.connected) {
      this.socket?.send(encodeStompFrame("SUBSCRIBE", { id, destination }));
    }

    return () => {
      this.subscriptions.delete(id);
      if (this.connected) {
        this.socket?.send(encodeStompFrame("UNSUBSCRIBE", { id }));
      }
    };
  }

  sendMessage(roomId: string, content: string) {
    return this.send(`/app/rooms/${roomId}/messages`, { content });
  }

  sendMessageStatus(messageId: string, status: "delivered" | "seen") {
    return this.send(`/app/messages/${messageId}/status`, { status });
  }

  sendTyping(roomId: string, typing: boolean) {
    return this.send(`/app/rooms/${roomId}/typing`, { typing });
  }

  private send(destination: string, payload: unknown) {
    if (!this.connected || this.socket?.readyState !== WebSocket.OPEN) {
      return false;
    }

    this.socket.send(
      encodeStompFrame(
        "SEND",
        {
          destination,
          "content-type": "application/json",
        },
        JSON.stringify(payload),
      ),
    );

    return true;
  }

  private handleFrame(rawFrame: string) {
    const frames = rawFrame.split("\0").filter(Boolean);

    for (const frame of frames) {
      const { command, headers, body } = parseStompFrame(frame);

      if (command === "CONNECTED") {
        this.reconnectAttempt = 0;
        this.setConnected(true);
        this.startHeartbeat();
        this.subscriptions.forEach(({ destination }, id) => {
          this.socket?.send(encodeStompFrame("SUBSCRIBE", { id, destination }));
        });
        continue;
      }

      if (command === "MESSAGE") {
        const subscriptionId = headers.subscription;
        const subscription = subscriptionId
          ? this.subscriptions.get(subscriptionId)
          : undefined;
        subscription?.handler(body);
      }
    }
  }

  private setConnected(connected: boolean) {
    if (this.connected === connected) {
      return;
    }

    this.connected = connected;
    this.connectionHandlers.forEach((handler) => handler(connected));
  }

  private scheduleReconnect() {
    if (!this.shouldReconnect || this.reconnectTimer) {
      return;
    }

    const delay = Math.min(1000 * 2 ** this.reconnectAttempt, 15000);
    this.reconnectAttempt += 1;
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      this.openSocket();
    }, delay);
  }

  private clearReconnectTimer() {
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  private startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatTimer = window.setInterval(() => {
      if (this.socket?.readyState === WebSocket.OPEN && this.connected) {
        this.socket.send("\n");
      }
    }, 10000);
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      window.clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  private closeSocket() {
    const socket = this.socket;
    this.socket = null;

    if (!socket) {
      return;
    }

    socket.close();
  }
}

export const parseRealtimeMessage = (payload: string): ChatMessage | null => {
  try {
    return JSON.parse(payload) as ChatMessage;
  } catch (_error) {
    return null;
  }
};

export const parsePresenceEvent = (payload: string): PresenceEvent | null => {
  try {
    return JSON.parse(payload) as PresenceEvent;
  } catch (_error) {
    return null;
  }
};
