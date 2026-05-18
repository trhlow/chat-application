import { API_URL } from "@/lib/config";
import type { ChatMessage, PresenceEvent } from "@/types/chat";

type StompHandler = (payload: string) => void;

interface Subscription {
  destination: string;
  handler: StompHandler;
}

const baseUrl = API_URL.replace(/\/api\/?$/, "");
const WS_URL =
  import.meta.env.VITE_WS_URL ??
  baseUrl.replace(/^http/, "ws").replace(/\/$/, "") + "/ws";

const encodeFrame = (
  command: string,
  headers: Record<string, string>,
  body = "",
) => {
  const headerLines = Object.entries(headers).map(
    ([key, value]) => `${key}:${value}`,
  );

  return `${command}\n${headerLines.join("\n")}\n\n${body}\0`;
};

export class ChatRealtimeClient {
  private socket: WebSocket | null = null;
  private connected = false;
  private subscriptionId = 0;
  private reconnectTimer: number | null = null;
  private readonly subscriptions = new Map<string, Subscription>();

  connect(accessToken: string) {
    this.disconnect();

    this.socket = new WebSocket(WS_URL);

    this.socket.addEventListener("open", () => {
      this.socket?.send(
        encodeFrame("CONNECT", {
          "accept-version": "1.2",
          Authorization: `Bearer ${accessToken}`,
          "heart-beat": "10000,10000",
        }),
      );
    });

    this.socket.addEventListener("message", (event) => {
      this.handleFrame(String(event.data));
    });

    this.socket.addEventListener("close", () => {
      this.connected = false;
    });
  }

  disconnect() {
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.socket?.readyState === WebSocket.OPEN && this.connected) {
      this.socket.send(encodeFrame("DISCONNECT", {}, ""));
    }

    this.socket?.close();
    this.socket = null;
    this.connected = false;
    this.subscriptions.clear();
  }

  subscribe(destination: string, handler: StompHandler) {
    const id = `sub-${++this.subscriptionId}`;
    this.subscriptions.set(id, { destination, handler });

    if (this.connected) {
      this.socket?.send(encodeFrame("SUBSCRIBE", { id, destination }));
    }

    return () => {
      this.subscriptions.delete(id);
      if (this.connected) {
        this.socket?.send(encodeFrame("UNSUBSCRIBE", { id }));
      }
    };
  }

  sendMessage(roomId: string, content: string) {
    return this.send(`/app/rooms/${roomId}/messages`, { content });
  }

  sendMessageStatus(messageId: string, status: "delivered" | "seen") {
    return this.send(`/app/messages/${messageId}/status`, { status });
  }

  private send(destination: string, payload: unknown) {
    if (!this.connected || this.socket?.readyState !== WebSocket.OPEN) {
      return false;
    }

    this.socket.send(
      encodeFrame(
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
      const [rawHeaders, body = ""] = frame.split("\n\n");
      const [command, ...headerRows] = rawHeaders.split("\n");
      const headers = Object.fromEntries(
        headerRows
          .map((row) => row.split(/:(.*)/s).slice(0, 2))
          .filter(([key]) => key),
      );

      if (command === "CONNECTED") {
        this.connected = true;
        this.subscriptions.forEach(({ destination }, id) => {
          this.socket?.send(encodeFrame("SUBSCRIBE", { id, destination }));
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
