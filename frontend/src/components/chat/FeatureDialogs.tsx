import {
  Bell,
  Camera,
  CheckCheck,
  Crown,
  LogOut,
  Search,
  Shield,
  Trash2,
  UserMinus,
  UserPlus,
  X,
} from "lucide-react";
import { type ChangeEvent, type ReactNode, useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { toast } from "sonner";

import { UserAvatar } from "@/components/chat/chat-shell";
import { Button } from "@/components/ui/button";
import { chatApi } from "@/services/chatService";
import { useChatStore } from "@/stores/useChatStore";
import type { AuthUser } from "@/types/auth";
import type { ChatMessage, ChatRoom, FriendUser, GroupJoinRequest, PublicUserProfile, RoomMember } from "@/types/chat";

const EMPTY_ROOM_MEMBERS: RoomMember[] = [];

const Dialog = ({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) => (
  <div className="fixed inset-0 z-[70] grid place-items-center bg-slate-950/55 p-4 backdrop-blur-sm">
    <section className="max-h-[92dvh] w-full max-w-2xl overflow-hidden rounded-2xl border border-border bg-card shadow-2xl">
      <header className="flex items-center justify-between border-b border-border px-5 py-4">
        <h2 className="text-lg font-bold">{title}</h2>
        <Button variant="ghost" size="icon" onClick={onClose} aria-label="Đóng"><X className="h-4 w-4" /></Button>
      </header>
      <div className="pretty-scrollbar max-h-[calc(92dvh-70px)] overflow-y-auto p-5">{children}</div>
    </section>
  </div>
);

const nameOf = (user?: { displayName: string | null; username: string }) =>
  user?.displayName?.trim() || user?.username || "Người dùng";

export const NotificationCenter = () => {
  const notifications = useChatStore((state) => state.notifications);
  const unreadCount = useChatStore((state) => state.notificationUnreadCount);
  const fetchNotifications = useChatStore((state) => state.fetchNotifications);
  const markNotificationRead = useChatStore((state) => state.markNotificationRead);
  const markAllNotificationsRead = useChatStore((state) => state.markAllNotificationsRead);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    void fetchNotifications();
  }, [fetchNotifications]);

  useEffect(() => {
    if (!open) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [open]);

  return (
    <div className="relative">
      <Button variant="ghost" size="icon" className="relative text-white hover:bg-white/15 hover:text-white" onClick={() => setOpen((value) => !value)} aria-label="Thông báo">
        <Bell className="h-5 w-5" />
        {unreadCount > 0 ? <span className="absolute -right-1 -top-1 min-w-5 rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">{unreadCount > 99 ? "99+" : unreadCount}</span> : null}
      </Button>
      {open ? createPortal(
        <>
          <button type="button" className="fixed inset-0 z-[59] cursor-default bg-transparent" aria-label="Đóng thông báo" onClick={() => setOpen(false)} />
          <section className="fixed left-3 right-3 top-20 z-[60] overflow-hidden rounded-2xl border border-border bg-card text-card-foreground shadow-2xl md:left-[340px] md:right-auto md:w-[380px]">
            <header className="flex items-center justify-between gap-3 border-b border-border px-4 py-3">
              <div className="min-w-0"><p className="truncate font-bold">Thông báo</p><p className="text-xs text-muted-foreground">{unreadCount} chưa đọc</p></div>
              <Button className="shrink-0" variant="ghost" size="sm" onClick={() => void markAllNotificationsRead()}><CheckCheck className="mr-2 h-4 w-4" />Đọc tất cả</Button>
            </header>
            <div className="pretty-scrollbar max-h-[min(420px,calc(100dvh-160px))] overflow-y-auto p-2">
              {notifications.length === 0 ? <p className="p-6 text-center text-sm text-muted-foreground">Chưa có thông báo.</p> : notifications.map((item) => (
                <button key={item.id} className={`w-full rounded-xl p-3 text-left transition hover:bg-muted ${item.read ? "" : "bg-primary/8"}`} onClick={() => void markNotificationRead(item.id)}>
                  <p className="text-sm font-semibold">{item.title}</p>
                  <p className="mt-1 text-xs leading-5 text-muted-foreground">{item.message}</p>
                  <p className="mt-2 text-[11px] text-muted-foreground">{new Date(item.createdAt).toLocaleString()}</p>
                </button>
              ))}
            </div>
          </section>
        </>,
        document.body,
      ) : null}
    </div>
  );
};

export const UserProfileDialog = ({ userId, onClose }: { userId: string; onClose: () => void }) => {
  const [profile, setProfile] = useState<PublicUserProfile | null>(null);
  useEffect(() => {
    void chatApi.getUserProfile(userId).then((response) => setProfile(response.data)).catch(() => toast.error("Không thể tải hồ sơ."));
  }, [userId]);
  return <Dialog title="Hồ sơ người dùng" onClose={onClose}>
    {!profile ? <p className="text-sm text-muted-foreground">Đang tải...</p> : (
      <div className="flex items-center gap-4 rounded-2xl bg-muted/60 p-5">
        <UserAvatar name={nameOf(profile)} src={profile.avatarEndpoint ?? profile.avatar} size="xl" />
        <div><h3 className="text-xl font-bold">{nameOf(profile)}</h3><p className="text-sm text-muted-foreground">@{profile.username}</p></div>
      </div>
    )}
  </Dialog>;
};

export const BlockedUsersDialog = ({ onClose }: { onClose: () => void }) => {
  const blockedUsers = useChatStore((state) => state.blockedUsers);
  const fetchBlockedUsers = useChatStore((state) => state.fetchBlockedUsers);
  const unblockUser = useChatStore((state) => state.unblockUser);
  useEffect(() => { void fetchBlockedUsers(); }, [fetchBlockedUsers]);
  return <Dialog title="Danh sách đã chặn" onClose={onClose}>
    <div className="space-y-2">
      {blockedUsers.length === 0 ? <p className="py-8 text-center text-sm text-muted-foreground">Danh sách chặn đang trống.</p> : blockedUsers.map((user) => (
        <div key={user.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
          <UserAvatar name={nameOf(user)} src={user.avatarEndpoint ?? user.avatar} />
          <div className="min-w-0 flex-1"><p className="truncate font-semibold">{nameOf(user)}</p><p className="text-xs text-muted-foreground">@{user.username}</p></div>
          <Button variant="outline" size="sm" onClick={() => void unblockUser(user.id)}>Bỏ chặn</Button>
        </div>
      ))}
    </div>
  </Dialog>;
};

export const ConversationSearchDialog = ({ roomId, onClose }: { roomId: string; onClose: () => void }) => {
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState<ChatMessage[]>([]);
  const search = async () => {
    if (!keyword.trim()) return;
    const response = await chatApi.searchMessages(roomId, keyword.trim());
    setResults(response.data.items);
  };
  return <Dialog title="Tìm trong cuộc trò chuyện" onClose={onClose}>
    <form className="flex gap-2" onSubmit={(event) => { event.preventDefault(); void search(); }}>
      <input className="h-10 flex-1 rounded-xl border border-input bg-background px-3 text-sm" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Nhập nội dung cần tìm" />
      <Button type="submit"><Search className="mr-2 h-4 w-4" />Tìm</Button>
    </form>
    <div className="mt-5 space-y-2">
      {results.map((message) => <div key={message.id} className="rounded-xl border border-border p-3"><p className="text-sm">{message.content}</p><p className="mt-2 text-xs text-muted-foreground">{new Date(message.timestamp).toLocaleString()}</p></div>)}
    </div>
  </Dialog>;
};

export const RoomSettingsDialog = ({ room, user, onClose }: { room: ChatRoom; user: AuthUser; onClose: () => void }) => {
  const friends = useChatStore((state) => state.friends);
  const members = useChatStore((state) => state.roomMembersByRoomId[room.id] ?? EMPTY_ROOM_MEMBERS);
  const fetchRoomMembers = useChatStore((state) => state.fetchRoomMembers);
  const refreshRoom = useChatStore((state) => state.refreshRoom);
  const removeRoomLocally = useChatStore((state) => state.removeRoomLocally);
  const [name, setName] = useState(room.name ?? "");
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const isOwner = room.ownerId === user.id;
  const canManage = isOwner || room.admins.includes(user.id);
  useEffect(() => { void fetchRoomMembers(room.id); }, [fetchRoomMembers, room.id]);
  const availableFriends = useMemo(() => friends.filter(({ friend }) => !room.memberIds.includes(friend.id)), [friends, room.memberIds]);
  const applyRoom = (request: Promise<{ data: ChatRoom }>) => void request.then((response) => { refreshRoom(response.data); void fetchRoomMembers(room.id); }).catch(() => toast.error("Không thể cập nhật nhóm."));
  const uploadAvatar = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) applyRoom(chatApi.uploadRoomAvatar(room.id, file));
  };
  return <Dialog title="Quản lý nhóm" onClose={onClose}>
    <div className="space-y-6">
      <section className="flex items-center gap-4 rounded-2xl bg-muted/60 p-4">
        <UserAvatar name={room.name || "Nhóm"} src={room.avatarEndpoint ?? room.avatar} size="xl" />
        <div className="flex-1 space-y-2">
          <input className="h-10 w-full rounded-xl border border-input bg-background px-3 text-sm" value={name} onChange={(event) => setName(event.target.value)} disabled={!canManage} />
          <div className="flex gap-2">
            <Button size="sm" disabled={!canManage || !name.trim()} onClick={() => applyRoom(chatApi.updateRoomName(room.id, name.trim()))}>Lưu tên</Button>
            <label className="inline-flex cursor-pointer items-center rounded-lg border border-input px-3 text-xs font-semibold"><Camera className="mr-2 h-4 w-4" />Đổi ảnh<input type="file" accept="image/*" className="sr-only" disabled={!canManage} onChange={uploadAvatar} /></label>
          </div>
        </div>
      </section>
      <section><h3 className="mb-3 font-bold">Thành viên ({members.length})</h3><div className="space-y-2">
        {members.map((member) => <div key={member.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
          <UserAvatar name={nameOf(member)} src={member.avatarEndpoint ?? member.avatar} />
          <div className="min-w-0 flex-1"><p className="truncate font-semibold">{nameOf(member)}</p><p className="text-xs text-muted-foreground">{member.role}</p></div>
          {isOwner && member.id !== user.id ? <div className="flex gap-1">
            {member.role === "ADMIN" ? <Button variant="ghost" size="icon" title="Gỡ quản trị viên" onClick={() => applyRoom(chatApi.demoteRoomAdmin(room.id, member.id))}><Shield className="h-4 w-4" /></Button> : <Button variant="ghost" size="icon" title="Thêm quản trị viên" onClick={() => applyRoom(chatApi.promoteRoomAdmin(room.id, member.id))}><Shield className="h-4 w-4" /></Button>}
            <Button variant="ghost" size="icon" title="Chuyển chủ nhóm" onClick={() => applyRoom(chatApi.transferRoomOwner(room.id, member.id))}><Crown className="h-4 w-4" /></Button>
            <Button variant="ghost" size="icon" title="Xóa thành viên" onClick={() => applyRoom(chatApi.removeRoomMember(room.id, member.id))}><UserMinus className="h-4 w-4 text-red-500" /></Button>
          </div> : null}
        </div>)}
      </div></section>
      {canManage && availableFriends.length > 0 ? <section><h3 className="mb-3 font-bold">Thêm thành viên</h3><div className="grid gap-2 sm:grid-cols-2">{availableFriends.map(({ friend }) => <label key={friend.id} className="flex items-center gap-2 rounded-xl border border-border p-3 text-sm"><input type="checkbox" checked={selectedIds.includes(friend.id)} onChange={(event) => setSelectedIds((items) => event.target.checked ? [...items, friend.id] : items.filter((id) => id !== friend.id))} /><span className="truncate">{nameOf(friend)}</span></label>)}</div><Button className="mt-3" disabled={selectedIds.length === 0} onClick={() => applyRoom(chatApi.addRoomMembers(room.id, selectedIds))}><UserPlus className="mr-2 h-4 w-4" />Thêm đã chọn</Button></section> : null}
      <div className="flex flex-wrap gap-2 border-t border-border pt-5">
        {!isOwner ? <Button variant="outline" onClick={() => void chatApi.leaveRoom(room.id).then(() => { removeRoomLocally(room.id); onClose(); })}><LogOut className="mr-2 h-4 w-4" />Rời nhóm</Button> : null}
        {isOwner ? <Button variant="outline" className="text-red-600" onClick={() => { if (window.confirm("Giải tán nhóm này?")) void chatApi.dissolveRoom(room.id).then(() => { removeRoomLocally(room.id); onClose(); }); }}><Trash2 className="mr-2 h-4 w-4" />Giải tán nhóm</Button> : null}
      </div>
    </div>
  </Dialog>;
};

export const FriendActions = ({ friend }: { friend: FriendUser }) => {
  const unfriend = useChatStore((state) => state.unfriend);
  const blockUser = useChatStore((state) => state.blockUser);
  const [profileOpen, setProfileOpen] = useState(false);
  return <>
    <div className="ml-auto flex gap-1">
      <Button variant="ghost" size="sm" onClick={(event) => { event.stopPropagation(); setProfileOpen(true); }}>Hồ sơ</Button>
      <Button variant="ghost" size="icon" title="Hủy kết bạn" onClick={(event) => { event.stopPropagation(); void unfriend(friend.id); }}><UserMinus className="h-4 w-4" /></Button>
      <Button variant="ghost" size="icon" title="Chặn" onClick={(event) => { event.stopPropagation(); void blockUser(friend.id); }}><Shield className="h-4 w-4 text-red-500" /></Button>
    </div>
    {profileOpen ? <UserProfileDialog userId={friend.id} onClose={() => setProfileOpen(false)} /> : null}
  </>;
};

export const GroupRequestsWorkspace = ({ rooms }: { rooms: ChatRoom[] }) => {
  const refreshRoom = useChatStore((state) => state.refreshRoom);
  const [requests, setRequests] = useState<GroupJoinRequest[]>([]);
  const groupRooms = useMemo(() => rooms.filter((room) => room.type.toUpperCase() === "GROUP"), [rooms]);
  const load = async () => {
    const responses = await Promise.all(groupRooms.map((room) => chatApi.getJoinRequests(room.id).catch(() => ({ data: [] as GroupJoinRequest[] }))));
    setRequests(responses.flatMap((response) => response.data));
  };
  useEffect(() => { void load(); }, [groupRooms.length]);
  return <div className="space-y-3">
    {requests.length === 0 ? <p className="py-8 text-center text-sm text-muted-foreground">Không có yêu cầu vào nhóm đang chờ.</p> : requests.map((request) => {
      const room = groupRooms.find((item) => item.id === request.roomId);
      return <div key={request.id} className="flex items-center gap-3 rounded-xl border border-border p-4">
        <div className="min-w-0 flex-1"><p className="font-semibold">{room?.name || "Nhóm chat"}</p><p className="text-xs text-muted-foreground">Yêu cầu thêm người dùng {request.targetUserId}</p></div>
        <Button size="sm" onClick={() => void chatApi.approveJoinRequest(request.roomId, request.id).then((response) => { refreshRoom(response.data); void load(); })}>Duyệt</Button>
        <Button variant="outline" size="sm" onClick={() => void chatApi.rejectJoinRequest(request.roomId, request.id).then(() => load())}>Từ chối</Button>
      </div>;
    })}
  </div>;
};
