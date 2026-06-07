import { create } from "zustand";

import { chatApi } from "@/lib/chat-client";
import {
  ChatRealtimeClient,
  parsePresenceEvent,
  parseRealtimeMessage,
} from "@/lib/realtime-client";
import { useAuthStore } from "@/store/auth-store";
import type {
  ChatMessage,
  ChatRoom,
  ChatUser,
  FriendRequest,
  Friendship,
  PresenceEvent,
  UserProfile,
} from "@/types/chat";

interface MessagePageState {
  nextBefore: string | null;
  hasMore: boolean;
  loadingOlder: boolean;
}

interface ChatState {
  rooms: ChatRoom[];
  usersById: Record<string, ChatUser>;
  messagesByRoomId: Record<string, ChatMessage[]>;
  messagePagesByRoomId: Record<string, MessagePageState>;
  friends: Friendship[];
  incomingFriendRequests: FriendRequest[];
  outgoingFriendRequests: FriendRequest[];
  profile: UserProfile | null;
  selectedRoomId: string | null;
  isLoadingRooms: boolean;
  isLoadingMessages: boolean;
  isLoadingFriends: boolean;
  isLoadingProfile: boolean;
  isSending: boolean;
  isMutating: boolean;
  error: string | null;
  realtimeConnected: boolean;
  activeUnsubscribe: (() => void) | null;
  activeStatusUnsubscribe: (() => void) | null;
  setSelectedRoomId: (roomId: string | null) => void;
  fetchConversations: () => Promise<void>;
  fetchMessages: (roomId: string) => Promise<void>;
  fetchOlderMessages: (roomId: string) => Promise<void>;
  sendMessage: (content: string) => Promise<void>;
  createDirectRoom: (memberId: string) => Promise<ChatRoom | null>;
  createGroupRoom: (payload: { name: string; memberIds: string[] }) => Promise<ChatRoom | null>;
  fetchFriends: () => Promise<void>;
  sendFriendRequest: (receiverId: string) => Promise<void>;
  acceptFriendRequest: (requestId: string, firstMessage?: string) => Promise<void>;
  rejectFriendRequest: (requestId: string) => Promise<void>;
  fetchProfile: () => Promise<void>;
  updateProfile: (payload: {
    username: string;
    displayName: string;
    bio: string;
    phone: string;
    themePreference: string;
  }) => Promise<void>;
  uploadAvatar: (file: File) => Promise<void>;
  markVisibleMessages: (roomId: string) => void;
  markMessageStatus: (message: ChatMessage, status: "delivered" | "seen") => void;
  connectRealtime: (accessToken: string) => void;
  disconnectRealtime: () => void;
  handleIncomingMessage: (message: ChatMessage) => void;
  handleMessageStatus: (message: ChatMessage) => void;
  handlePresence: (event: PresenceEvent) => void;
}

const realtimeClient = new ChatRealtimeClient();
const statusUpdatesInFlight = new Set<string>();

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

const mergeRooms = (rooms: ChatRoom[], room: ChatRoom) =>
  sortRooms([room, ...rooms.filter((item) => item.id !== room.id)]);

const getProfileAvatar = (profile: UserProfile) =>
  profile.avatarEndpoint ?? profile.avatar;

const mapProfileToAuthUser = (profile: UserProfile) => ({
  fullName: profile.displayName?.trim() || profile.username,
  username: profile.username,
  email: profile.email,
  avatarUrl: getProfileAvatar(profile),
  isOnline: profile.online,
  lastSeenAt: profile.lastSeenAt,
});

