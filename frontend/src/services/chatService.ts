import { apiClient } from "@/lib/axios";
import type {
  ChatMessage,
  ChatRoom,
  ChatUser,
  FriendRequest,
  Friendship,
  MessagePage,
  RoomUnreadCount,
  UserProfile,
} from "@/types/chat";

export const chatApi = {
  getRooms: () => apiClient.get<ChatRoom[]>("/rooms"),
  getUsers: () => apiClient.get<ChatUser[]>("/users"),
  getMessages: (roomId: string, before?: string | null) =>
    apiClient.get<MessagePage>("/messages", {
      params: {
        roomId,
        limit: 40,
        ...(before ? { before } : {}),
      },
    }),
  getUnreadCounts: () =>
    apiClient.get<RoomUnreadCount[]>("/messages/unread-counts"),
  markRoomAsRead: (roomId: string) =>
    apiClient.post<void>(`/messages/rooms/${roomId}/read`),
  updateMessageStatus: (messageId: string, status: "delivered" | "seen") =>
    apiClient.patch<ChatMessage>(`/messages/${messageId}/status`, { status }),
  sendMessage: (roomId: string, content: string) =>
    apiClient.post<ChatMessage>("/messages", { roomId, content }),
  createRoom: (payload: { name?: string | null; type: "direct" | "group"; memberIds: string[] }) =>
    apiClient.post<ChatRoom>("/rooms", payload),
  searchUsers: (query?: string) =>
    apiClient.get<ChatUser[]>("/users", {
      params: query?.trim() ? { query: query.trim() } : undefined,
    }),
  getFriends: () => apiClient.get<Friendship[]>("/friends"),
  sendFriendRequest: (receiverId: string) =>
    apiClient.post<FriendRequest>("/friends/requests", { receiverId }),
  getIncomingFriendRequests: () =>
    apiClient.get<FriendRequest[]>("/friends/requests/incoming"),
  getOutgoingFriendRequests: () =>
    apiClient.get<FriendRequest[]>("/friends/requests/outgoing"),
  acceptFriendRequest: (requestId: string) =>
    apiClient.post<FriendRequest>(`/friends/requests/${requestId}/accept`),
  rejectFriendRequest: (requestId: string) =>
    apiClient.post<FriendRequest>(`/friends/requests/${requestId}/reject`),
  getProfile: () => apiClient.get<UserProfile>("/users/me"),
  updateProfile: (payload: {
    username?: string;
    displayName?: string | null;
    bio?: string | null;
    phone?: string | null;
    themePreference?: string | null;
  }) => apiClient.put<UserProfile>("/users/me", payload),
  uploadAvatar: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient.post<UserProfile>("/users/me/avatar", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
};
