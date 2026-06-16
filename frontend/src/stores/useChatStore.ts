import { create } from "zustand";
import { toast } from "sonner";

import { chatApi } from "@/services/chatService";
import {
  ChatRealtimeClient,
  parsePresenceEvent,
  parseRealtimeMessage,
} from "@/lib/realtime-client";
import { sendWithSingleFallback } from "@/lib/message-sender";
import { useAuthStore } from "@/stores/useAuthStore";
import type {
  ChatMessage,
  ChatRoom,
  ChatUser,
  FriendRequest,
  Friendship,
  NotificationItem,
  NotificationRealtimeEvent,
  PresenceEvent,
  PublicUserProfile,
  RoomMember,
  TypingEvent,
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
  blockedUsers: PublicUserProfile[];
  notifications: NotificationItem[];
  notificationUnreadCount: number;
  typingByRoomId: Record<string, TypingEvent[]>;
  roomMembersByRoomId: Record<string, RoomMember[]>;
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
  setSelectedRoomId: (roomId: string | null) => void;
  fetchConversations: () => Promise<void>;
  fetchMessages: (roomId: string) => Promise<void>;
  fetchOlderMessages: (roomId: string) => Promise<void>;
  sendMessage: (content: string) => Promise<void>;
  sendAttachment: (file: File, content?: string) => Promise<void>;
  recallMessage: (messageId: string) => Promise<void>;
  deleteMessageForMe: (messageId: string) => Promise<void>;
  createDirectRoom: (memberId: string) => Promise<ChatRoom | null>;
  createGroupRoom: (payload: { name: string; memberIds: string[] }) => Promise<ChatRoom | null>;
  fetchFriends: () => Promise<void>;
  sendFriendRequest: (receiverId: string) => Promise<void>;
  acceptFriendRequest: (requestId: string, firstMessage?: string) => Promise<void>;
  rejectFriendRequest: (requestId: string) => Promise<void>;
  cancelFriendRequest: (requestId: string) => Promise<void>;
  unfriend: (friendId: string) => Promise<void>;
  blockUser: (userId: string) => Promise<void>;
  unblockUser: (userId: string) => Promise<void>;
  fetchBlockedUsers: () => Promise<void>;
  fetchNotifications: () => Promise<void>;
  markNotificationRead: (notificationId: string) => Promise<void>;
  markAllNotificationsRead: () => Promise<void>;
  sendTyping: (typing: boolean) => void;
  fetchRoomMembers: (roomId: string) => Promise<void>;
  refreshRoom: (room: ChatRoom) => void;
  removeRoomLocally: (roomId: string) => void;
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
const roomSubscriptions = new Map<string, () => void>();
let presenceUnsubscribe: (() => void) | null = null;
let notificationUnsubscribe: (() => void) | null = null;
let connectionUnsubscribe: (() => void) | null = null;

const clearRealtimeSubscriptions = () => {
  roomSubscriptions.forEach((unsubscribe) => unsubscribe());
  roomSubscriptions.clear();
  presenceUnsubscribe?.();
  presenceUnsubscribe = null;
  notificationUnsubscribe?.();
  notificationUnsubscribe = null;
  connectionUnsubscribe?.();
  connectionUnsubscribe = null;
};

const syncRoomSubscriptions = (roomIds: string[]) => {
  const expectedRoomIds = new Set(roomIds);

  roomSubscriptions.forEach((unsubscribe, roomId) => {
    if (!expectedRoomIds.has(roomId)) {
      unsubscribe();
      roomSubscriptions.delete(roomId);
    }
  });

  expectedRoomIds.forEach((roomId) => {
    if (roomSubscriptions.has(roomId)) {
      return;
    }

    const unsubscribeMessage = realtimeClient.subscribe(
      `/topic/rooms/${roomId}/messages`,
      (payload) => {
        const message = parseRealtimeMessage(payload);
        if (message) {
          useChatStore.getState().handleIncomingMessage(message);
        }
      },
    );
    const unsubscribeStatus = realtimeClient.subscribe(
      `/topic/rooms/${roomId}/status`,
      (payload) => {
        const message = parseRealtimeMessage(payload);
        if (message) {
          useChatStore.getState().handleMessageStatus(message);
        }
      },
    );
    const unsubscribeTyping = realtimeClient.subscribe(
      `/topic/rooms/${roomId}/typing`,
      (payload) => {
        try {
          const event = JSON.parse(payload) as TypingEvent;
          useChatStore.setState((state) => ({
            typingByRoomId: {
              ...state.typingByRoomId,
              [roomId]: event.typing
                ? [
                    ...(state.typingByRoomId[roomId] ?? []).filter(
                      (item) => item.userId !== event.userId,
                    ),
                    event,
                  ]
                : (state.typingByRoomId[roomId] ?? []).filter(
                    (item) => item.userId !== event.userId,
                  ),
            },
          }));
        } catch (_error) {
          // Ignore malformed realtime payloads.
        }
      },
    );

    roomSubscriptions.set(roomId, () => {
      unsubscribeMessage();
      unsubscribeStatus();
      unsubscribeTyping();
    });
  });
};

const normalizeRoom = (room: ChatRoom): ChatRoom => ({
  ...room,
  memberIds: room.memberIds ?? [],
  admins: room.admins ?? [],
  unreadCount: room.unreadCount ?? 0,
});

const normalizeMessage = (message: ChatMessage): ChatMessage => ({
  ...message,
  content: message.content ?? "",
  deliveredToUserIds: message.deliveredToUserIds ?? [],
  readByUserIds: message.readByUserIds ?? [],
  attachments: message.attachments ?? [],
});

const sortRooms = (rooms: ChatRoom[]) =>
  rooms.map(normalizeRoom).sort((a, b) => {
    const aTime = a.lastMessageAt ?? a.updatedAt ?? a.createdAt ?? "";
    const bTime = b.lastMessageAt ?? b.updatedAt ?? b.createdAt ?? "";
    return bTime.localeCompare(aTime);
  });

const uniqueMessages = (messages: ChatMessage[]) => {
  const byId = new Map<string, ChatMessage>();
  messages.forEach((message) => {
    const normalizedMessage = normalizeMessage(message);
    byId.set(normalizedMessage.id, normalizedMessage);
  });
  return [...byId.values()].sort((a, b) =>
    a.timestamp.localeCompare(b.timestamp),
  );
};

const mergeRooms = (rooms: ChatRoom[], room: ChatRoom) =>
  sortRooms([normalizeRoom(room), ...rooms.filter((item) => item.id !== room.id)]);

export const getNextUnreadCount = ({
  currentCount,
  isSelected,
  alreadyKnown,
}: {
  currentCount: number;
  isSelected: boolean;
  alreadyKnown: boolean;
}) => {
  if (isSelected) return 0;
  if (alreadyKnown) return currentCount;
  return currentCount + 1;
};

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
  blockedUsers: [],
  notifications: [],
  notificationUnreadCount: 0,
  typingByRoomId: {},
  roomMembersByRoomId: {},
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
  setSelectedRoomId: (roomId) => {
    set({ selectedRoomId: roomId });

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
      const [roomsResponse, unreadResponse] = await Promise.all([
        chatApi.getRooms(),
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
      });
      syncRoomSubscriptions(rooms.map((room) => room.id));
    } catch (_error) {
      const message = "Không thể tải danh sách cuộc trò chuyện.";
      set({ error: message });
      toast.error(message);
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
      const message = "Không thể tải tin nhắn.";
      set({ error: message });
      toast.error(message);
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
      const message = "Không thể tải thêm tin nhắn cũ.";
      set((state) => ({
        error: message,
        messagePagesByRoomId: {
          ...state.messagePagesByRoomId,
          [roomId]: { ...pageState, loadingOlder: false },
        },
      }));
      toast.error(message);
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
      const response = await sendWithSingleFallback({
        sendRealtime: () =>
          realtimeClient.sendMessage(selectedRoomId, trimmedContent),
        sendRest: () => chatApi.sendMessage(selectedRoomId, trimmedContent),
      });

      if (response) {
        get().handleIncomingMessage(response.data);
      }
    } catch (_error) {
      const message = "Không thể gửi tin nhắn. Vui lòng thử lại.";
      set({ error: message });
      toast.error(message);
    } finally {
      set({ isSending: false });
    }
  },
  sendAttachment: async (file, content) => {
    const roomId = get().selectedRoomId;
    if (!roomId) return;
    set({ isSending: true, error: null });
    try {
      const response = await chatApi.sendAttachment(roomId, file, content);
      get().handleIncomingMessage(response.data);
      toast.success("Đã gửi tệp đính kèm.");
    } catch (_error) {
      toast.error("Không thể gửi tệp đính kèm.");
    } finally {
      set({ isSending: false });
    }
  },
  recallMessage: async (messageId) => {
    try {
      const response = await chatApi.recallMessage(messageId);
      get().handleMessageStatus(response.data);
      toast.success("Đã thu hồi tin nhắn.");
    } catch (_error) {
      toast.error("Không thể thu hồi tin nhắn.");
    }
  },
  deleteMessageForMe: async (messageId) => {
    try {
      await chatApi.deleteMessageForMe(messageId);
      set((state) => ({
        messagesByRoomId: Object.fromEntries(
          Object.entries(state.messagesByRoomId).map(([roomId, messages]) => [
            roomId,
            messages.filter((message) => message.id !== messageId),
          ]),
        ),
      }));
      toast.success("Đã xóa tin nhắn ở phía bạn.");
    } catch (_error) {
      toast.error("Không thể xóa tin nhắn.");
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
      syncRoomSubscriptions([...get().rooms.map((item) => item.id), room.id]);
      get().setSelectedRoomId(room.id);
      return room;
    } catch (_error) {
      const message = "Không thể tạo cuộc trò chuyện trực tiếp.";
      set({ error: message });
      toast.error(message);
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
      syncRoomSubscriptions([...get().rooms.map((item) => item.id), room.id]);
      get().setSelectedRoomId(room.id);
      toast.success("Đã tạo nhóm chat.");
      return room;
    } catch (_error) {
      const message = "Không thể tạo nhóm mới. Nhóm cần tối thiểu 3 thành viên.";
      set({ error: message });
      toast.error(message);
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
        usersById: {
          ...get().usersById,
          ...Object.fromEntries(
            [
              ...friends.data.map((item) => item.friend),
              ...incoming.data.flatMap((item) => [item.requester, item.receiver]),
              ...outgoing.data.flatMap((item) => [item.requester, item.receiver]),
            ].map((user) => [
              user.id,
              {
                ...user,
                avatar: user.avatarEndpoint ?? user.avatar ?? null,
                online: get().usersById[user.id]?.online ?? false,
                lastSeenAt: get().usersById[user.id]?.lastSeenAt ?? null,
              },
            ]),
          ),
        },
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
      toast.success("Đã gửi lời mời kết bạn.");
    } catch (_error) {
      const message = "Không thể gửi lời mời kết bạn.";
      set({ error: message });
      toast.error(message);
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
      toast.success("Đã chấp nhận lời mời kết bạn.");
    } catch (_error) {
      const message = "Không thể chấp nhận lời mời kết bạn.";
      set({ error: message });
      toast.error(message);
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
      toast.success("Đã từ chối lời mời kết bạn.");
    } catch (_error) {
      const message = "Không thể từ chối lời mời kết bạn.";
      set({ error: message });
      toast.error(message);
    } finally {
      set({ isMutating: false });
    }
  },
  cancelFriendRequest: async (requestId) => {
    try {
      await chatApi.cancelFriendRequest(requestId);
      set((state) => ({
        outgoingFriendRequests: state.outgoingFriendRequests.filter((item) => item.id !== requestId),
      }));
      toast.success("Đã thu hồi lời mời kết bạn.");
    } catch (_error) {
      toast.error("Không thể thu hồi lời mời.");
    }
  },
  unfriend: async (friendId) => {
    try {
      await chatApi.unfriend(friendId);
      set((state) => ({ friends: state.friends.filter((item) => item.friend.id !== friendId) }));
      toast.success("Đã hủy kết bạn.");
    } catch (_error) {
      toast.error("Không thể hủy kết bạn.");
    }
  },
  blockUser: async (userId) => {
    try {
      await chatApi.blockUser(userId);
      await Promise.all([get().fetchFriends(), get().fetchBlockedUsers()]);
      toast.success("Đã chặn người dùng.");
    } catch (_error) {
      toast.error("Không thể chặn người dùng.");
    }
  },
  unblockUser: async (userId) => {
    try {
      await chatApi.unblockUser(userId);
      await get().fetchBlockedUsers();
      toast.success("Đã bỏ chặn người dùng.");
    } catch (_error) {
      toast.error("Không thể bỏ chặn người dùng.");
    }
  },
  fetchBlockedUsers: async () => {
    try {
      const response = await chatApi.getBlockedUsers();
      set({ blockedUsers: response.data });
    } catch (_error) {
      set({ blockedUsers: [] });
    }
  },
  fetchNotifications: async () => {
    try {
      const [items, count] = await Promise.all([
        chatApi.getNotifications(),
        chatApi.getNotificationUnreadCount(),
      ]);
      set({ notifications: items.data.items, notificationUnreadCount: count.data.unreadCount });
    } catch (_error) {
      toast.error("Không thể tải thông báo.");
    }
  },
  markNotificationRead: async (notificationId) => {
    const response = await chatApi.markNotificationRead(notificationId);
    set((state) => ({
      notifications: state.notifications.map((item) =>
        item.id === notificationId ? response.data : item,
      ),
      notificationUnreadCount: Math.max(0, state.notificationUnreadCount - 1),
    }));
  },
  markAllNotificationsRead: async () => {
    await chatApi.markAllNotificationsRead();
    set((state) => ({
      notifications: state.notifications.map((item) => ({ ...item, read: true })),
      notificationUnreadCount: 0,
    }));
  },
  sendTyping: (typing) => {
    const roomId = get().selectedRoomId;
    if (roomId) realtimeClient.sendTyping(roomId, typing);
  },
  fetchRoomMembers: async (roomId) => {
    const response = await chatApi.getRoomMembers(roomId);
    set((state) => ({
      roomMembersByRoomId: { ...state.roomMembersByRoomId, [roomId]: response.data },
    }));
  },
  refreshRoom: (room) => {
    set((state) => ({ rooms: mergeRooms(state.rooms, room) }));
  },
  removeRoomLocally: (roomId) => {
    set((state) => ({
      rooms: state.rooms.filter((room) => room.id !== roomId),
      selectedRoomId: state.selectedRoomId === roomId ? null : state.selectedRoomId,
    }));
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
          .filter((message) => !(message.deliveredToUserIds ?? []).includes(response.data.id))
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
      toast.success("Đã cập nhật hồ sơ.");
    } catch (_error) {
      const message = "Không thể cập nhật hồ sơ.";
      set({ error: message });
      toast.error(message);
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
      toast.success("Đã cập nhật ảnh đại diện.");
    } catch (_error) {
      const message = "Không thể tải ảnh đại diện lên.";
      set({ error: message });
      toast.error(message);
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
      .filter((message) => !(message.readByUserIds ?? []).includes(currentUserId))
      .forEach((message) => {
        get().markMessageStatus(message, "seen");
      });
  },
  markMessageStatus: (message, status) => {
    const currentUserId = get().profile?.id;
    if (!currentUserId || message.senderId === currentUserId) {
      return;
    }

    if (status === "delivered" && (message.deliveredToUserIds ?? []).includes(currentUserId)) {
      return;
    }

    if (status === "seen" && (message.readByUserIds ?? []).includes(currentUserId)) {
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
    clearRealtimeSubscriptions();
    let connectedOnce = false;
    connectionUnsubscribe = realtimeClient.onConnectionChange((connected) => {
      set({ realtimeConnected: connected });
      if (!connected && connectedOnce) {
        toast.warning("Mất kết nối realtime. Đang thử kết nối lại...");
        return;
      }

      if (!connected) return;
      if (connectedOnce) {
        toast.success("Đã kết nối lại realtime.");
      }
      connectedOnce = true;
      syncRoomSubscriptions(get().rooms.map((room) => room.id));
    });
    presenceUnsubscribe = realtimeClient.subscribe("/user/queue/presence", (payload) => {
      const event = parsePresenceEvent(payload);
      if (event) {
        get().handlePresence(event);
      }
    });
    notificationUnsubscribe = realtimeClient.subscribe("/user/queue/notifications", (payload) => {
      try {
        const event = JSON.parse(payload) as NotificationRealtimeEvent;
        set((state) => ({
          notifications: event.notification
            ? [event.notification, ...state.notifications.filter((item) => item.id !== event.notification?.id)]
            : state.notifications,
          notificationUnreadCount: event.unreadCount,
        }));
      } catch (_error) {
        // Ignore malformed realtime payloads.
      }
    });
    syncRoomSubscriptions(get().rooms.map((room) => room.id));
    realtimeClient.connect(accessToken);
  },
  disconnectRealtime: () => {
    clearRealtimeSubscriptions();
    realtimeClient.disconnect();
    set({ realtimeConnected: false });
  },
  handleIncomingMessage: (message) => {
    set((state) => {
      const roomMessages = state.messagesByRoomId[message.roomId] ?? [];
      const alreadyKnown = roomMessages.some((item) => item.id === message.id);
      const selectedRoomId = state.selectedRoomId;
      const rooms = state.rooms.map((room) =>
        room.id === message.roomId
          ? {
              ...room,
              lastMessageAt: message.timestamp,
              lastMessagePreview: message.content,
              unreadCount: getNextUnreadCount({
                currentCount: room.unreadCount,
                isSelected: selectedRoomId === message.roomId,
                alreadyKnown,
              }),
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
      if ((message.deliveredToUserIds ?? []).includes(currentUserId)) {
        statusUpdatesInFlight.delete(`${message.id}:delivered`);
      }
      if ((message.readByUserIds ?? []).includes(currentUserId)) {
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