export const useChatStore = create<ChatState>((set, get) => ({
  rooms: [],
  usersById: {},
  messagesByRoomId: {},
  messagePagesByRoomId: {},
  friends: [],
  incomingFriendRequests: [],
  outgoingFriendRequests: [],
  profile: null,
  selectedRoomId: null,
  isLoadingRooms: false,
  isLoadingMessages: false,
  isLoadingFriends: false,
  isLoadingProfile: false,
  isSending: false,
  isMutating: false,
  error: null,
  realtimeConnected: false,
  activeUnsubscribe: null,
  activeStatusUnsubscribe: null,
  setSelectedRoomId: (roomId) => {
    const previousUnsubscribe = get().activeUnsubscribe;
    const previousStatusUnsubscribe = get().activeStatusUnsubscribe;
    previousUnsubscribe?.();
    previousStatusUnsubscribe?.();

    const messageUnsubscribe = roomId
      ? realtimeClient.subscribe(`/topic/rooms/${roomId}/messages`, (payload) => {
          const message = parseRealtimeMessage(payload);
          if (message) {
            get().handleIncomingMessage(message);
          }
        })
      : null;
    const statusUnsubscribe = roomId
      ? realtimeClient.subscribe(`/topic/rooms/${roomId}/status`, (payload) => {
          const message = parseRealtimeMessage(payload);
          if (message) {
            get().handleMessageStatus(message);
          }
        })
      : null;

    set({
      selectedRoomId: roomId,
      activeUnsubscribe: messageUnsubscribe,
      activeStatusUnsubscribe: statusUnsubscribe,
    });

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
        avatar: room.avatarEndpoint ?? room.avatar,
        unreadCount: unreadByRoomId.get(room.id) ?? room.unreadCount ?? 0,
      }));

      set({
        rooms: sortRooms(rooms),
        usersById: Object.fromEntries(
          usersResponse.data.map((user) => [
            user.id,
            { ...user, avatar: user.avatarEndpoint ?? user.avatar ?? null },
          ]),
        ),
      });
    } catch (_error) {
      set({ error: "Không thể tải danh sách cuộc trò chuyện." });
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
        messagePagesByRoomId: {
          ...state.messagePagesByRoomId,
          [roomId]: {
            nextBefore: response.data.nextBefore,
            hasMore: response.data.hasMore,
            loadingOlder: false,
          },
        },
      }));
      get().markVisibleMessages(roomId);
    } catch (_error) {
      set({ error: "Không thể tải tin nhắn." });
    } finally {
      set({ isLoadingMessages: false });
    }
  },
  fetchOlderMessages: async (roomId) => {
    const pageState = get().messagePagesByRoomId[roomId];
    if (!pageState?.hasMore || pageState.loadingOlder) {
      return;
    }

    set((state) => ({
      messagePagesByRoomId: {
        ...state.messagePagesByRoomId,
        [roomId]: { ...pageState, loadingOlder: true },
      },
    }));

    try {
      const response = await chatApi.getMessages(roomId, pageState.nextBefore);
      set((state) => ({
        messagesByRoomId: {
          ...state.messagesByRoomId,
          [roomId]: uniqueMessages([
            ...response.data.items,
            ...(state.messagesByRoomId[roomId] ?? []),
          ]),
        },
        messagePagesByRoomId: {
          ...state.messagePagesByRoomId,
          [roomId]: {
            nextBefore: response.data.nextBefore,
            hasMore: response.data.hasMore,
            loadingOlder: false,
          },
        },
      }));
    } catch (_error) {
      set((state) => ({
        error: "Không thể tải thêm tin nhắn cũ.",
        messagePagesByRoomId: {
          ...state.messagePagesByRoomId,
          [roomId]: { ...pageState, loadingOlder: false },
        },
      }));
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
  createDirectRoom: async (memberId) => {
    set({ isMutating: true, error: null });
    try {
      const response = await chatApi.createRoom({
        type: "direct",
        memberIds: [memberId],
      });
      const room = { ...response.data, avatar: response.data.avatarEndpoint ?? response.data.avatar };
      set((state) => ({ rooms: mergeRooms(state.rooms, room) }));
      get().setSelectedRoomId(room.id);
      return room;
    } catch (_error) {
      set({ error: "Không thể tạo cuộc trò chuyện trực tiếp." });
      return null;
    } finally {
      set({ isMutating: false });
    }
  },
  createGroupRoom: async ({ name, memberIds }) => {
    set({ isMutating: true, error: null });
    try {
      const response = await chatApi.createRoom({
        name: name.trim(),
        type: "group",
        memberIds,
      });
      const room = { ...response.data, avatar: response.data.avatarEndpoint ?? response.data.avatar };
      set((state) => ({ rooms: mergeRooms(state.rooms, room) }));
      get().setSelectedRoomId(room.id);
      return room;
    } catch (_error) {
      set({ error: "Không thể tạo nhóm mới. Nhóm cần tối thiểu 3 thành viên." });
      return null;
    } finally {
      set({ isMutating: false });
    }
  },
  fetchFriends: async () => {
    set({ isLoadingFriends: true, error: null });
    try {
      const [friends, incoming, outgoing] = await Promise.all([
        chatApi.getFriends(),
        chatApi.getIncomingFriendRequests(),
        chatApi.getOutgoingFriendRequests(),
      ]);
      set({
        friends: friends.data,
        incomingFriendRequests: incoming.data,
        outgoingFriendRequests: outgoing.data,
      });
    } catch (_error) {
      set({ error: "Không thể tải danh sách bạn bè." });
    } finally {
      set({ isLoadingFriends: false });
    }
  },
  sendFriendRequest: async (receiverId) => {
    set({ isMutating: true, error: null });
    try {
      const response = await chatApi.sendFriendRequest(receiverId);
      set((state) => ({
        outgoingFriendRequests: [response.data, ...state.outgoingFriendRequests],
      }));
    } catch (_error) {
      set({ error: "Không thể gửi lời mời kết bạn." });
    } finally {
      set({ isMutating: false });
    }
  },
  acceptFriendRequest: async (requestId, firstMessage) => {
    set({ isMutating: true, error: null });
    try {
      const response = await chatApi.acceptFriendRequest(requestId);
      const friend = response.data.requester;
      const roomResponse = await chatApi.createRoom({
        type: "direct",
        memberIds: [friend.id],
      });
      const room = {
        ...roomResponse.data,
        avatar: roomResponse.data.avatarEndpoint ?? roomResponse.data.avatar,
      };
      if (firstMessage?.trim()) {
        await chatApi.sendMessage(room.id, firstMessage.trim());
      }
      set((state) => ({
        incomingFriendRequests: state.incomingFriendRequests.filter((item) => item.id !== requestId),
        rooms: mergeRooms(state.rooms, room),
      }));
      get().setSelectedRoomId(room.id);
      await Promise.all([get().fetchFriends(), get().fetchConversations()]);
    } catch (_error) {
      set({ error: "Không thể chấp nhận lời mời kết bạn." });
    } finally {
      set({ isMutating: false });
    }
  },
  rejectFriendRequest: async (requestId) => {
    set({ isMutating: true, error: null });
    try {
      await chatApi.rejectFriendRequest(requestId);
      set((state) => ({
        incomingFriendRequests: state.incomingFriendRequests.filter((item) => item.id !== requestId),
      }));
    } catch (_error) {
      set({ error: "Không thể từ chối lời mời kết bạn." });
    } finally {
      set({ isMutating: false });
    }
  },
  fetchProfile: async () => {
    set({ isLoadingProfile: true, error: null });
    try {
      const response = await chatApi.getProfile();
      set({ profile: response.data });
      useAuthStore.getState().updateUser(mapProfileToAuthUser(response.data));
      const state = get();
      const selectedRoomId = state.selectedRoomId;
      if (selectedRoomId) {
        state.markVisibleMessages(selectedRoomId);
      }
      Object.entries(state.messagesByRoomId).forEach(([roomId, messages]) => {
        if (roomId === selectedRoomId) {
          return;
        }

        messages
          .filter((message) => message.senderId !== response.data.id)
          .filter((message) => !message.deliveredToUserIds.includes(response.data.id))
          .forEach((message) => state.markMessageStatus(message, "delivered"));
      });
    } catch (_error) {
      set({ error: "Không thể tải hồ sơ." });
    } finally {
      set({ isLoadingProfile: false });
    }
  },
  updateProfile: async (payload) => {
    set({ isMutating: true, error: null });
    try {
      const response = await chatApi.updateProfile({
        username: payload.username.trim(),
        displayName: payload.displayName.trim() || null,
        bio: payload.bio.trim() || null,
        phone: payload.phone.trim() || null,
        themePreference: payload.themePreference,
      });
      set({ profile: response.data });
      useAuthStore.getState().updateUser(mapProfileToAuthUser(response.data));
    } catch (_error) {
      set({ error: "Không thể cập nhật hồ sơ." });
    } finally {
      set({ isMutating: false });
    }
  },
  uploadAvatar: async (file) => {
    set({ isMutating: true, error: null });
    try {
      const response = await chatApi.uploadAvatar(file);
      set({ profile: response.data });
      useAuthStore.getState().updateUser(mapProfileToAuthUser(response.data));
    } catch (_error) {
      set({ error: "Không thể tải ảnh đại diện lên." });
    } finally {
      set({ isMutating: false });
    }
  },
  markVisibleMessages: (roomId) => {
    const currentUserId = get().profile?.id;
    if (!currentUserId) {
      return;
    }
    const messages = get().messagesByRoomId[roomId] ?? [];
    messages
      .filter((message) => message.senderId !== currentUserId)
      .filter((message) => !message.readByUserIds.includes(currentUserId))
      .forEach((message) => {
        get().markMessageStatus(message, "seen");
      });
  },
  markMessageStatus: (message, status) => {
    const currentUserId = get().profile?.id;
    if (!currentUserId || message.senderId === currentUserId) {
      return;
    }

    if (status === "delivered" && message.deliveredToUserIds.includes(currentUserId)) {
      return;
    }

    if (status === "seen" && message.readByUserIds.includes(currentUserId)) {
      return;
    }

    const requestKey = `${message.id}:${status}`;
    if (statusUpdatesInFlight.has(requestKey)) {
      return;
    }

    statusUpdatesInFlight.add(requestKey);
    const wasSentRealtime = realtimeClient.sendMessageStatus(message.id, status);

    if (wasSentRealtime) {
      window.setTimeout(() => {
        statusUpdatesInFlight.delete(requestKey);
      }, 5000);
      return;
    }

    void chatApi.updateMessageStatus(message.id, status)
      .then((response) => {
        get().handleMessageStatus(response.data);
      })
      .catch(() => undefined)
      .finally(() => {
        statusUpdatesInFlight.delete(requestKey);
      });
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
    set({
      realtimeConnected: false,
      activeUnsubscribe: null,
      activeStatusUnsubscribe: null,
    });
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

    const currentUserId = get().profile?.id;
    if (!currentUserId || message.senderId === currentUserId) {
      return;
    }

    if (get().selectedRoomId === message.roomId) {
      void chatApi.markRoomAsRead(message.roomId).catch(() => undefined);
      get().markVisibleMessages(message.roomId);
    } else {
      get().markMessageStatus(message, "delivered");
    }
  },
  handleMessageStatus: (message) => {
    const currentUserId = get().profile?.id;
    if (currentUserId) {
      if (message.deliveredToUserIds.includes(currentUserId)) {
        statusUpdatesInFlight.delete(`${message.id}:delivered`);
      }
      if (message.readByUserIds.includes(currentUserId)) {
        statusUpdatesInFlight.delete(`${message.id}:seen`);
      }
    }

    set((state) => {
      const roomMessages = state.messagesByRoomId[message.roomId] ?? [];
      return {
        messagesByRoomId: {
          ...state.messagesByRoomId,
          [message.roomId]: uniqueMessages([...roomMessages, message]),
        },
      };
    });
  },
  handlePresence: (event) => {
    set((state) => ({
      usersById: {
        ...state.usersById,
        [event.userId]: {
          ...(state.usersById[event.userId] ?? {
            id: event.userId,
            username: "unknown",
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
