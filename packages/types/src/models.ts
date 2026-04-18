export type EntityId = string;

export interface UserSummary {
  id: EntityId;
  username: string;
  displayName?: string;
  avatarUrl?: string;
}

export interface RoomSummary {
  id: EntityId;
  name: string;
  memberCount: number;
  updatedAt: string;
}

export interface MessageSummary {
  id: EntityId;
  roomId: EntityId;
  senderId: EntityId;
  content: string;
  createdAt: string;
}
