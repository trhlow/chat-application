export type RoomType = "DIRECT" | "GROUP" | string;

export interface ChatUser {
  id: string;
  username: string;
  email: string;
  displayName: string | null;
  avatar: string | null;
  online: boolean;
  lastSeenAt: string | null;
}

export interface ChatRoom {
  id: string;
  name: string | null;
  type: RoomType;
  avatar: string | null;
  avatarProvider: string | null;
  memberIds: string[];
  admins: string[];
  createdBy: string | null;
  ownerId: string | null;
  unreadCount: number;
  lastMessageAt: string | null;
  lastMessagePreview: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface MessageAttachment {
  id: string;
  messageId: string;
  url: string;
  type: string;
  fileName: string | null;
  size: number | null;
}

export interface ChatMessage {
  id: string;
  roomId: string;
  senderId: string;
  content: string;
  timestamp: string;
  status: string;
  deliveredToUserIds: string[];
  readByUserIds: string[];
  attachments: MessageAttachment[];
}

export interface MessagePage {
  items: ChatMessage[];
  nextBefore: string | null;
  hasMore: boolean;
}

export interface PresenceEvent {
  userId: string;
  online: boolean;
  lastSeenAt: string | null;
}

export interface RoomUnreadCount {
  roomId: string;
  unreadCount: number;
}
