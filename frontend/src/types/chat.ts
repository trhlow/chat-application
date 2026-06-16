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
  settings?: GroupSettings | null;
}

export interface GroupSettings {
  sendMessagePermission: "ALL" | "ADMIN_ONLY" | string;
  editGroupInfoPermission: "ALL" | "ADMIN_ONLY" | string;
  inviteMemberPermission: "ALL" | "ADMIN_ONLY" | string;
  allowNewMemberReadHistory: boolean;
}

export interface MessageAttachment {
  id: string;
  messageId: string;
  downloadEndpoint: string;
  fileType: string;
  mimeType: string;
  fileSize: number | null;
  originalName: string | null;
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
  type?: string;
  replyToMessageId?: string | null;
  replyPreview?: string | null;
  recalled?: boolean;
  recalledAt?: string | null;
  clientMessageId?: string | null;
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

export interface TypingEvent {
  roomId: string;
  userId: string;
  username: string;
  typing: boolean;
  timestamp: string;
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

export interface PublicUserProfile {
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

export interface RoomMember extends PublicUserProfile {
  role: "OWNER" | "ADMIN" | "MEMBER" | string;
}

export interface GroupJoinRequest {
  id: string;
  roomId: string;
  requesterId: string;
  targetUserId: string;
  status: string;
  createdAt: string;
  respondedAt: string | null;
  respondedBy: string | null;
}

export interface NotificationItem {
  id: string;
  userId: string;
  type: string;
  title: string;
  message: string;
  relatedId: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationPage {
  items: NotificationItem[];
  page: number;
  size: number;
  hasMore: boolean;
}

export interface NotificationRealtimeEvent {
  eventType: string;
  notification: NotificationItem | null;
  unreadCount: number;
}
