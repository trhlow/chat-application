import {
  Camera,
  Check,
  ChevronRight,
  ChevronsUpDown,
  CircleHelp,
  Database,
  ExternalLink,
  Globe2,
  Heart,
  Loader2,
  LogOut,
  MessageCircleMore,
  MoonStar,
  Paperclip,
  Phone,
  Plus,
  Search,
  SendHorizontal,
  Settings,
  Smile,
  SlidersHorizontal,
  SunMedium,
  UserPlus,
  UserMinus,
  UserRound,
  UsersRound,
  Video,
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
import { toast } from "sonner";

import { EmojiPicker } from "@/components/chat/EmojiPicker";
import {
  BlockedUsersDialog,
  ConversationSearchDialog,
  FriendActions,
  GroupRequestsWorkspace,
  NotificationCenter,
  RoomSettingsDialog,
} from "@/components/chat/FeatureDialogs";
import { useTheme } from "@/components/theme-provider";
import { Button } from "@/components/ui/button";
import { API_URL } from "@/lib/config";
import { chatApi } from "@/services/chatService";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/stores/useAuthStore";
import { useChatStore } from "@/stores/useChatStore";
import type { AuthUser } from "@/types/auth";
import type { ChatMessage, ChatRoom, ChatUser, FriendRequest, FriendUser, TypingEvent } from "@/types/chat";

type SidebarTab = "chats" | "friends" | "profile";
type AppView = "chats" | "friends";
type FriendsSection = "list" | "groups" | "requests" | "groupRequests";

const EMPTY_TYPING_USERS: TypingEvent[] = [];

const apiOrigin = API_URL.replace(/\/api\/?$/, "");

const resolveMediaUrl = (value?: string | null) => {
  if (!value) {
    return null;
  }
  if (/^https?:\/\//i.test(value) || value.startsWith("data:image/")) {
    return value;
  }
  return `${apiOrigin}${value.startsWith("/") ? value : `/${value}`}`;
};

const formatFileSize = (size?: number | null) => {
  if (!size || size < 1) return null;
  if (size < 1024 * 1024) return `${Math.ceil(size / 1024)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
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
  size?: "sm" | "md" | "lg" | "xl";
}) => (
  <div
    className={cn(
      "relative grid shrink-0 place-items-center overflow-hidden rounded-full border-2 border-white bg-secondary font-semibold text-secondary-foreground shadow-sm dark:border-card",
      size === "sm" && "h-9 w-9 text-xs",
      size === "md" && "h-11 w-11 text-sm",
      size === "lg" && "h-12 w-12 text-base",
      size === "xl" && "h-24 w-24 text-xl",
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

export const UnreadCountBadge = ({ count }: { count: number }) =>
  count > 0 ? (
    <span className="grid min-w-5 place-items-center rounded-full bg-primary px-1.5 py-0.5 text-[10px] font-bold text-primary-foreground shadow-sm">
      {count > 99 ? "99+" : count}
    </span>
  ) : null;

const SkeletonLine = ({ className }: { className?: string }) => (
  <div className={cn("animate-pulse rounded-md bg-muted", className)} />
);

export const ConversationSkeleton = () => (
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
  size = "default",
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
  size?: "compact" | "default" | "wide";
}) => (
  <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/55 p-4 backdrop-blur-[2px]">
    <section className={cn(
      "max-h-[92vh] w-full overflow-hidden rounded-2xl border border-white/70 bg-card shadow-[0_24px_70px_-24px_rgb(15_23_42/0.5)]",
      size === "compact" && "max-w-[420px]",
      size === "default" && "max-w-lg",
      size === "wide" && "max-w-[680px]",
    )}>
      <header className="flex items-center justify-between px-5 pb-2 pt-4">
        <h2 className="text-xl font-extrabold tracking-tight">{title}</h2>
        <Button variant="ghost" size="icon" className="h-8 w-8 rounded-lg text-muted-foreground" onClick={onClose} aria-label="Đóng" title="Đóng">
          <X className="h-4 w-4" />
        </Button>
      </header>
      <div className="pretty-scrollbar max-h-[calc(92vh-54px)] overflow-y-auto px-5 pb-5 pt-2">{children}</div>
    </section>
  </div>
);

export const ChatCard = ({
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
        "conversation-card grid min-h-[70px] w-full grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 rounded-xl border px-3 py-2.5 text-left transition duration-200",
        active
          ? "border-primary/20 bg-primary/[0.08] text-foreground shadow-[inset_3px_0_0_hsl(var(--primary))]"
          : "border-transparent bg-transparent hover:bg-muted/70",
      )}
      onClick={onSelect}
    >
      {group ? (
        <div className="flex w-[54px] shrink-0 -space-x-3">
          {room.memberIds.slice(0, 3).map((memberId) => {
            const member = usersById[memberId];
            const memberName = member?.displayName || member?.username || name;
            return (
              <UserAvatar
                key={memberId}
                name={memberName}
                src={member?.avatarEndpoint ?? member?.avatar}
                size="sm"
              />
            );
          })}
        </div>
      ) : (
        <UserAvatar name={name} src={getRoomAvatar(room, currentUser, usersById)} online={online} />
      )}
      <span className="min-w-0">
        <span className="flex min-w-0 items-center gap-2">
          <span className="truncate text-sm font-semibold">{name}</span>
        </span>
        <span className="mt-1 block truncate text-sm text-muted-foreground">
          {group ? `${room.memberIds.length} thành viên` : room.lastMessagePreview || "Chưa có tin nhắn"}
        </span>
      </span>
      <span className="flex flex-col items-end gap-2">
        <span className="text-xs text-muted-foreground">{formatRelativeTime(room.lastMessageAt)}</span>
        <UnreadCountBadge count={room.unreadCount} />
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
    <div className="flex items-center gap-3 bg-card px-1 py-2">
      <button className="contents" onClick={onProfile} aria-label="Open profile">
        <UserAvatar name={user.fullName} src={user.avatarUrl} online={user.isOnline} />
      </button>
      <button className="min-w-0 flex-1 text-left" onClick={onProfile}>
        <p className="truncate text-sm font-semibold">{user.fullName}</p>
        <p className="truncate text-xs text-muted-foreground">@{user.username}</p>
      </button>
      <Button variant="ghost" size="icon" className="h-9 w-9 rounded-lg" onClick={() => void signout()} aria-label="Đăng xuất" title="Đăng xuất">
        <ChevronsUpDown className="h-4 w-4" />
      </Button>
    </div>
  );
};

export const AppSidebar = ({
  user,
  activeView,
  onViewChange,
  friendsSection,
  onFriendsSectionChange,
}: {
  user: AuthUser;
  activeView: AppView;
  onViewChange: (view: AppView) => void;
  friendsSection: FriendsSection;
  onFriendsSectionChange: (section: FriendsSection) => void;
}) => {
  const { theme, toggleTheme } = useTheme();
  const signout = useAuthStore((state) => state.signout);
  const signoutAll = useAuthStore((state) => state.signoutAll);
  const rooms = useChatStore((state) => state.rooms);
  const usersById = useChatStore((state) => state.usersById);
  const selectedRoomId = useChatStore((state) => state.selectedRoomId);
  const setSelectedRoomId = useChatStore((state) => state.setSelectedRoomId);
  const isLoadingRooms = useChatStore((state) => state.isLoadingRooms);
  const incomingRequests = useChatStore((state) => state.incomingFriendRequests);
  const fetchProfile = useChatStore((state) => state.fetchProfile);
  const [query, setQuery] = useState("");
  const [tab, setTab] = useState<SidebarTab>(activeView);
  const [profileOpen, setProfileOpen] = useState(false);
  const [modal, setModal] = useState<"friend" | "requests" | "group" | null>(null);
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const [settingsMenuOpen, setSettingsMenuOpen] = useState(false);
  const [blockedUsersOpen, setBlockedUsersOpen] = useState(false);

  const filteredRooms = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) return rooms;
    return rooms.filter((room) => getRoomName(room, user, usersById).toLowerCase().includes(normalizedQuery));
  }, [query, rooms, user, usersById]);
  const groupRooms = filteredRooms.filter((room) => !isDirectRoom(room));
  const directRooms = filteredRooms.filter(isDirectRoom);

  const tabTitle = tab === "chats" ? "Tin nhắn" : tab === "friends" ? "Bạn bè" : "Hồ sơ";

  useEffect(() => {
    if (tab !== "profile") setTab(activeView);
  }, [activeView, tab]);

  return (
    <aside className="relative flex h-full min-h-0 flex-col border-r border-border/70 bg-card">
      <nav className="flex h-[72px] shrink-0 items-center gap-2 border-b border-border/70 px-5">
        <span className="mr-auto flex items-center gap-2.5 text-xl font-extrabold tracking-tight">
          <span className="brand-gradient grid h-9 w-9 place-items-center rounded-xl text-white shadow-[0_8px_20px_-10px_hsl(var(--primary))]"><MessageCircleMore className="h-5 w-5" /></span>
          InChat
        </span>
        <button
          className="hidden"
          onClick={() => setAccountMenuOpen((current) => !current)}
          aria-label="Mở menu tài khoản"
          aria-expanded={accountMenuOpen}
          title="Tài khoản"
        >
          <UserAvatar name={user.fullName} src={user.avatarUrl} size="md" />
        </button>

        <div className="hidden">
          {(["chats", "friends"] as AppView[]).map((item) => {
            const Icon = item === "chats" ? MessageCircleMore : UsersRound;
            const label = item === "chats" ? "Tin nhắn" : "Bạn bè";
            return (
              <button
                key={item}
                className={cn(
                  "relative grid h-9 w-9 place-items-center rounded-lg transition hover:bg-white/15",
                  tab === item && "bg-white/20 shadow-[inset_0_0_0_1px_rgb(255_255_255/0.2)]",
                  item === "chats" && "hidden",
                )}
                onClick={() => {
                  setTab(item);
                  onViewChange(item);
                  setAccountMenuOpen(false);
                  setSettingsMenuOpen(false);
                }}
                aria-label={label}
                title={label}
              >
                <Icon className="h-4 w-4" strokeWidth={2} />
                {item === "friends" && incomingRequests.length > 0 ? (
                  <span className="absolute right-1 top-1 grid min-w-5 place-items-center rounded-full bg-blue-500 px-1 py-0.5 text-[10px] font-bold text-white ring-2 ring-blue-600">
                    {incomingRequests.length > 9 ? "9+" : incomingRequests.length}
                  </span>
                ) : null}
              </button>
            );
          })}
        </div>

        <div className="hidden">
          <button
            className={cn(
              "grid h-9 w-9 place-items-center rounded-lg transition hover:bg-white/15",
              settingsMenuOpen && "bg-white/20 shadow-[inset_0_0_0_1px_rgb(255_255_255/0.2)]",
            )}
            onClick={() => {
              setSettingsMenuOpen((current) => !current);
              setAccountMenuOpen(false);
            }}
            aria-label="Cài đặt"
            title="Cài đặt"
          >
            <Settings className="h-4 w-4" />
          </button>
        </div>
        <SunMedium className="h-4 w-4 text-muted-foreground" />
        <button
          type="button"
          role="switch"
          aria-checked={theme === "dark"}
          aria-label="Đổi giao diện sáng tối"
          title="Đổi giao diện sáng tối"
          className="relative h-6 w-11 rounded-full bg-muted p-0.5 shadow-inner transition hover:bg-muted/80"
          onClick={toggleTheme}
        >
          <span className={cn(
            "block h-5 w-5 rounded-full bg-white shadow-md transition-transform",
            theme === "dark" && "translate-x-5 bg-blue-500",
          )} />
        </button>
        <MoonStar className="h-4 w-4 text-muted-foreground" />
        <NotificationCenter />
      </nav>

      <button
        type="button"
        className="mx-4 mt-4 flex h-11 shrink-0 items-center gap-3 rounded-xl bg-primary px-3 text-left text-sm font-semibold text-primary-foreground shadow-[0_10px_24px_-16px_hsl(var(--primary))] transition hover:bg-primary/90 active:scale-[0.98]"
        onClick={() => setModal("friend")}
      >
        <span className="grid h-7 w-7 place-items-center rounded-lg bg-white/15 text-white">
          <MessageCircleMore className="h-4 w-4" />
        </span>
        Gửi Tin Nhắn Mới
      </button>

      {accountMenuOpen ? (
        <section className="absolute left-3 top-16 z-40 w-[320px] overflow-hidden rounded-2xl border border-border bg-card text-card-foreground shadow-[0_22px_60px_-18px_hsl(var(--primary)/0.35)]">
          <div className="border-b border-border px-5 py-4">
            <p className="truncate text-lg font-bold">{user.fullName}</p>
            <p className="mt-0.5 truncate text-xs text-muted-foreground">@{user.username}</p>
          </div>
          <div className="p-2">
            <button
              className="flex w-full items-center justify-between rounded-lg px-3 py-3 text-left text-sm transition hover:bg-muted"
              onClick={() => {
                setProfileOpen(true);
                void fetchProfile();
                setAccountMenuOpen(false);
              }}
            >
              <span className="flex items-center gap-3"><UserRound className="h-4 w-4" /> Hồ sơ của bạn</span>
              <ExternalLink className="h-4 w-4 text-muted-foreground" />
            </button>
            <button
              className="flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm transition hover:bg-muted"
              onClick={() => {
                toggleTheme();
                setAccountMenuOpen(false);
              }}
            >
              {theme === "dark" ? <SunMedium className="h-4 w-4" /> : <MoonStar className="h-4 w-4" />}
              {theme === "dark" ? "Chuyển sang giao diện sáng" : "Chuyển sang giao diện tối"}
            </button>
          </div>
          <div className="border-t border-border p-2">
            <button
              className="flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm font-medium text-red-600 transition hover:bg-red-500/10 dark:text-red-400"
              onClick={() => void signout()}
            >
              <LogOut className="h-4 w-4" /> Đăng xuất
            </button>
          </div>
        </section>
      ) : null}

      {settingsMenuOpen ? (
        <section className="absolute right-3 top-16 z-40 w-[290px] rounded-2xl border border-border bg-card p-2 text-card-foreground shadow-[0_22px_60px_-18px_hsl(var(--primary)/0.35)]">
          <button className="flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm transition hover:bg-muted" onClick={() => { setTab("profile"); setSettingsMenuOpen(false); }}>
            <UserRound className="h-5 w-5" /> Thông tin tài khoản
          </button>
          <button className="flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm transition hover:bg-muted" onClick={() => { toggleTheme(); setSettingsMenuOpen(false); }}>
            <Settings className="h-5 w-5" /> Cài đặt
          </button>
          <button className="flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm transition hover:bg-muted" onClick={() => { setBlockedUsersOpen(true); setSettingsMenuOpen(false); }}>
            <UserMinus className="h-5 w-5" /> Danh sách đã chặn
          </button>
          <div className="my-1 border-t border-border" />
          {[
            { icon: Database, label: "Dữ liệu" },
            { icon: Globe2, label: "Ngôn ngữ" },
            { icon: CircleHelp, label: "Hỗ trợ" },
          ].map(({ icon: Icon, label }) => (
            <button
              key={label}
              className="flex w-full items-center justify-between rounded-lg px-3 py-3 text-left text-sm transition hover:bg-muted"
              onClick={() => toast.info(`${label} chưa được backend hỗ trợ.`)}
            >
              <span className="flex items-center gap-3"><Icon className="h-5 w-5" /> {label}</span>
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            </button>
          ))}
          <div className="my-1 border-t border-border" />
          <button className="flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm font-medium text-red-600 transition hover:bg-red-500/10 dark:text-red-400" onClick={() => void signout()}>
            <LogOut className="h-5 w-5" /> Đăng xuất
          </button>
          <button className="flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm font-medium text-red-600 transition hover:bg-red-500/10 dark:text-red-400" onClick={() => void signoutAll()}>
            <LogOut className="h-5 w-5" /> Đăng xuất tất cả thiết bị
          </button>
          <button className="flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm transition hover:bg-muted" onClick={() => setSettingsMenuOpen(false)}>
            <X className="h-5 w-5" /> Thoát
          </button>
        </section>
      ) : null}

      <div className="mt-4 flex min-h-0 flex-1 flex-col bg-card px-4">
        <header className="shrink-0 pb-3">
          <div className="flex items-center justify-between gap-2">
            <div>
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
            <div className="mt-3 space-y-3">
              <label className="relative block">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <input
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  className="h-10 w-full rounded-xl border border-transparent bg-muted/80 pl-9 pr-3 text-sm outline-none transition focus:border-primary/25 focus:bg-card focus:ring-2 focus:ring-ring/15"
                  placeholder="Tìm kiếm"
                />
              </label>
              <button className="hidden" onClick={() => setModal("friend")}>
                <span className="grid h-7 w-7 place-items-center rounded-full bg-primary text-primary-foreground"><Plus className="h-4 w-4" /></span>
                Gửi tin nhắn mới
              </button>
            </div>
          ) : null}
        </header>

        <div className="pretty-scrollbar min-h-0 flex-1 space-y-7 overflow-y-auto py-2">
          {tab === "chats" && (
            isLoadingRooms ? <ConversationSkeleton /> : (
              <div className="space-y-5">
                {filteredRooms.length === 0 ? (
                  <div className="px-3"><EmptyText text="Không tìm thấy cuộc trò chuyện." /></div>
                ) : (
                  <>
                    <ChatRoomSection title="Nhóm chat" rooms={groupRooms} selectedRoomId={selectedRoomId} user={user} usersById={usersById} onSelect={setSelectedRoomId} onAction={() => setModal("group")} />
                    <ChatRoomSection title="Bạn bè" rooms={directRooms} selectedRoomId={selectedRoomId} user={user} usersById={usersById} onSelect={setSelectedRoomId} onAction={() => setModal("friend")} />
                  </>
                )}
              </div>
            )
          )}
          {tab === "friends" && (
            <FriendsNavigation
              active={friendsSection}
              incomingCount={incomingRequests.length}
              onChange={onFriendsSectionChange}
            />
          )}
        </div>
        <div className="shrink-0 border-t border-border/60 bg-card py-2">
          <NavUser user={user} onProfile={() => {
            setProfileOpen(true);
            void fetchProfile();
          }} />
        </div>
      </div>

      {profileOpen ? (
        <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/55 p-4 backdrop-blur-[2px]">
          <section className="pretty-scrollbar relative max-h-[94vh] w-full max-w-[460px] overflow-y-auto rounded-2xl border border-white/70 bg-card shadow-[0_24px_70px_-24px_rgb(15_23_42/0.5)]">
            <Button
              variant="ghost"
              size="icon"
              className="absolute right-5 top-5 z-10 h-8 w-8 rounded-lg text-muted-foreground"
              onClick={() => setProfileOpen(false)}
              aria-label="Đóng hồ sơ"
              title="Đóng hồ sơ"
            >
              <X className="h-4 w-4" />
            </Button>
            <ProfileCard />
          </section>
        </div>
      ) : null}
      {blockedUsersOpen ? <BlockedUsersDialog onClose={() => setBlockedUsersOpen(false)} /> : null}
      {modal === "friend" ? <AddFriendModal onClose={() => setModal(null)} /> : null}
      {modal === "requests" ? <FriendRequestDialog onClose={() => setModal(null)} /> : null}
      {modal === "group" ? <NewGroupChatModal onClose={() => setModal(null)} /> : null}
    </aside>
  );
};

const ChatRoomSection = ({
  title,
  rooms,
  selectedRoomId,
  user,
  usersById,
  onSelect,
  onAction,
}: {
  title: string;
  rooms: ChatRoom[];
  selectedRoomId: string | null;
  user: AuthUser;
  usersById: Record<string, ChatUser>;
  onSelect: (roomId: string) => void;
  onAction: () => void;
}) => (
  <section className="space-y-2">
    <div className="flex items-center justify-between px-1">
      <h2 className="text-xs font-bold uppercase tracking-[0.08em] text-muted-foreground">{title}</h2>
      <button type="button" className="grid h-7 w-7 place-items-center rounded-lg text-muted-foreground transition hover:bg-muted hover:text-primary" onClick={onAction} aria-label={`Thêm ${title.toLowerCase()}`}>
        {title === "Nhóm chat" ? <UsersRound className="h-4 w-4" /> : <UserPlus className="h-4 w-4" />}
      </button>
    </div>
    {rooms.length === 0 ? (
      <p className="px-2 py-2 text-xs text-muted-foreground">Chưa có cuộc trò chuyện.</p>
    ) : rooms.map((room) => (
      <ChatCard
        key={room.id}
        room={room}
        active={room.id === selectedRoomId}
        currentUser={user}
        usersById={usersById}
        onSelect={() => onSelect(room.id)}
      />
    ))}
  </section>
);

const FriendsNavigation = ({
  active,
  incomingCount,
  onChange,
}: {
  active: FriendsSection;
  incomingCount: number;
  onChange: (section: FriendsSection) => void;
}) => {
  const items: { id: FriendsSection; label: string; icon: typeof UsersRound; count?: number }[] = [
    { id: "list", label: "Danh sách bạn bè", icon: UserRound },
    { id: "groups", label: "Danh sách nhóm", icon: UsersRound },
    { id: "requests", label: "Lời mời kết bạn", icon: UserPlus, count: incomingCount },
    { id: "groupRequests", label: "Lời mời vào nhóm", icon: UsersRound },
  ];

  return (
    <div className="-mx-1 space-y-2">
      {items.map(({ id, label, icon: Icon, count }) => (
        <button
          key={id}
          className={cn(
            "flex w-full items-center gap-3 rounded-lg px-4 py-4 text-left text-sm transition hover:bg-muted",
            active === id && "bg-accent font-semibold text-accent-foreground",
          )}
          onClick={() => onChange(id)}
        >
          <Icon className="h-5 w-5 shrink-0" />
          <span className="min-w-0 flex-1 truncate">{label}</span>
          {count ? <span className="rounded-full bg-red-500 px-2 py-0.5 text-xs font-bold text-white">{count}</span> : null}
        </button>
      ))}
    </div>
  );
};

export const FriendsWorkspace = ({
  section,
  onOpenChat,
}: {
  section: FriendsSection;
  onOpenChat: () => void;
}) => {
  const friends = useChatStore((state) => state.friends);
  const rooms = useChatStore((state) => state.rooms);
  const incoming = useChatStore((state) => state.incomingFriendRequests);
  const outgoing = useChatStore((state) => state.outgoingFriendRequests);
  const isLoading = useChatStore((state) => state.isLoadingFriends);
  const isMutating = useChatStore((state) => state.isMutating);
  const createDirectRoom = useChatStore((state) => state.createDirectRoom);
  const acceptFriendRequest = useChatStore((state) => state.acceptFriendRequest);
  const rejectFriendRequest = useChatStore((state) => state.rejectFriendRequest);
  const setSelectedRoomId = useChatStore((state) => state.setSelectedRoomId);
  const [query, setQuery] = useState("");
  const [firstMessages, setFirstMessages] = useState<Record<string, string>>({});

  const filteredFriends = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return [...friends]
      .filter(({ friend }) => !normalized || getFriendName(friend).toLowerCase().includes(normalized))
      .sort((a, b) => getFriendName(a.friend).localeCompare(getFriendName(b.friend), "vi"));
  }, [friends, query]);

  const title =
    section === "list" ? "Danh sách bạn bè" :
    section === "groups" ? "Danh sách nhóm" :
    section === "requests" ? "Lời mời kết bạn" : "Lời mời vào nhóm";

  return (
    <section className="flex h-full min-h-0 flex-col bg-muted/60">
      <header className="flex h-[78px] items-center gap-3 border-b border-border bg-card px-6">
        {section === "list" ? <UserRound className="h-6 w-6" /> : <UsersRound className="h-6 w-6" />}
        <h1 className="text-lg font-bold">{title}</h1>
      </header>

      <div className="pretty-scrollbar min-h-0 flex-1 overflow-y-auto">
        <div className="border-b border-border bg-muted/70 px-5 py-5 text-sm font-bold">
          {section === "list" ? `Bạn bè (${friends.length})` : title}
        </div>

        <div className="m-5 rounded-xl border border-border bg-card p-5">
          {section === "list" ? (
            <>
              <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_220px]">
                <label className="relative">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <input value={query} onChange={(event) => setQuery(event.target.value)} className="h-10 w-full rounded-lg border border-input bg-background pl-9 pr-3 text-sm outline-none focus:ring-2 focus:ring-ring/20" placeholder="Tìm bạn" />
                </label>
                <div className="flex h-10 items-center gap-2 rounded-lg border border-input px-3 text-sm">
                  <SlidersHorizontal className="h-4 w-4" /> Tên (A-Z)
                </div>
              </div>
              <div className="mt-6">
                {isLoading ? <ConversationSkeleton /> : filteredFriends.length === 0 ? <EmptyText text="Không tìm thấy bạn bè." /> : (
                  <div className="divide-y divide-border">
                    {filteredFriends.map(({ id, friend }) => (
                      <div
                        key={id}
                        className="flex w-full items-center gap-4 px-2 py-4 text-left transition hover:bg-muted"
                        onClick={() => void createDirectRoom(friend.id).then((room) => { if (room) onOpenChat(); })}
                      >
                        <UserAvatar name={getFriendName(friend)} src={friend.avatarEndpoint ?? friend.avatar} size="lg" />
                        <div className="min-w-0">
                          <p className="truncate font-semibold">{getFriendName(friend)}</p>
                          <p className="truncate text-xs text-muted-foreground">@{friend.username}</p>
                        </div>
                        <FriendActions friend={friend} />
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
          ) : section === "groups" ? (
            <div className="space-y-2">
              {rooms.filter((room) => !isDirectRoom(room)).length === 0 ? <EmptyText text="Bạn chưa tham gia nhóm nào." /> : rooms.filter((room) => !isDirectRoom(room)).map((room) => (
                <button
                  key={room.id}
                  type="button"
                  className="flex w-full items-center gap-4 border-b border-border px-2 py-4 text-left transition hover:bg-muted last:border-0"
                  onClick={() => {
                    setSelectedRoomId(room.id);
                    onOpenChat();
                  }}
                >
                  <UserAvatar name={room.name || "Nhóm"} src={room.avatarEndpoint ?? room.avatar} size="lg" />
                  <div><p className="font-semibold">{room.name || "Nhóm chat"}</p><p className="text-xs text-muted-foreground">{room.memberIds.length} thành viên</p></div>
                </button>
              ))}
            </div>
          ) : section === "requests" ? (
            <div className="grid gap-6 xl:grid-cols-2">
              <div className="space-y-3">
                <h2 className="font-bold">Đã nhận ({incoming.length})</h2>
                {incoming.length === 0 ? <EmptyText text="Không có lời mời mới." /> : incoming.map((request) => (
                  <FriendRequestItem
                    key={request.id}
                    request={request}
                    value={firstMessages[request.id] ?? ""}
                    onChange={(value) => setFirstMessages((current) => ({ ...current, [request.id]: value }))}
                    onAccept={() => void acceptFriendRequest(request.id, firstMessages[request.id])}
                    onReject={() => void rejectFriendRequest(request.id)}
                    disabled={isMutating}
                  />
                ))}
              </div>
              <div className="space-y-3">
                <h2 className="font-bold">Đã gửi ({outgoing.length})</h2>
                {outgoing.length === 0 ? <EmptyText text="Bạn chưa gửi lời mời nào." /> : outgoing.map((request) => (
                  <div key={request.id} className="flex items-center gap-2 rounded-lg border border-border p-3">
                    <div className="min-w-0 flex-1"><SimpleUserRow user={request.receiver} suffix="Đang chờ" /></div>
                    <Button variant="ghost" size="sm" onClick={() => void useChatStore.getState().cancelFriendRequest(request.id)}>Thu hồi</Button>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <GroupRequestsWorkspace rooms={rooms} />
          )}
        </div>
      </div>
    </section>
  );
};

export const ProfileCard = () => {
  const storedProfile = useChatStore((state) => state.profile);
  const authUser = useAuthStore((state) => state.user);
  const { theme, toggleTheme } = useTheme();
  const isLoadingProfile = useChatStore((state) => state.isLoadingProfile);
  const isMutating = useChatStore((state) => state.isMutating);
  const updateProfile = useChatStore((state) => state.updateProfile);
  const uploadAvatar = useChatStore((state) => state.uploadAvatar);
  const [form, setForm] = useState({ username: "", displayName: "", bio: "", phone: "", themePreference: "system" });
  const [profileTab, setProfileTab] = useState<"account" | "preferences" | "privacy">("account");
  const [showOnlineStatus, setShowOnlineStatus] = useState(true);
  const profile = useMemo(() => storedProfile ?? (authUser ? {
    id: authUser.id,
    username: authUser.username,
    email: authUser.email,
    displayName: authUser.fullName,
    bio: null,
    phone: null,
    themePreference: "system",
    avatarEndpoint: authUser.avatarUrl,
    avatar: authUser.avatarUrl,
    online: authUser.isOnline,
    lastSeenAt: authUser.lastSeenAt,
  } : null), [authUser, storedProfile]);

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

  if (!profile) {
    return (
      <div className="space-y-4 p-4">
        <SkeletonLine className="h-6 w-48" />
        <SkeletonLine className="h-44 w-full rounded-2xl" />
        <SkeletonLine className="h-11 w-full rounded-xl" />
        <SkeletonLine className="h-72 w-full rounded-xl" />
      </div>
    );
  }

  const submit = (event: FormEvent) => {
    event.preventDefault();
    void updateProfile(form);
  };

  return (
    <form className="overflow-hidden rounded-2xl border border-border bg-card p-4 shadow-[0_18px_45px_-30px_hsl(var(--primary)/0.5)]" onSubmit={submit}>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-xl font-extrabold tracking-tight">Profile &amp; Settings</h2>
        {isLoadingProfile ? <Loader2 className="mr-10 h-4 w-4 animate-spin text-primary" /> : null}
      </div>
      <div className="profile-cover relative min-h-44 overflow-hidden rounded-2xl p-6 text-white shadow-[0_16px_30px_-18px_hsl(var(--primary)/0.75)]">
        <div className="absolute -right-8 -top-10 h-32 w-32 rounded-full bg-white/10" />
        <div className="absolute bottom-0 right-16 h-20 w-20 rounded-full bg-white/10 blur-xl" />
        <div className="relative flex items-end gap-5 pt-12">
          <div className="relative">
            <UserAvatar name={form.displayName || form.username} src={profile.avatarEndpoint ?? profile.avatar} size="xl" />
            <label className="absolute -bottom-1 -right-1 grid h-9 w-9 cursor-pointer place-items-center rounded-full border-2 border-white bg-secondary text-primary shadow-md transition hover:scale-105">
              <Camera className="h-4 w-4" />
              <span className="sr-only">Avatar</span>
              <input type="file" accept="image/*" className="sr-only" onChange={(event) => handleAvatarChange(event, uploadAvatar)} />
            </label>
          </div>
          <div className="min-w-0 flex-1 pb-2">
            <p className="truncate text-2xl font-bold">{form.displayName || form.username}</p>
            <p className="mt-1 line-clamp-2 text-sm text-white/80">{form.bio || `@${form.username}`}</p>
          </div>
          <span className="mb-3 inline-flex items-center gap-1.5 rounded-full bg-white px-3 py-1 text-xs font-bold text-emerald-600 shadow-sm">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
            {profile.online ? "Online" : "Offline"}
          </span>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-3 rounded-xl bg-muted/60 p-1">
        {([
          ["account", "Tài khoản"],
          ["preferences", "Cấu hình"],
          ["privacy", "Bảo mật"],
        ] as const).map(([id, label]) => (
          <button
            key={id}
            type="button"
            className={cn(
              "rounded-lg px-1 py-2.5 text-sm font-semibold text-muted-foreground transition",
              profileTab === id && "bg-card text-foreground shadow-sm",
            )}
            onClick={() => setProfileTab(id)}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="mt-3 space-y-4 rounded-xl border border-border bg-card p-5 shadow-sm">
        {profileTab === "account" ? (
          <>
            <div>
              <p className="flex items-center gap-2 text-base font-bold"><Heart className="h-5 w-5 text-primary" /> Thông tin cá nhân</p>
              <p className="mt-1 text-xs leading-5 text-muted-foreground">Cập nhật chi tiết cá nhân và thông tin hồ sơ của bạn.</p>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <TextInput label="Họ và tên" value={form.displayName} onChange={(value) => setForm((current) => ({ ...current, displayName: value }))} />
              <TextInput label="Tên người dùng" value={form.username} onChange={(value) => setForm((current) => ({ ...current, username: value }))} />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <label className="block space-y-1.5">
                <span className="text-xs font-semibold text-muted-foreground">Email</span>
                <input value={profile.email} readOnly className="h-10 w-full rounded-xl border border-input bg-muted/40 px-3 text-sm outline-none" />
              </label>
              <TextInput label="Số điện thoại" value={form.phone} onChange={(value) => setForm((current) => ({ ...current, phone: value }))} />
            </div>
            <label className="block space-y-1.5">
              <span className="text-xs font-semibold text-muted-foreground">Giới thiệu</span>
              <textarea value={form.bio} onChange={(event) => setForm((current) => ({ ...current, bio: event.target.value }))} rows={3} className="w-full resize-none rounded-xl border border-input bg-background px-3 py-2 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/15" />
            </label>
          </>
        ) : profileTab === "preferences" ? (
          <div className="space-y-5">
            <div>
              <p className="flex items-center gap-2 text-base font-bold"><SlidersHorizontal className="h-5 w-5 text-primary" /> Tùy chỉnh ứng dụng</p>
              <p className="mt-1 text-xs leading-5 text-muted-foreground">Cá nhân hóa trải nghiệm trò chuyện của bạn.</p>
            </div>
            <div className="flex items-center gap-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold">Chế độ tối</p>
                <p className="mt-0.5 text-xs text-muted-foreground">Chuyển đổi giữa giao diện sáng và tối</p>
              </div>
              <SunMedium className="h-4 w-4 text-muted-foreground" />
              <button
                type="button"
                role="switch"
                aria-checked={theme === "dark"}
                className={cn(
                  "relative h-6 w-11 rounded-full p-0.5 transition",
                  theme === "dark" ? "bg-primary" : "bg-muted",
                )}
                onClick={() => {
                  toggleTheme();
                  setForm((current) => ({ ...current, themePreference: theme === "dark" ? "light" : "dark" }));
                }}
              >
                <span className={cn("block h-5 w-5 rounded-full bg-white shadow-sm transition-transform", theme === "dark" && "translate-x-5")} />
              </button>
              <MoonStar className="h-4 w-4 text-muted-foreground" />
            </div>
            <div className="flex items-center gap-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold">Hiển thị trạng thái online</p>
                <p className="mt-0.5 text-xs text-muted-foreground">Cho phép người khác thấy khi bạn đang online</p>
              </div>
              <button
                type="button"
                role="switch"
                aria-checked={showOnlineStatus}
                className={cn(
                  "relative h-6 w-11 rounded-full p-0.5 transition",
                  showOnlineStatus ? "bg-primary" : "bg-muted",
                )}
                onClick={() => setShowOnlineStatus((current) => !current)}
              >
                <span className={cn("block h-5 w-5 rounded-full bg-white shadow-sm transition-transform", showOnlineStatus && "translate-x-5")} />
              </button>
            </div>
          </div>
        ) : (
          <div className="rounded-xl border border-border bg-muted/60 p-4">
            <p className="text-sm font-bold">Quyền riêng tư</p>
            <p className="mt-2 text-xs leading-5 text-muted-foreground">Trạng thái trực tuyến và thông tin hồ sơ đang được đồng bộ an toàn qua tài khoản của bạn.</p>
          </div>
        )}
        {profileTab === "account" ? (
          <Button type="submit" className="brand-gradient rounded-xl border-0 px-5 text-white" disabled={isMutating}>{isMutating ? "Đang lưu..." : "Lưu thay đổi"}</Button>
        ) : null}
      </div>
    </form>
  );
};

const handleAvatarChange = (event: ChangeEvent<HTMLInputElement>, uploadAvatar: (file: File) => Promise<void>) => {
  const file = event.target.files?.[0];
  if (!file) return;

  if (!file.type.startsWith("image/")) {
    toast.error("Chỉ chấp nhận tệp hình ảnh.");
  } else if (file.size > 5 * 1024 * 1024) {
    toast.error("Ảnh đại diện không được vượt quá 5 MB.");
  } else {
    void uploadAvatar(file);
  }
  event.target.value = "";
};

const TextInput = ({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) => (
  <label className="block space-y-1.5">
    <span className="text-xs font-semibold text-muted-foreground">{label}</span>
    <input value={value} onChange={(event) => onChange(event.target.value)} className="h-10 w-full rounded-xl border border-input bg-background px-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/15" />
  </label>
);

export const AddFriendModal = ({ onClose }: { onClose: () => void }) => {
  const currentUser = useAuthStore((state) => state.user);
  const sendFriendRequest = useChatStore((state) => state.sendFriendRequest);
  const isMutating = useChatStore((state) => state.isMutating);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<ChatUser[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      setLoading(true);
      void chatApi.searchUsers(query).then((response) => {
        if (active) {
          setResults(response.data.filter((user) => user.id !== currentUser?.id));
        }
      }).catch(() => {
        if (active) {
          setResults([]);
          toast.error("Không thể tìm kiếm người dùng.");
        }
      }).finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    }, 250);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [currentUser?.id, query]);

  return (
    <Modal title="Kết Bạn" onClose={onClose} size="compact">
      <div className="space-y-3">
        <label className="block space-y-2">
          <span className="text-sm font-bold">Tìm bằng username</span>
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            className="h-11 w-full rounded-xl border-2 border-primary/70 bg-background px-3 text-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
            placeholder="Gõ tên username vào đây..."
          />
        </label>
        <div className="grid grid-cols-2 gap-2">
          <Button variant="outline" className="h-10 rounded-xl" onClick={onClose}>Cancel</Button>
          <Button className="brand-gradient h-10 rounded-xl border-0 text-white" disabled={!query.trim() || loading}>
            <Search className="h-4 w-4" /> Tìm
          </Button>
        </div>
        {query.trim() ? (
          <div className="pretty-scrollbar max-h-56 space-y-2 overflow-y-auto pt-1">
            {loading ? <ConversationSkeleton /> : results.length === 0 ? (
              <p className="rounded-xl bg-muted/60 p-3 text-center text-sm text-muted-foreground">Không tìm thấy người dùng.</p>
            ) : results.map((user) => (
              <div key={user.id} className="flex items-center gap-3 rounded-xl border border-border p-3">
                <UserAvatar name={user.displayName || user.username} src={user.avatarEndpoint ?? user.avatar} />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold">{user.displayName || user.username}</p>
                  <p className="truncate text-xs text-muted-foreground">@{user.username}</p>
                </div>
                <Button size="sm" className="brand-gradient rounded-lg border-0 text-white" disabled={isMutating} onClick={() => void sendFriendRequest(user.id)}>Kết bạn</Button>
              </div>
            ))}
          </div>
        ) : null}
      </div>
    </Modal>
  );
};

export const FriendRequestDialog = ({ onClose }: { onClose: () => void }) => {
  const incoming = useChatStore((state) => state.incomingFriendRequests);
  const outgoing = useChatStore((state) => state.outgoingFriendRequests);
  const acceptFriendRequest = useChatStore((state) => state.acceptFriendRequest);
  const rejectFriendRequest = useChatStore((state) => state.rejectFriendRequest);
  const isMutating = useChatStore((state) => state.isMutating);
  const [activeRequestTab, setActiveRequestTab] = useState<"received" | "sent">("received");

  return (
    <Modal title="Lời mời kết bạn" onClose={onClose} size="compact">
      <div className="space-y-3">
        <div className="grid grid-cols-2 rounded-xl bg-muted/70 p-1">
          <button
            type="button"
            className={cn(
              "rounded-lg px-3 py-1.5 text-xs font-bold text-muted-foreground transition",
              activeRequestTab === "received" && "border-2 border-primary bg-card py-1 text-foreground shadow-sm",
            )}
            onClick={() => setActiveRequestTab("received")}
          >
            Đã nhận
          </button>
          <button
            type="button"
            className={cn(
              "rounded-lg px-3 py-1.5 text-xs font-bold text-muted-foreground transition",
              activeRequestTab === "sent" && "border-2 border-primary bg-card py-1 text-foreground shadow-sm",
            )}
            onClick={() => setActiveRequestTab("sent")}
          >
            Đã gửi
          </button>
        </div>

        <div className="pretty-scrollbar max-h-64 space-y-2 overflow-y-auto">
          {activeRequestTab === "received" ? (
            incoming.length === 0 ? <EmptyText text="Không có lời mời mới." /> : incoming.map((request) => (
              <div key={request.id} className="flex items-center gap-3 rounded-xl border border-border bg-card p-2.5 shadow-sm">
                <UserAvatar name={getFriendName(request.requester)} src={request.requester.avatarEndpoint ?? request.requester.avatar} />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold">{getFriendName(request.requester)}</p>
                  <p className="truncate text-xs text-muted-foreground">@{request.requester.username}</p>
                </div>
                <Button variant="outline" size="sm" className="h-8 rounded-lg border-primary/40 px-3 text-primary" disabled={isMutating} onClick={() => void acceptFriendRequest(request.id)}>
                  Chấp nhận
                </Button>
                <button type="button" className="shrink-0 text-xs font-semibold text-muted-foreground underline underline-offset-2 hover:text-foreground" disabled={isMutating} onClick={() => void rejectFriendRequest(request.id)}>
                  Từ chối
                </button>
              </div>
            ))
          ) : (
            outgoing.length === 0 ? <EmptyText text="Bạn chưa gửi lời mời nào." /> : outgoing.map((request) => (
              <div key={request.id} className="rounded-xl border border-border bg-card p-2.5 shadow-sm">
                <SimpleUserRow user={request.receiver} suffix="Đang chờ" />
              </div>
            ))
          )}
        </div>
      </div>
    </Modal>
  );
};

export const FriendRequestItem = ({
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

export const NewGroupChatModal = ({ onClose }: { onClose: () => void }) => {
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

  if (!selectedRoom || !selectedRoomId) return <ChatWelcomeScreen />;

  return (
    <section className="chat-surface flex h-full min-h-0 flex-col">
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

export const ChatWelcomeScreen = () => (
  <section className="chat-surface grid h-full place-items-center p-6">
    <div className="max-w-md text-center">
      <div className="welcome-orb mx-auto grid h-28 w-28 place-items-center rounded-full">
        <div className="welcome-chat-icon brand-gradient grid h-20 w-20 place-items-center rounded-full text-white shadow-[0_18px_40px_-12px_hsl(var(--primary)/0.7)]">
          <MessageCircleMore className="h-8 w-8" />
        </div>
      </div>
      <h1 className="mt-5 bg-gradient-to-r from-blue-600 to-cyan-500 bg-clip-text text-2xl font-extrabold tracking-tight text-transparent">
        Chào mừng bạn đến với InChat!
      </h1>
      <p className="mt-2 text-sm font-medium leading-6 text-muted-foreground">Chọn một cuộc hội thoại để bắt đầu chat!</p>
    </div>
  </section>
);

export const ChatWindowHeader = ({ room, user, usersById }: { room: ChatRoom; user: AuthUser; usersById: Record<string, ChatUser> }) => {
  const name = getRoomName(room, user, usersById);
  const online = getRoomOnline(room, user, usersById);
  const peer = getRoomPeer(room, user.id, usersById);
  const [searchOpen, setSearchOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);

  return (
    <header className="flex min-h-[80px] items-center gap-3 border-b border-border/60 bg-card px-5 shadow-[0_1px_0_hsl(var(--border)/0.35)] md:px-7">
      <UserAvatar name={name} src={getRoomAvatar(room, user, usersById)} online={online} size="lg" />
      <div className="min-w-0 flex-1">
        <h1 className="truncate text-base font-bold tracking-tight">{name}</h1>
        <div className="mt-0.5 flex items-center gap-2">
          <StatusBadge online={online} />
          {!online && peer?.lastSeenAt ? <span className="text-xs text-muted-foreground">Last seen {formatRelativeTime(peer.lastSeenAt)}</span> : null}
        </div>
      </div>
      {!isDirectRoom(room) ? <span className="hidden">{room.memberIds.length} thành viên</span> : null}
      <div className="hidden">
        {[
          { icon: Search, label: "Tìm trong cuộc trò chuyện", message: "Tính năng tìm kiếm trong hội thoại đang được chuẩn bị." },
          { icon: Phone, label: "Gọi thoại", message: "Cuộc gọi thoại cần kết nối dịch vụ WebRTC." },
          { icon: Video, label: "Gọi video", message: "Cuộc gọi video cần kết nối dịch vụ WebRTC." },
          { icon: CircleHelp, label: "Thông tin hội thoại", message: `${name} có ${room.memberIds.length} thành viên.` },
        ].map(({ icon: Icon, label, message }) => (
          <Button key={label} variant="ghost" size="icon" className="h-10 w-10 rounded-xl text-muted-foreground hover:text-primary" aria-label={label} title={label} onClick={() => toast.info(message)}>
            <Icon className="h-4.5 w-4.5" />
          </Button>
        ))}
      </div>
      <div className="flex gap-1">
        <Button variant="ghost" size="icon" className="h-10 w-10 rounded-xl text-muted-foreground hover:text-primary" aria-label="Tìm trong cuộc trò chuyện" onClick={() => setSearchOpen(true)}><Search className="h-5 w-5" /></Button>
        {!isDirectRoom(room) ? <Button variant="ghost" size="icon" className="h-10 w-10 rounded-xl text-muted-foreground hover:text-primary" aria-label="Quản lý nhóm" onClick={() => setSettingsOpen(true)}><Settings className="h-5 w-5" /></Button> : null}
      </div>
      {searchOpen ? <ConversationSearchDialog roomId={room.id} onClose={() => setSearchOpen(false)} /> : null}
      {settingsOpen ? <RoomSettingsDialog room={room} user={user} onClose={() => setSettingsOpen(false)} /> : null}
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
  const typingUsers = useChatStore((state) => state.typingByRoomId[roomId] ?? EMPTY_TYPING_USERS);
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
    const previousTop = node.scrollTop;
    void fetchOlderMessages(roomId).then(() => {
      window.requestAnimationFrame(() => {
        if (scrollRef.current) {
          scrollRef.current.scrollTop =
            scrollRef.current.scrollHeight - previousHeight + previousTop;
        }
        markVisibleMessages(roomId);
      });
    });
  };

  return (
    <div ref={scrollRef} onScroll={handleScroll} className="pretty-scrollbar min-h-0 flex-1 overflow-y-auto px-4 py-5 md:px-7">
      {pageState?.loadingOlder ? <p className="mb-3 flex items-center justify-center gap-2 text-xs text-muted-foreground"><Loader2 className="h-3.5 w-3.5 animate-spin" /> Loading older messages</p> : null}
      {loading ? (
        <ChatWindowSkeleton />
      ) : messages.length === 0 ? (
        <div className="grid h-full place-items-center">
          <p className="text-base font-medium text-muted-foreground">Chưa có tin nhắn nào trong cuộc trò chuyện này.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {messages.map((message) => (
            <MessageItem key={message.id} message={message} mine={message.senderId === currentUser.id} sender={usersById[message.senderId]} currentUserId={currentUser.id} />
          ))}
        </div>
      )}
      {typingUsers.filter((item) => item.userId !== currentUser.id).length > 0 ? (
        <p className="mt-3 text-xs font-medium text-primary">
          {typingUsers.filter((item) => item.userId !== currentUser.id).map((item) => item.username).join(", ")} đang nhập...
        </p>
      ) : null}
      <div ref={bottomRef} />
    </div>
  );
};

export const ChatWindowSkeleton = () => (
  <div className="space-y-3">
    <SkeletonLine className="h-12 w-2/3" />
    <SkeletonLine className="ml-auto h-12 w-1/2" />
    <SkeletonLine className="h-12 w-3/5" />
  </div>
);

export const MessageItem = ({ message, mine, sender, currentUserId }: { message: ChatMessage; mine: boolean; sender?: ChatUser; currentUserId: string }) => {
  const senderName = sender?.displayName || sender?.username || "User";
  const receipt = getReceiptLabel(message, currentUserId);
  const recallMessage = useChatStore((state) => state.recallMessage);
  const deleteMessageForMe = useChatStore((state) => state.deleteMessageForMe);

  return (
    <div className={cn("group mx-auto flex max-w-none gap-2.5", mine && "justify-end")}>
      {!mine ? <UserAvatar name={senderName} src={sender?.avatarEndpoint ?? sender?.avatar} size="sm" /> : null}
      <div className={cn("max-w-[76%]", mine && "text-right")}>
        {!mine ? <p className="mb-1 px-1 text-xs font-medium text-muted-foreground">{senderName}</p> : null}
        <div className={cn(
          "inline-block space-y-2 rounded-2xl px-3.5 py-2 text-left text-sm leading-6",
          mine
            ? "rounded-br-md bg-primary text-primary-foreground shadow-[0_8px_18px_-14px_hsl(var(--primary))]"
            : "rounded-bl-md border border-border/70 bg-card text-card-foreground shadow-[0_5px_14px_-12px_hsl(var(--foreground)/0.3)]",
        )}>
          {message.content ? <p className="whitespace-pre-wrap break-words">{message.content}</p> : null}
          {message.attachments?.map((attachment) => {
            const url = resolveMediaUrl(attachment.downloadEndpoint);
            if (!url) return null;

            if (attachment.mimeType?.toLowerCase().startsWith("image/") || attachment.fileType?.toLowerCase() === "image") {
              return (
                <a key={attachment.id} href={url} target="_blank" rel="noreferrer" className="block overflow-hidden rounded-lg">
                  <img src={url} alt={attachment.originalName || "Ảnh đính kèm"} className="max-h-72 w-full object-cover" loading="lazy" />
                </a>
              );
            }

            const fileSize = formatFileSize(attachment.fileSize);
            return (
              <a key={attachment.id} href={url} target="_blank" rel="noreferrer" className="block rounded-lg border border-current/20 px-3 py-2 underline-offset-4 hover:underline">
                {attachment.originalName || "Tệp đính kèm"}
                {fileSize ? ` (${fileSize})` : ""}
              </a>
            );
          })}
        </div>
        <div className={cn("mt-1 flex h-4 gap-2 opacity-0 transition group-hover:opacity-60 hover:!opacity-100", mine && "justify-end")}>
          {mine && !message.recalled ? <button className="text-[11px] hover:underline" onClick={() => void recallMessage(message.id)}>Thu hồi</button> : null}
          <button className="text-[11px] hover:underline" onClick={() => void deleteMessageForMe(message.id)}>Xóa phía tôi</button>
        </div>
        <p className="px-1 text-[10px] text-muted-foreground/80">
          {new Date(message.timestamp).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" })}
          {mine ? ` - ${receipt}` : ""}
        </p>
      </div>
    </div>
  );
};

const getReceiptLabel = (message: ChatMessage, currentUserId: string) => {
  const readByOthers = (message.readByUserIds ?? []).filter((id) => id !== currentUserId);
  if (readByOthers.length > 0 || message.status?.toLowerCase() === "seen") return "Seen";
  const deliveredToOthers = (message.deliveredToUserIds ?? []).filter((id) => id !== currentUserId);
  if (deliveredToOthers.length > 0 || message.status?.toLowerCase() === "delivered") return "Delivered";
  return "Sent";
};

export const MessageInput = () => {
  const sendMessage = useChatStore((state) => state.sendMessage);
  const sendAttachment = useChatStore((state) => state.sendAttachment);
  const sendTyping = useChatStore((state) => state.sendTyping);
  const isSending = useChatStore((state) => state.isSending);
  const [value, setValue] = useState("");
  const [emojiOpen, setEmojiOpen] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const typingTimerRef = useRef<number | null>(null);

  const sendCurrentValue = () => {
    const content = value.trim();
    if (!content) return;
    setValue("");
    setEmojiOpen(false);
    sendTyping(false);
    void sendMessage(content);
  };

  useEffect(() => () => {
    if (typingTimerRef.current) window.clearTimeout(typingTimerRef.current);
    sendTyping(false);
  }, [sendTyping]);

  const handleTyping = (nextValue: string) => {
    setValue(nextValue);
    sendTyping(Boolean(nextValue.trim()));
    if (typingTimerRef.current) window.clearTimeout(typingTimerRef.current);
    typingTimerRef.current = window.setTimeout(() => sendTyping(false), 1800);
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    sendCurrentValue();
  };

  return (
    <form onSubmit={submit} className="border-t border-border/60 bg-card px-4 py-2.5 md:px-6">
      <div className="mx-auto flex items-end gap-1 rounded-xl border border-input/70 bg-card p-1 transition focus-within:border-primary/45 focus-within:ring-2 focus-within:ring-primary/10">
        <input ref={fileInputRef} type="file" className="sr-only" onChange={(event) => { const file = event.target.files?.[0]; if (file) void sendAttachment(file, value).then(() => setValue("")); event.target.value = ""; }} />
        <Button type="button" variant="ghost" size="icon" className="h-10 w-10 rounded-xl text-muted-foreground hover:text-primary" aria-label="Đính kèm tệp" title="Đính kèm tệp" onClick={() => fileInputRef.current?.click()}>
          <Paperclip className="h-5 w-5" />
        </Button>
        <div className="relative">
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="h-10 w-10 rounded-xl text-muted-foreground hover:text-primary"
            aria-label="Chọn emoji"
            aria-expanded={emojiOpen}
            onClick={() => setEmojiOpen((current) => !current)}
          >
            <Smile className="h-5 w-5" />
          </Button>
          {emojiOpen ? (
            <div className="absolute bottom-12 left-0 z-30 w-80">
              <EmojiPicker
                onSelect={(emoji) => {
                  setValue((current) => `${current}${emoji}`);
                  setEmojiOpen(false);
                }}
              />
            </div>
          ) : null}
        </div>
        <textarea
          value={value}
          onChange={(event) => handleTyping(event.target.value)}
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
        <Button type="submit" size="icon" className="h-10 w-10 rounded-xl shadow-[0_8px_18px_-8px_hsl(var(--primary)/0.8)]" disabled={isSending || !value.trim()} aria-label="Send message" title="Send">
          <SendHorizontal className="h-5 w-5" />
        </Button>
      </div>
    </form>
  );
};
