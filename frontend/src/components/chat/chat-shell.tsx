import {
  Bell,
  Camera,
  Check,
  Hash,
  Loader2,
  LogOut,
  MessageCircleMore,
  MoonStar,
  Plus,
  Search,
  SendHorizontal,
  SunMedium,
  UserPlus,
  UserRound,
  UsersRound,
  X,
} from "lucide-react";
import {
  type ChangeEvent,
  type FormEvent,
  type ReactNode,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { useTheme } from "@/components/theme-provider";
import { Button } from "@/components/ui/button";
import { API_URL } from "@/lib/config";
import { chatApi } from "@/lib/chat-client";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";
import { useChatStore } from "@/store/chat-store";
import type { AuthUser } from "@/types/auth";
import type { ChatMessage, ChatRoom, ChatUser, FriendRequest, FriendUser } from "@/types/chat";

type SidebarTab = "chats" | "friends" | "profile";

const apiOrigin = API_URL.replace(/\/api\/?$/, "");

const resolveMediaUrl = (value?: string | null) => {
  if (!value) {
    return null;
  }
  if (/^https?:\/\//i.test(value) || value.startsWith("data:")) {
    return value;
  }
  return `${apiOrigin}${value.startsWith("/") ? value : `/${value}`}`;
};

const formatRelativeTime = (value?: string | null) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const diffMinutes = Math.max(1, Math.floor((Date.now() - date.getTime()) / 60000));
  if (diffMinutes < 60) return `${diffMinutes}m`;
  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours}h`;
  return date.toLocaleDateString(undefined, { day: "2-digit", month: "short" });
};

const getInitials = (name: string) =>
  name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("") || "IC";

const isDirectRoom = (room: ChatRoom) => room.type?.toUpperCase() === "DIRECT";

const getFriendName = (user: FriendUser) =>
  user.displayName?.trim() || user.username || "User";

const getRoomPeer = (
  room: ChatRoom,
  currentUserId: string,
  usersById: Record<string, ChatUser>,
) => {
  const peerId = room.memberIds.find((id) => id !== currentUserId);
  return peerId ? usersById[peerId] : undefined;
};

const getRoomName = (
  room: ChatRoom,
  currentUser: AuthUser,
  usersById: Record<string, ChatUser>,
) => {
  if (!isDirectRoom(room)) return room.name?.trim() || "Group chat";
  const peer = getRoomPeer(room, currentUser.id, usersById);
  return peer?.displayName?.trim() || peer?.username || room.name || "Direct chat";
};

const getRoomAvatar = (
  room: ChatRoom,
  currentUser: AuthUser,
  usersById: Record<string, ChatUser>,
) => {
  if (!isDirectRoom(room)) return room.avatarEndpoint ?? room.avatar;
  const peer = getRoomPeer(room, currentUser.id, usersById);
  return peer?.avatarEndpoint ?? peer?.avatar ?? room.avatarEndpoint ?? room.avatar;
};

const getRoomOnline = (
  room: ChatRoom,
  currentUser: AuthUser,
  usersById: Record<string, ChatUser>,
) => {
  if (!isDirectRoom(room)) {
    return room.memberIds.some((id) => id !== currentUser.id && usersById[id]?.online);
  }
  return Boolean(getRoomPeer(room, currentUser.id, usersById)?.online);
};

export const UserAvatar = ({
  name,
  src,
  online,
  size = "md",
}: {
  name: string;
  src?: string | null;
  online?: boolean;
  size?: "sm" | "md" | "lg";
}) => (
  <div
    className={cn(
      "relative grid shrink-0 place-items-center overflow-hidden rounded-xl border border-border bg-secondary font-semibold text-secondary-foreground",
      size === "sm" && "h-9 w-9 text-xs",
      size === "md" && "h-11 w-11 text-sm",
      size === "lg" && "h-12 w-12 text-base",
    )}
  >
    {src ? (
      <img src={resolveMediaUrl(src) ?? undefined} alt={name} className="h-full w-full object-cover" />
    ) : (
      <span>{getInitials(name)}</span>
    )}
    {online !== undefined ? <StatusBadge online={online} compact /> : null}
  </div>
);

export const StatusBadge = ({ online, compact }: { online: boolean; compact?: boolean }) => (
  <span
    className={cn(
      "inline-flex items-center gap-1.5 text-xs font-medium",
      compact ? "absolute bottom-0 right-0 h-3 w-3 rounded-full border-2 border-card p-0" : "text-muted-foreground",
    )}
  >
    <span className={cn("h-2.5 w-2.5 rounded-full", online ? "bg-emerald-500" : "bg-muted-foreground/50", compact && "h-full w-full")} />
    {compact ? null : online ? "Online" : "Offline"}
  </span>
);

const UnreadBadge = ({ count }: { count: number }) =>
  count > 0 ? (
    <span className="grid min-w-6 place-items-center rounded-full bg-primary px-2 py-1 text-xs font-bold text-primary-foreground">
      {count > 99 ? "99+" : count}
    </span>
  ) : null;

const SkeletonLine = ({ className }: { className?: string }) => (
  <div className={cn("animate-pulse rounded-md bg-muted", className)} />
);

const ListSkeleton = () => (
  <div className="space-y-2">
    {Array.from({ length: 6 }).map((_, index) => (
      <div key={index} className="grid grid-cols-[auto_1fr] gap-3 rounded-lg border border-border p-3">
        <SkeletonLine className="h-11 w-11" />
        <div className="space-y-2">
          <SkeletonLine className="h-4 w-2/3" />
          <SkeletonLine className="h-3 w-full" />
        </div>
      </div>
    ))}
  </div>
);

const Modal = ({
  title,
  children,
  onClose,
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
}) => (
  <div className="fixed inset-0 z-50 grid place-items-center bg-background/70 p-4 backdrop-blur-sm">
    <section className="max-h-[88vh] w-full max-w-lg overflow-hidden rounded-lg border border-border bg-card shadow-soft">
      <header className="flex items-center justify-between border-b border-border px-4 py-3">
        <h2 className="text-base font-bold">{title}</h2>
        <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg" onClick={onClose} aria-label="Close" title="Close">
          <X className="h-4 w-4" />
        </Button>
      </header>
      <div className="pretty-scrollbar max-h-[calc(88vh-58px)] overflow-y-auto p-4">{children}</div>
    </section>
  </div>
);

const ChatCard = ({
  room,
  active,
  currentUser,
  usersById,
  onSelect,
}: {
  room: ChatRoom;
  active: boolean;
  currentUser: AuthUser;
  usersById: Record<string, ChatUser>;
  onSelect: () => void;
}) => {
  const name = getRoomName(room, currentUser, usersById);
  const online = getRoomOnline(room, currentUser, usersById);
  const group = !isDirectRoom(room);

  return (
    <button
      className={cn(
        "grid w-full grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 rounded-xl border px-3 py-2.5 text-left transition",
        active ? "border-primary/20 bg-accent text-accent-foreground" : "border-transparent hover:bg-muted/80",
      )}
      onClick={onSelect}
    >
      <UserAvatar name={name} src={getRoomAvatar(room, currentUser, usersById)} online={online} />
      <span className="min-w-0">
        <span className="flex min-w-0 items-center gap-2">
          {group ? <UsersRound className="h-3.5 w-3.5 shrink-0 text-muted-foreground" /> : <Hash className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />}
          <span className="truncate text-sm font-semibold">{name}</span>
        </span>
        <span className="mt-1 block truncate text-xs text-muted-foreground">
          {room.lastMessagePreview || "Chưa có tin nhắn"}
        </span>
      </span>
      <span className="flex flex-col items-end gap-2">
        <span className="text-xs text-muted-foreground">{formatRelativeTime(room.lastMessageAt)}</span>
        <UnreadBadge count={room.unreadCount} />
      </span>
    </button>
  );
};

const ChatListSection = ({ title, count, children }: { title: string; count: number; children: ReactNode }) => (
  <section className="space-y-2">
    <div className="flex items-center justify-between px-1">
      <h2 className="text-xs font-bold uppercase tracking-[0.16em] text-muted-foreground">{title}</h2>
      <span className="text-xs text-muted-foreground">{count}</span>
    </div>
    <div className="space-y-1.5">{children}</div>
  </section>
);

export const NavUser = ({ user, onProfile }: { user: AuthUser; onProfile: () => void }) => {
  const signout = useAuthStore((state) => state.signout);

  return (
    <div className="flex items-center gap-3 rounded-xl bg-muted/70 p-2.5">
      <button className="contents" onClick={onProfile} aria-label="Open profile">
        <UserAvatar name={user.fullName} src={user.avatarUrl} online={user.isOnline} />
      </button>
      <button className="min-w-0 flex-1 text-left" onClick={onProfile}>
        <p className="truncate text-sm font-semibold">{user.fullName}</p>
        <p className="truncate text-xs text-muted-foreground">@{user.username}</p>
      </button>
      <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg" onClick={() => void signout()} aria-label="Sign out" title="Sign out">
        <LogOut className="h-4 w-4" />
      </Button>
    </div>
  );
};

export const AppSidebar = ({ user }: { user: AuthUser }) => {
  const { theme, toggleTheme } = useTheme();
  const signout = useAuthStore((state) => state.signout);
  const rooms = useChatStore((state) => state.rooms);
  const usersById = useChatStore((state) => state.usersById);
  const selectedRoomId = useChatStore((state) => state.selectedRoomId);
  const setSelectedRoomId = useChatStore((state) => state.setSelectedRoomId);
  const isLoadingRooms = useChatStore((state) => state.isLoadingRooms);
  const friends = useChatStore((state) => state.friends);
  const incomingRequests = useChatStore((state) => state.incomingFriendRequests);
  const isLoadingFriends = useChatStore((state) => state.isLoadingFriends);
  const [query, setQuery] = useState("");
  const [tab, setTab] = useState<SidebarTab>("chats");
  const [modal, setModal] = useState<"friend" | "requests" | "group" | null>(null);

  const filteredRooms = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) return rooms;
    return rooms.filter((room) => getRoomName(room, user, usersById).toLowerCase().includes(normalizedQuery));
  }, [query, rooms, user, usersById]);

  const directRooms = filteredRooms.filter(isDirectRoom);
  const groupRooms = filteredRooms.filter((room) => !isDirectRoom(room));
  const tabTitle = tab === "chats" ? "Tin nhắn" : tab === "friends" ? "Bạn bè" : "Hồ sơ";

  return (
    <aside className="grid h-full min-h-0 grid-cols-[68px_minmax(0,1fr)] border-r border-border bg-card">
      <nav className="flex min-h-0 flex-col items-center bg-primary py-4 text-primary-foreground">
        <button
          className="mb-5 rounded-full ring-2 ring-white/80 ring-offset-2 ring-offset-primary"
          onClick={() => setTab("profile")}
          aria-label="Mở hồ sơ"
          title="Hồ sơ"
        >
          <UserAvatar name={user.fullName} src={user.avatarUrl} size="sm" />
        </button>

        <div className="flex w-full flex-col items-center gap-1 px-2">
          {(["chats", "friends", "profile"] as SidebarTab[]).map((item) => {
            const Icon = item === "chats" ? MessageCircleMore : item === "friends" ? UsersRound : UserRound;
            const label = item === "chats" ? "Tin nhắn" : item === "friends" ? "Bạn bè" : "Hồ sơ";
            return (
              <button
                key={item}
                className={cn(
                  "relative grid h-12 w-12 place-items-center rounded-xl transition hover:bg-white/15",
                  tab === item && "bg-[#0759c7] shadow-inner",
                )}
                onClick={() => setTab(item)}
                aria-label={label}
                title={label}
              >
                <Icon className="h-5 w-5" strokeWidth={2} />
                {item === "friends" && incomingRequests.length > 0 ? (
                  <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-red-500 ring-2 ring-primary" />
                ) : null}
              </button>
            );
          })}
        </div>

        <div className="mt-auto flex flex-col gap-1 px-2">
          <button className="grid h-12 w-12 place-items-center rounded-xl transition hover:bg-white/15" onClick={toggleTheme} aria-label="Đổi giao diện" title="Đổi giao diện">
            {theme === "dark" ? <SunMedium className="h-5 w-5" /> : <MoonStar className="h-5 w-5" />}
          </button>
          <button className="grid h-12 w-12 place-items-center rounded-xl transition hover:bg-white/15" onClick={() => void signout()} aria-label="Đăng xuất" title="Đăng xuất">
            <LogOut className="h-5 w-5" />
          </button>
        </div>
      </nav>

      <div className="flex min-h-0 flex-col bg-card">
        <header className="border-b border-border px-4 pb-4 pt-5">
          <div className="flex items-center justify-between gap-2">
            <div>
              <p className="text-xs font-medium text-muted-foreground">InChat</p>
              <h1 className="text-lg font-bold tracking-tight">{tabTitle}</h1>
            </div>
            {tab === "friends" ? (
              <Button variant="ghost" size="icon" className="h-9 w-9" onClick={() => setModal("friend")} aria-label="Thêm bạn" title="Thêm bạn">
                <UserPlus className="h-4 w-4" />
              </Button>
            ) : tab === "chats" ? (
              <Button variant="ghost" size="icon" className="h-9 w-9" onClick={() => setModal("group")} aria-label="Tạo nhóm" title="Tạo nhóm">
                <Plus className="h-4 w-4" />
              </Button>
            ) : null}
          </div>

          {tab === "chats" ? (
            <label className="relative mt-4 block">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                className="h-10 w-full rounded-lg border-0 bg-muted pl-9 pr-3 text-sm outline-none transition focus:ring-2 focus:ring-ring/20"
                placeholder="Tìm kiếm"
              />
            </label>
          ) : null}
        </header>

        <div className="pretty-scrollbar min-h-0 flex-1 space-y-5 overflow-y-auto px-3 py-4">
          {tab === "chats" && (
            isLoadingRooms ? <ListSkeleton /> : (
              <>
                <ChatListSection title="Trò chuyện" count={directRooms.length}>
                  {directRooms.map((room) => (
                    <ChatCard key={room.id} room={room} active={room.id === selectedRoomId} currentUser={user} usersById={usersById} onSelect={() => setSelectedRoomId(room.id)} />
                  ))}
                </ChatListSection>
                {groupRooms.length > 0 ? (
                  <ChatListSection title="Nhóm" count={groupRooms.length}>
                    {groupRooms.map((room) => (
                      <ChatCard key={room.id} room={room} active={room.id === selectedRoomId} currentUser={user} usersById={usersById} onSelect={() => setSelectedRoomId(room.id)} />
                    ))}
                  </ChatListSection>
                ) : null}
              </>
            )
          )}
          {tab === "friends" && (
            <FriendsPanel
              loading={isLoadingFriends}
              friends={friends}
              incomingCount={incomingRequests.length}
              onAddFriend={() => setModal("friend")}
              onRequests={() => setModal("requests")}
            />
          )}
          {tab === "profile" && <ProfilePanel />}
        </div>
      </div>

      {modal === "friend" ? <AddFriendModal onClose={() => setModal(null)} /> : null}
      {modal === "requests" ? <FriendRequestsModal onClose={() => setModal(null)} /> : null}
      {modal === "group" ? <CreateGroupModal onClose={() => setModal(null)} /> : null}
    </aside>
  );
};

const FriendsPanel = ({
  loading,
  friends,
  incomingCount,
  onAddFriend,
  onRequests,
}: {
  loading: boolean;
  friends: { id: string; friend: FriendUser }[];
  incomingCount: number;
  onAddFriend: () => void;
  onRequests: () => void;
}) => {
  const createDirectRoom = useChatStore((state) => state.createDirectRoom);
  if (loading) return <ListSkeleton />;
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-2">
        <Button variant="outline" className="rounded-lg" onClick={onAddFriend}><UserPlus className="h-4 w-4" /> Thêm bạn</Button>
        <Button variant="outline" className="rounded-lg" onClick={onRequests}><Bell className="h-4 w-4" /> Lời mời {incomingCount ? `(${incomingCount})` : ""}</Button>
      </div>
      <ChatListSection title="Bạn bè" count={friends.length}>
        {friends.length === 0 ? <EmptyText text="Bạn chưa có người bạn nào." /> : friends.map(({ id, friend }) => (
          <button key={id} className="flex w-full items-center gap-3 rounded-lg border border-border p-3 text-left transition hover:bg-muted" onClick={() => void createDirectRoom(friend.id)}>
            <UserAvatar name={getFriendName(friend)} src={friend.avatarEndpoint ?? friend.avatar} />
            <span className="min-w-0">
              <span className="block truncate text-sm font-semibold">{getFriendName(friend)}</span>
              <span className="block truncate text-xs text-muted-foreground">@{friend.username}</span>
            </span>
          </button>
        ))}
      </ChatListSection>
    </div>
  );
};

const ProfilePanel = () => {
  const profile = useChatStore((state) => state.profile);
  const isLoadingProfile = useChatStore((state) => state.isLoadingProfile);
  const isMutating = useChatStore((state) => state.isMutating);
  const updateProfile = useChatStore((state) => state.updateProfile);
  const uploadAvatar = useChatStore((state) => state.uploadAvatar);
  const [form, setForm] = useState({ username: "", displayName: "", bio: "", phone: "", themePreference: "system" });

  useEffect(() => {
    if (profile) {
      setForm({
        username: profile.username,
        displayName: profile.displayName ?? "",
        bio: profile.bio ?? "",
        phone: profile.phone ?? "",
        themePreference: profile.themePreference ?? "system",
      });
    }
  }, [profile]);

  if (isLoadingProfile || !profile) return <ListSkeleton />;

  const submit = (event: FormEvent) => {
    event.preventDefault();
    void updateProfile(form);
  };

  return (
    <form className="space-y-4" onSubmit={submit}>
      <div className="flex items-center gap-3">
        <UserAvatar name={form.displayName || form.username} src={profile.avatarEndpoint ?? profile.avatar} size="lg" online={profile.online} />
        <label className="inline-flex h-9 cursor-pointer items-center justify-center rounded-lg border border-border bg-background px-3 text-xs font-semibold hover:bg-muted">
          <Camera className="mr-2 h-4 w-4" /> Avatar
          <input type="file" accept="image/*" className="sr-only" onChange={(event) => handleAvatarChange(event, uploadAvatar)} />
        </label>
      </div>
      <TextInput label="Username" value={form.username} onChange={(value) => setForm((current) => ({ ...current, username: value }))} />
      <TextInput label="Display name" value={form.displayName} onChange={(value) => setForm((current) => ({ ...current, displayName: value }))} />
      <TextInput label="Phone" value={form.phone} onChange={(value) => setForm((current) => ({ ...current, phone: value }))} />
      <label className="block space-y-1.5">
        <span className="text-xs font-semibold text-muted-foreground">Giới thiệu</span>
        <textarea value={form.bio} onChange={(event) => setForm((current) => ({ ...current, bio: event.target.value }))} rows={4} className="w-full resize-none rounded-lg border border-input bg-background px-3 py-2 text-sm outline-none focus:border-ring focus:ring-2 focus:ring-ring/20" />
      </label>
      <select value={form.themePreference} onChange={(event) => setForm((current) => ({ ...current, themePreference: event.target.value }))} className="h-10 w-full rounded-lg border border-input bg-background px-3 text-sm outline-none">
        <option value="system">Theo hệ thống</option>
        <option value="light">Sáng</option>
        <option value="dark">Tối</option>
      </select>
      <Button type="submit" className="w-full rounded-lg" disabled={isMutating}>{isMutating ? "Đang lưu..." : "Lưu hồ sơ"}</Button>
    </form>
  );
};

const handleAvatarChange = (event: ChangeEvent<HTMLInputElement>, uploadAvatar: (file: File) => Promise<void>) => {
  const file = event.target.files?.[0];
  if (file) void uploadAvatar(file);
  event.target.value = "";
};

const TextInput = ({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) => (
  <label className="block space-y-1.5">
    <span className="text-xs font-semibold text-muted-foreground">{label}</span>
    <input value={value} onChange={(event) => onChange(event.target.value)} className="h-10 w-full rounded-lg border border-input bg-background px-3 text-sm outline-none focus:border-ring focus:ring-2 focus:ring-ring/20" />
  </label>
);

const AddFriendModal = ({ onClose }: { onClose: () => void }) => {
  const currentUser = useAuthStore((state) => state.user);
  const sendFriendRequest = useChatStore((state) => state.sendFriendRequest);
  const isMutating = useChatStore((state) => state.isMutating);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<ChatUser[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setLoading(true);
      void chatApi.searchUsers(query).then((response) => {
        setResults(response.data.filter((user) => user.id !== currentUser?.id));
      }).finally(() => setLoading(false));
    }, 250);
    return () => window.clearTimeout(timer);
  }, [currentUser?.id, query]);

  return (
    <Modal title="Thêm bạn" onClose={onClose}>
      <div className="space-y-3">
        <TextInput label="Tìm người dùng" value={query} onChange={setQuery} />
        {loading ? <ListSkeleton /> : results.map((user) => (
          <div key={user.id} className="flex items-center gap-3 rounded-lg border border-border p-3">
            <UserAvatar name={user.displayName || user.username} src={user.avatarEndpoint ?? user.avatar} />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold">{user.displayName || user.username}</p>
              <p className="truncate text-xs text-muted-foreground">@{user.username}</p>
            </div>
            <Button size="sm" className="rounded-lg" disabled={isMutating} onClick={() => void sendFriendRequest(user.id)}>Gửi lời mời</Button>
          </div>
        ))}
      </div>
    </Modal>
  );
};

const FriendRequestsModal = ({ onClose }: { onClose: () => void }) => {
  const incoming = useChatStore((state) => state.incomingFriendRequests);
  const outgoing = useChatStore((state) => state.outgoingFriendRequests);
  const acceptFriendRequest = useChatStore((state) => state.acceptFriendRequest);
  const rejectFriendRequest = useChatStore((state) => state.rejectFriendRequest);
  const isMutating = useChatStore((state) => state.isMutating);
  const [firstMessages, setFirstMessages] = useState<Record<string, string>>({});

  return (
    <Modal title="Lời mời kết bạn" onClose={onClose}>
      <div className="space-y-5">
        <ChatListSection title="Đã nhận" count={incoming.length}>
          {incoming.length === 0 ? <EmptyText text="Không có lời mời mới." /> : incoming.map((request) => (
            <RequestCard
              key={request.id}
              request={request}
              value={firstMessages[request.id] ?? ""}
              onChange={(value) => setFirstMessages((current) => ({ ...current, [request.id]: value }))}
              onAccept={() => void acceptFriendRequest(request.id, firstMessages[request.id])}
              onReject={() => void rejectFriendRequest(request.id)}
              disabled={isMutating}
            />
          ))}
        </ChatListSection>
        <ChatListSection title="Đã gửi" count={outgoing.length}>
          {outgoing.length === 0 ? <EmptyText text="Bạn chưa gửi lời mời nào." /> : outgoing.map((request) => (
            <SimpleUserRow key={request.id} user={request.receiver} suffix="Đang chờ" />
          ))}
        </ChatListSection>
      </div>
    </Modal>
  );
};

const RequestCard = ({
  request,
  value,
  onChange,
  onAccept,
  onReject,
  disabled,
}: {
  request: FriendRequest;
  value: string;
  onChange: (value: string) => void;
  onAccept: () => void;
  onReject: () => void;
  disabled: boolean;
}) => (
  <div className="space-y-3 rounded-lg border border-border p-3">
    <SimpleUserRow user={request.requester} suffix={formatRelativeTime(request.createdAt)} />
    <input value={value} onChange={(event) => onChange(event.target.value)} className="h-10 w-full rounded-lg border border-input bg-background px-3 text-sm outline-none" placeholder="Tin nhắn đầu tiên sau khi đồng ý" />
    <div className="flex justify-end gap-2">
      <Button variant="outline" size="sm" className="rounded-lg" disabled={disabled} onClick={onReject}><X className="h-4 w-4" /> Từ chối</Button>
      <Button size="sm" className="rounded-lg" disabled={disabled} onClick={onAccept}><Check className="h-4 w-4" /> Đồng ý</Button>
    </div>
  </div>
);

const CreateGroupModal = ({ onClose }: { onClose: () => void }) => {
  const friends = useChatStore((state) => state.friends);
  const createGroupRoom = useChatStore((state) => state.createGroupRoom);
  const isMutating = useChatStore((state) => state.isMutating);
  const [name, setName] = useState("");
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const room = await createGroupRoom({ name, memberIds: selectedIds });
    if (room) onClose();
  };

  return (
    <Modal title="Tạo nhóm mới" onClose={onClose}>
      <form className="space-y-4" onSubmit={submit}>
        <TextInput label="Tên nhóm" value={name} onChange={setName} />
        <ChatListSection title="Thành viên" count={selectedIds.length}>
          {friends.map(({ friend }) => (
            <label key={friend.id} className="flex cursor-pointer items-center gap-3 rounded-lg border border-border p-3 hover:bg-muted">
              <input
                type="checkbox"
                checked={selectedIds.includes(friend.id)}
                onChange={(event) => setSelectedIds((current) => event.target.checked ? [...current, friend.id] : current.filter((id) => id !== friend.id))}
              />
              <UserAvatar name={getFriendName(friend)} src={friend.avatarEndpoint ?? friend.avatar} />
              <span className="min-w-0">
                <span className="block truncate text-sm font-semibold">{getFriendName(friend)}</span>
                <span className="block truncate text-xs text-muted-foreground">@{friend.username}</span>
              </span>
            </label>
          ))}
        </ChatListSection>
        <Button type="submit" className="w-full rounded-lg" disabled={isMutating || !name.trim() || selectedIds.length < 2}>Tạo nhóm</Button>
      </form>
    </Modal>
  );
};

const SimpleUserRow = ({ user, suffix }: { user: FriendUser; suffix?: string }) => (
  <div className="flex items-center gap-3">
    <UserAvatar name={getFriendName(user)} src={user.avatarEndpoint ?? user.avatar} />
    <div className="min-w-0 flex-1">
      <p className="truncate text-sm font-semibold">{getFriendName(user)}</p>
      <p className="truncate text-xs text-muted-foreground">@{user.username}</p>
    </div>
    {suffix ? <span className="text-xs text-muted-foreground">{suffix}</span> : null}
  </div>
);

const EmptyText = ({ text }: { text: string }) => (
  <p className="rounded-lg border border-border bg-background p-4 text-center text-sm text-muted-foreground">{text}</p>
);

export const ChatWindowLayout = ({ user }: { user: AuthUser }) => {
  const rooms = useChatStore((state) => state.rooms);
  const usersById = useChatStore((state) => state.usersById);
  const selectedRoomId = useChatStore((state) => state.selectedRoomId);
  const messagesByRoomId = useChatStore((state) => state.messagesByRoomId);
  const messagePagesByRoomId = useChatStore((state) => state.messagePagesByRoomId);
  const isLoadingMessages = useChatStore((state) => state.isLoadingMessages);
  const error = useChatStore((state) => state.error);

  const selectedRoom = rooms.find((room) => room.id === selectedRoomId) ?? null;
  const messages = selectedRoomId ? messagesByRoomId[selectedRoomId] ?? [] : [];
  const pageState = selectedRoomId ? messagePagesByRoomId[selectedRoomId] : undefined;

  if (!selectedRoom || !selectedRoomId) return <WelcomeScreen />;

  return (
    <section className="flex h-full min-h-0 flex-col bg-background">
      <ChatWindowHeader room={selectedRoom} user={user} usersById={usersById} />
      {error ? (
        <div className="border-b border-red-500/20 bg-red-500/10 px-4 py-2 text-center text-xs text-red-600 dark:text-red-300">
          {error}
        </div>
      ) : null}
      <ChatWindowBody roomId={selectedRoomId} messages={messages} currentUser={user} usersById={usersById} loading={isLoadingMessages} pageState={pageState} />
      <MessageInput />
    </section>
  );
};

const WelcomeScreen = () => (
  <section className="grid h-full place-items-center bg-background p-6">
    <div className="max-w-md text-center">
      <div className="mx-auto grid h-14 w-14 place-items-center rounded-lg bg-accent text-accent-foreground">
        <MessageCircleMore className="h-7 w-7" />
      </div>
      <h1 className="mt-5 text-2xl font-bold tracking-tight">Chọn một cuộc trò chuyện</h1>
      <p className="mt-2 text-sm leading-6 text-muted-foreground">Tin nhắn và trạng thái trực tuyến sẽ được cập nhật theo thời gian thực.</p>
    </div>
  </section>
);

export const ChatWindowHeader = ({ room, user, usersById }: { room: ChatRoom; user: AuthUser; usersById: Record<string, ChatUser> }) => {
  const name = getRoomName(room, user, usersById);
  const online = getRoomOnline(room, user, usersById);
  const peer = getRoomPeer(room, user.id, usersById);

  return (
    <header className="flex items-center gap-3 border-b border-border bg-card/80 px-4 py-3 backdrop-blur">
      <UserAvatar name={name} src={getRoomAvatar(room, user, usersById)} online={online} size="lg" />
      <div className="min-w-0 flex-1">
        <h1 className="truncate text-base font-bold">{name}</h1>
        <div className="mt-1 flex items-center gap-2">
          <StatusBadge online={online} />
          {!online && peer?.lastSeenAt ? <span className="text-xs text-muted-foreground">Last seen {formatRelativeTime(peer.lastSeenAt)}</span> : null}
        </div>
      </div>
      {!isDirectRoom(room) ? <span className="text-xs text-muted-foreground">{room.memberIds.length} members</span> : null}
    </header>
  );
};

export const ChatWindowBody = ({
  roomId,
  messages,
  currentUser,
  usersById,
  loading,
  pageState,
}: {
  roomId: string;
  messages: ChatMessage[];
  currentUser: AuthUser;
  usersById: Record<string, ChatUser>;
  loading: boolean;
  pageState?: { hasMore: boolean; loadingOlder: boolean };
}) => {
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const previousRoomIdRef = useRef<string | null>(null);
  const previousLastMessageIdRef = useRef<string | null>(null);
  const fetchOlderMessages = useChatStore((state) => state.fetchOlderMessages);
  const markVisibleMessages = useChatStore((state) => state.markVisibleMessages);
  const lastMessageId = messages.at(-1)?.id ?? null;

  useEffect(() => {
    const roomChanged = previousRoomIdRef.current !== roomId;
    const newMessageAtBottom =
      previousLastMessageIdRef.current !== null &&
      previousLastMessageIdRef.current !== lastMessageId;

    if (roomChanged || newMessageAtBottom) {
      bottomRef.current?.scrollIntoView({ behavior: roomChanged ? "auto" : "smooth" });
    }

    previousRoomIdRef.current = roomId;
    previousLastMessageIdRef.current = lastMessageId;
    markVisibleMessages(roomId);
  }, [lastMessageId, markVisibleMessages, roomId]);

  const handleScroll = () => {
    const node = scrollRef.current;
    if (!node || node.scrollTop > 48 || !pageState?.hasMore || pageState.loadingOlder) return;
    const previousHeight = node.scrollHeight;
    void fetchOlderMessages(roomId).then(() => {
      window.requestAnimationFrame(() => {
        if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight - previousHeight;
      });
    });
  };

  return (
    <div ref={scrollRef} onScroll={handleScroll} className="pretty-scrollbar min-h-0 flex-1 overflow-y-auto px-4 py-5">
      {pageState?.loadingOlder ? <p className="mb-3 flex items-center justify-center gap-2 text-xs text-muted-foreground"><Loader2 className="h-3.5 w-3.5 animate-spin" /> Loading older messages</p> : null}
      {loading ? (
        <MessageSkeleton />
      ) : messages.length === 0 ? (
        <EmptyText text="Chưa có tin nhắn nào. Hãy gửi lời chào đầu tiên." />
      ) : (
        <div className="space-y-3">
          {messages.map((message) => (
            <MessageItem key={message.id} message={message} mine={message.senderId === currentUser.id} sender={usersById[message.senderId]} currentUserId={currentUser.id} />
          ))}
        </div>
      )}
      <div ref={bottomRef} />
    </div>
  );
};

const MessageSkeleton = () => (
  <div className="space-y-3">
    <SkeletonLine className="h-12 w-2/3" />
    <SkeletonLine className="ml-auto h-12 w-1/2" />
    <SkeletonLine className="h-12 w-3/5" />
  </div>
);

export const MessageItem = ({ message, mine, sender, currentUserId }: { message: ChatMessage; mine: boolean; sender?: ChatUser; currentUserId: string }) => {
  const senderName = sender?.displayName || sender?.username || "User";
  const receipt = getReceiptLabel(message, currentUserId);

  return (
    <div className={cn("mx-auto flex max-w-4xl gap-2", mine && "justify-end")}>
      {!mine ? <UserAvatar name={senderName} src={sender?.avatarEndpoint ?? sender?.avatar} size="sm" /> : null}
      <div className={cn("max-w-[78%]", mine && "text-right")}>
        {!mine ? <p className="mb-1 px-1 text-xs font-medium text-muted-foreground">{senderName}</p> : null}
        <div className={cn("rounded-2xl px-4 py-2.5 text-sm leading-6 shadow-sm", mine ? "rounded-br-md bg-primary text-primary-foreground" : "rounded-bl-md border border-border bg-card text-card-foreground")}>
          {message.content}
        </div>
        <p className="mt-1 px-1 text-xs text-muted-foreground">
          {new Date(message.timestamp).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" })}
          {mine ? ` - ${receipt}` : ""}
        </p>
      </div>
    </div>
  );
};

const getReceiptLabel = (message: ChatMessage, currentUserId: string) => {
  const readByOthers = message.readByUserIds.filter((id) => id !== currentUserId);
  if (readByOthers.length > 0 || message.status?.toLowerCase() === "seen") return "Seen";
  const deliveredToOthers = message.deliveredToUserIds.filter((id) => id !== currentUserId);
  if (deliveredToOthers.length > 0 || message.status?.toLowerCase() === "delivered") return "Delivered";
  return "Sent";
};

export const MessageInput = () => {
  const sendMessage = useChatStore((state) => state.sendMessage);
  const isSending = useChatStore((state) => state.isSending);
  const [value, setValue] = useState("");

  const sendCurrentValue = () => {
    const content = value.trim();
    if (!content) return;
    setValue("");
    void sendMessage(content);
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    sendCurrentValue();
  };

  return (
    <form onSubmit={submit} className="border-t border-border bg-card/90 p-3 backdrop-blur">
      <div className="mx-auto flex max-w-4xl items-end gap-2 rounded-2xl border border-input bg-background p-2 shadow-sm focus-within:border-primary/60 focus-within:ring-2 focus-within:ring-primary/10">
        <textarea
          value={value}
          onChange={(event) => setValue(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter" && !event.shiftKey) {
              event.preventDefault();
              sendCurrentValue();
            }
          }}
          rows={1}
          className="max-h-32 min-h-10 flex-1 resize-none bg-transparent px-1 py-2 text-sm leading-6 outline-none"
          placeholder="Nhập tin nhắn..."
        />
        <Button type="submit" size="icon" className="h-10 w-10 rounded-lg" disabled={isSending || !value.trim()} aria-label="Send message" title="Send">
          <SendHorizontal className="h-5 w-5" />
        </Button>
      </div>
    </form>
  );
};
