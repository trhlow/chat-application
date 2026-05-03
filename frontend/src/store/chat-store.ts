import { create } from "zustand";

import { chatApi } from "@/lib/chat-client";
import {
  ChatRealtimeClient,
  parsePresenceEvent,
  parseRealtimeMessage,
} from "@/lib/realtime-client";
import type { ChatMessage, ChatRoom, ChatUser, PresenceEvent } from "@/types/chat";

interface ChatState {
  rooms: ChatRoom[];
  usersById: Record<string, ChatUser>;
  messagesByRoomId: Record<string, ChatMessage[]>;
  selectedRoomId: string | null;
  isLoadingRooms: boolean;
  isLoadingMessages: boolean;
  isSending: boolean;
  error: string | null;
  realtimeConnected: boolean;
  activeUnsubscribe: (() => void) | null;
  setSelectedRoomId: (roomId: string | null) => void;
  fetchConversations: () => Promise<void>;
  fetchMessages: (roomId: string) => Promise<void>;
  sendMessage: (content: string) => Promise<void>;
  connectRealtime: (accessToken: string) => void;
  disconnectRealtime: () => void;
  handleIncomingMessage: (message: ChatMessage) => void;
  handlePresence: (event: PresenceEvent) => void;
}

const realtimeClient = new ChatRealtimeClient();

const sortRooms = (rooms: ChatRoom[]) =>
  [...rooms].sort((a, b) => {
    const aTime = a.lastMessageAt ?? a.updatedAt ?? a.createdAt ?? "";
    const bTime = b.lastMessageAt ?? b.updatedAt ?? b.createdAt ?? "";
    return bTime.localeCompare(aTime);
  });

const uniqueMessages = (messages: ChatMessage[]) => {
  const byId = new Map<string, ChatMessage>();
  messages.forEach((message) => byId.set(message.id, message));
  return [...byId.values()].sort((a, b) =>
    a.timestamp.localeCompare(b.timestamp),
  );
};

export const useChatStore = create<ChatState>((set, get) => ({
  rooms: [],
  usersById: {},
  messagesByRoomId: {},
  selectedRoomId: null,
  isLoadingRooms: false,
  isLoadingMessages: false,
  isSending: false,
  error: null,
  realtimeConnected: false,
  activeUnsubscribe: null,
  setSelectedRoomId: (roomId) => {
    const previousUnsubscribe = get().activeUnsubscribe;
    previousUnsubscribe?.();

    const unsubscribe = roomId
      ? realtimeClient.subscribe(`/topic/rooms/${roomId}/messages`, (payload) => {
          const message = parseRealtimeMessage(payload);
          if (message) {
            get().handleIncomingMessage(message);
          }
        })
      : null;

    set({ selectedRoomId: roomId, activeUnsubscribe: unsubscribe });

    if (roomId) {
      void get().fetchMessages(roomId);
      void chatApi.markRoomAsRead(roomId).catch(() => undefined);
      set((state) => ({
        rooms: state.rooms.map((room) =>
          room.id === roomId ? { ...room, unreadCount: 0 } : room,
        ),
      }));
    }
  },
  fetchConversations: async () => {
    set({ isLoadingRooms: true, error: null });

    try {
      const [roomsResponse, usersResponse, unreadResponse] = await Promise.all([
        chatApi.getRooms(),
        chatApi.getUsers(),
        chatApi.getUnreadCounts().catch(() => ({ data: [] })),
      ]);
      const unreadByRoomId = new Map(
        unreadResponse.data.map((item) => [item.roomId, item.unreadCount]),
      );
      const rooms = roomsResponse.data.map((room) => ({
        ...room,
        unreadCount: unreadByRoomId.get(room.id) ?? room.unreadCount ?? 0,
      }));

      set({
        rooms: sortRooms(rooms),
        usersById: Object.fromEntries(
          usersResponse.data.map((user) => [user.id, user]),
        ),
      });
    } catch (_error) {
      set({ error: "Khong the tai danh sach cuoc tro chuyen." });
    } finally {
      set({ isLoadingRooms: false });
    }
  },
  fetchMessages: async (roomId) => {
    set({ isLoadingMessages: true, error: null });

    try {
      const response = await chatApi.getMessages(roomId);
      set((state) => ({
        messagesByRoomId: {
          ...state.messagesByRoomId,
          [roomId]: uniqueMessages(response.data.items),
        },
      }));
    } catch (_error) {
      set({ error: "Khong the tai tin nhan." });
    } finally {
      set({ isLoadingMessages: false });
    }
  },
  sendMessage: async (content) => {
    const selectedRoomId = get().selectedRoomId;
    const trimmedContent = content.trim();

    if (!selectedRoomId || !trimmedContent) {
      return;
    }

    set({ isSending: true, error: null });

    try {
      const wasSentRealtime = realtimeClient.sendMessage(
        selectedRoomId,
        trimmedContent,
      );

      if (!wasSentRealtime) {
        const response = await chatApi.sendMessage(
          selectedRoomId,
          trimmedContent,
        );
        get().handleIncomingMessage(response.data);
      }
    } catch (_error) {
      const response = await chatApi.sendMessage(selectedRoomId, trimmedContent);
      get().handleIncomingMessage(response.data);
    } finally {
      set({ isSending: false });
    }
  },
  connectRealtime: (accessToken) => {
    realtimeClient.connect(accessToken);
    realtimeClient.subscribe("/topic/presence", (payload) => {
      const event = parsePresenceEvent(payload);
      if (event) {
        get().handlePresence(event);
      }
    });
    set({ realtimeConnected: true });
  },
  disconnectRealtime: () => {
    realtimeClient.disconnect();
    set({ realtimeConnected: false, activeUnsubscribe: null });
  },
  handleIncomingMessage: (message) => {
    set((state) => {
      const roomMessages = state.messagesByRoomId[message.roomId] ?? [];
      const selectedRoomId = state.selectedRoomId;
      const rooms = state.rooms.map((room) =>
        room.id === message.roomId
          ? {
              ...room,
              lastMessageAt: message.timestamp,
              lastMessagePreview: message.content,
              unreadCount:
                selectedRoomId === message.roomId ? 0 : room.unreadCount + 1,
            }
          : room,
      );

      return {
        messagesByRoomId: {
          ...state.messagesByRoomId,
          [message.roomId]: uniqueMessages([...roomMessages, message]),
        },
        rooms: sortRooms(rooms),
      };
    });

    if (get().selectedRoomId === message.roomId) {
      void chatApi.markRoomAsRead(message.roomId).catch(() => undefined);
    }
  },
  handlePresence: (event) => {
    set((state) => ({
      usersById: {
        ...state.usersById,
        [event.userId]: {
          ...(state.usersById[event.userId] ?? {
            id: event.userId,
            username: "unknown",
            email: "",
            displayName: null,
            avatar: null,
          }),
          online: event.online,
          lastSeenAt: event.lastSeenAt,
        },
      },
    }));
  },
}));
