export type RoomType = "DIRECT" | "GROUP" | string;

export interface ChatUser {
  id: string;
  username: string;
  email?: string;
  displayName: string | null;
  avatar: string | null;
  avatarEndpoint?: string | null;
  online: boolean;
  lastSeenAt: string | null;
}

export interface ChatRoom {
  id: string;
  name: string | null;
  type: RoomType;
  avatar: string | null;
  avatarEndpoint?: string | null;
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

export interface FriendUser {
  id: string;
  username: string;
  displayName: string | null;
  avatarEndpoint: string | null;
  avatar: string | null;
}

export interface FriendRequest {
  id: string;
  requester: FriendUser;
  receiver: FriendUser;
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELED" | string;
  createdAt: string;
  respondedAt: string | null;
}

export interface Friendship {
  id: string;
  friend: FriendUser;
  createdAt: string;
}

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  displayName: string | null;
  bio: string | null;
  phone: string | null;
  themePreference: string | null;
  avatarEndpoint: string | null;
  avatar: string | null;
  online: boolean;
  lastSeenAt: string | null;
}
