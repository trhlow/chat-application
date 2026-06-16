import { apiClient } from "@/lib/axios";
import type {
  ChatMessage,
  ChatRoom,
  ChatUser,
  FriendRequest,
  Friendship,
  GroupJoinRequest,
  MessagePage,
  NotificationPage,
  NotificationItem,
  PublicUserProfile,
  RoomMember,
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
  sendAttachment: (roomId: string, file: File, content?: string) => {
    const formData = new FormData();
    formData.append("roomId", roomId);
    formData.append("file", file);
    if (content?.trim()) formData.append("content", content.trim());
    return apiClient.post<ChatMessage>("/messages/attachments", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
  recallMessage: (messageId: string) =>
    apiClient.put<ChatMessage>(`/messages/${messageId}/recall`),
  deleteMessageForMe: (messageId: string) =>
    apiClient.delete<void>(`/messages/${messageId}/me`),
  searchMessages: (roomId: string, keyword: string, page = 0) =>
    apiClient.get<MessagePage>(`/rooms/${roomId}/messages/search`, {
      params: { keyword, page, size: 20 },
    }),
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
  cancelFriendRequest: (requestId: string) =>
    apiClient.delete<void>(`/friends/requests/${requestId}`),
  unfriend: (friendId: string) => apiClient.delete<void>(`/friends/${friendId}`),
  blockUser: (userId: string) => apiClient.post<void>(`/friends/blocks/${userId}`),
  unblockUser: (userId: string) => apiClient.delete<void>(`/friends/blocks/${userId}`),
  getBlockedUsers: () => apiClient.get<PublicUserProfile[]>("/friends/blocks"),
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
  getUserProfile: (userId: string) =>
    apiClient.get<PublicUserProfile>(`/users/${userId}`),
  updateRoomName: (roomId: string, name: string) =>
    apiClient.patch<ChatRoom>(`/rooms/${roomId}/name`, { name }),
  uploadRoomAvatar: (roomId: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient.post<ChatRoom>(`/rooms/${roomId}/avatar`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
  getRoomMembers: (roomId: string) =>
    apiClient.get<RoomMember[]>(`/rooms/${roomId}/members`),
  addRoomMembers: (roomId: string, memberIds: string[]) =>
    apiClient.post<ChatRoom>(`/rooms/${roomId}/members`, { memberIds }),
  removeRoomMember: (roomId: string, memberId: string) =>
    apiClient.delete<ChatRoom>(`/rooms/${roomId}/members/${memberId}`),
  promoteRoomAdmin: (roomId: string, memberId: string) =>
    apiClient.post<ChatRoom>(`/rooms/${roomId}/admins/${memberId}`),
  demoteRoomAdmin: (roomId: string, memberId: string) =>
    apiClient.delete<ChatRoom>(`/rooms/${roomId}/admins/${memberId}`),
  transferRoomOwner: (roomId: string, memberId: string) =>
    apiClient.put<ChatRoom>(`/rooms/${roomId}/owner/${memberId}`),
  getJoinRequests: (roomId: string) =>
    apiClient.get<GroupJoinRequest[]>(`/rooms/${roomId}/join-requests`),
  createJoinRequest: (roomId: string, targetUserId: string) =>
    apiClient.post<GroupJoinRequest>(`/rooms/${roomId}/join-requests`, { targetUserId }),
  approveJoinRequest: (roomId: string, requestId: string) =>
    apiClient.post<ChatRoom>(`/rooms/${roomId}/join-requests/${requestId}/approve`),
  rejectJoinRequest: (roomId: string, requestId: string) =>
    apiClient.post<GroupJoinRequest>(`/rooms/${roomId}/join-requests/${requestId}/reject`),
  leaveRoom: (roomId: string) => apiClient.post<void>(`/rooms/${roomId}/leave`),
  dissolveRoom: (roomId: string) => apiClient.delete<void>(`/rooms/${roomId}`),
  getNotifications: (page = 0) =>
    apiClient.get<NotificationPage>("/notifications", { params: { page, size: 20 } }),
  getNotificationUnreadCount: () =>
    apiClient.get<{ unreadCount: number }>("/notifications/unread-count"),
  markNotificationRead: (notificationId: string) =>
    apiClient.patch<NotificationItem>(`/notifications/${notificationId}/read`),
  markAllNotificationsRead: () => apiClient.patch<void>("/notifications/read-all"),
};
