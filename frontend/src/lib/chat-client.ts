import { apiClient } from "@/lib/http";
import type {
  ChatMessage,
  ChatRoom,
  ChatUser,
  MessagePage,
  RoomUnreadCount,
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
  sendMessage: (roomId: string, content: string) =>
    apiClient.post<ChatMessage>("/messages", { roomId, content }),
};
