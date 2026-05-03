import {
  Hash,
  LogOut,
  MessageCircleMore,
  MoonStar,
  Search,
  SendHorizontal,
  SmilePlus,
  Sparkles,
  SunMedium,
  UsersRound,
} from "lucide-react";
import {
  type FormEvent,
  type KeyboardEvent,
  type ReactNode,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { useTheme } from "@/components/theme-provider";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";
import { useChatStore } from "@/store/chat-store";
import type { AuthUser } from "@/types/auth";
import type { ChatMessage, ChatRoom, ChatUser } from "@/types/chat";

const emojiItems = [
  "😀",
  "😁",
  "😂",
  "😊",
  "😍",
  "😎",
  "🥳",
  "👍",
  "🙏",
  "🔥",
  "✨",
  "💬",
  "❤️",
  "✅",
  "🎯",
  "🚀",
];

const formatRelativeTime = (value?: string | null) => {
  if (!value) {
    return "";
  }

  const date = new Date(value);
  const diffMs = Date.now() - date.getTime();
  const diffMinutes = Math.max(1, Math.floor(diffMs / 60000));

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  if (diffMinutes < 60) {
    return `${diffMinutes}m`;
  }

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) {
    return `${diffHours}h`;
  }

  return date.toLocaleDateString(undefined, {
    day: "2-digit",
    month: "short",
  });
};

const getInitials = (name: string) =>
  name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("") || "IC";

const isDirectRoom = (room: ChatRoom) => room.type?.toUpperCase() === "DIRECT";

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
  if (!isDirectRoom(room)) {
    return room.name?.trim() || "Group chat";
  }

  const peer = getRoomPeer(room, currentUser.id, usersById);
  return peer?.displayName?.trim() || peer?.username || room.name || "Direct chat";
};

const getRoomAvatar = (
  room: ChatRoom,
  currentUser: AuthUser,
  usersById: Record<string, ChatUser>,
) => {
  if (!isDirectRoom(room)) {
    return room.avatar;
  }

  return getRoomPeer(room, currentUser.id, usersById)?.avatar ?? room.avatar;
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
      "relative grid shrink-0 place-items-center overflow-hidden rounded-lg border border-border bg-secondary font-semibold text-secondary-foreground",
      size === "sm" && "h-9 w-9 text-xs",
      size === "md" && "h-11 w-11 text-sm",
      size === "lg" && "h-12 w-12 text-base",
    )}
  >
    {src ? (
      <img src={src} alt={name} className="h-full w-full object-cover" />
    ) : (
      <span>{getInitials(name)}</span>
    )}
    {online !== undefined ? <StatusBadge online={online} compact /> : null}
  </div>
);

export const StatusBadge = ({
  online,
  compact,
}: {
  online: boolean;
  compact?: boolean;
}) => (
  <span
    className={cn(
      "inline-flex items-center gap-1.5 text-xs font-medium",
      compact
        ? "absolute bottom-0 right-0 h-3 w-3 rounded-full border-2 border-card p-0"
        : "text-muted-foreground",
    )}
  >
    <span
      className={cn(
        "h-2.5 w-2.5 rounded-full",
        online ? "bg-emerald-500" : "bg-muted-foreground/50",
        compact && "h-full w-full",
      )}
    />
    {compact ? null : online ? "Online" : "Offline"}
  </span>
);

export const UnreadBadge = ({ count }: { count: number }) => {
  if (count <= 0) {
    return null;
  }

  return (
    <span className="grid min-w-6 place-items-center rounded-full bg-primary px-2 py-1 text-xs font-bold text-primary-foreground">
      {count > 99 ? "99+" : count}
    </span>
  );
};

interface ChatCardProps {
  room: ChatRoom;
  active: boolean;
  currentUser: AuthUser;
  usersById: Record<string, ChatUser>;
  icon: "direct" | "group";
  onSelect: () => void;
}

const ChatCard = ({
  room,
  active,
  currentUser,
  usersById,
  icon,
  onSelect,
}: ChatCardProps) => {
  const name = getRoomName(room, currentUser, usersById);
  const online = getRoomOnline(room, currentUser, usersById);

  return (
    <button
      className={cn(
        "grid w-full grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 rounded-lg border p-3 text-left transition",
        active
          ? "border-primary/45 bg-primary/10 shadow-soft"
          : "border-transparent hover:border-border hover:bg-muted/70",
      )}
      onClick={onSelect}
    >
      <UserAvatar
        name={name}
        src={getRoomAvatar(room, currentUser, usersById)}
        online={online}
      />
      <span className="min-w-0">
        <span className="flex min-w-0 items-center gap-2">
          {icon === "group" ? (
            <UsersRound className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
          ) : (
            <Hash className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
          )}
          <span className="truncate text-sm font-semibold">{name}</span>
        </span>
        <span className="mt-1 block truncate text-xs text-muted-foreground">
          {room.lastMessagePreview || "Chua co tin nhan"}
        </span>
      </span>
      <span className="flex flex-col items-end gap-2">
        <span className="text-xs text-muted-foreground">
          {formatRelativeTime(room.lastMessageAt)}
        </span>
        <UnreadBadge count={room.unreadCount} />
      </span>
    </button>
  );
};

export const DirectMessageCard = (props: Omit<ChatCardProps, "icon">) => (
  <ChatCard {...props} icon="direct" />
);

export const GroupChatCard = (props: Omit<ChatCardProps, "icon">) => (
  <ChatCard {...props} icon="group" />
);

export const DirectMessageList = ({
  rooms,
  currentUser,
  usersById,
  selectedRoomId,
  onSelect,
}: {
  rooms: ChatRoom[];
  currentUser: AuthUser;
  usersById: Record<string, ChatUser>;
  selectedRoomId: string | null;
  onSelect: (roomId: string) => void;
}) => (
  <ChatListSection title="Direct messages" count={rooms.length}>
    {rooms.map((room) => (
      <DirectMessageCard
        key={room.id}
        room={room}
        active={room.id === selectedRoomId}
        currentUser={currentUser}
        usersById={usersById}
        onSelect={() => onSelect(room.id)}
      />
    ))}
  </ChatListSection>
);

export const GroupChatList = ({
  rooms,
  currentUser,
  usersById,
  selectedRoomId,
  onSelect,
}: {
  rooms: ChatRoom[];
  currentUser: AuthUser;
  usersById: Record<string, ChatUser>;
  selectedRoomId: string | null;
  onSelect: (roomId: string) => void;
}) => (
  <ChatListSection title="Groups" count={rooms.length}>
    {rooms.map((room) => (
      <GroupChatCard
        key={room.id}
        room={room}
        active={room.id === selectedRoomId}
        currentUser={currentUser}
        usersById={usersById}
        onSelect={() => onSelect(room.id)}
      />
    ))}
  </ChatListSection>
);

const ChatListSection = ({
  title,
  count,
  children,
}: {
  title: string;
  count: number;
  children: ReactNode;
}) => (
  <section className="space-y-2">
    <div className="flex items-center justify-between px-1">
      <h2 className="text-xs font-bold uppercase tracking-[0.16em] text-muted-foreground">
        {title}
      </h2>
      <span className="text-xs text-muted-foreground">{count}</span>
    </div>
    <div className="space-y-1.5">{children}</div>
  </section>
);

export const NavUser = ({ user }: { user: AuthUser }) => {
  const signout = useAuthStore((state) => state.signout);

  return (
    <div className="flex items-center gap-3 rounded-lg border border-border bg-card p-3">
      <UserAvatar name={user.fullName} src={user.avatarUrl} online={user.isOnline} />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-semibold">{user.fullName}</p>
        <p className="truncate text-xs text-muted-foreground">@{user.username}</p>
      </div>
      <Button
        variant="ghost"
        size="icon"
        className="h-9 w-9 rounded-lg"
        onClick={() => void signout()}
        aria-label="Sign out"
        title="Sign out"
      >
        <LogOut className="h-4 w-4" />
      </Button>
    </div>
  );
};

export const AppSidebar = ({ user }: { user: AuthUser }) => {
  const { theme, toggleTheme } = useTheme();
  const rooms = useChatStore((state) => state.rooms);
  const usersById = useChatStore((state) => state.usersById);
  const selectedRoomId = useChatStore((state) => state.selectedRoomId);
  const setSelectedRoomId = useChatStore((state) => state.setSelectedRoomId);
  const isLoadingRooms = useChatStore((state) => state.isLoadingRooms);
  const [query, setQuery] = useState("");

  const filteredRooms = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    if (!normalizedQuery) {
      return rooms;
    }

    return rooms.filter((room) =>
      getRoomName(room, user, usersById).toLowerCase().includes(normalizedQuery),
    );
  }, [query, rooms, user, usersById]);

  const directRooms = filteredRooms.filter(isDirectRoom);
  const groupRooms = filteredRooms.filter((room) => !isDirectRoom(room));

  return (
    <aside className="flex h-full min-h-0 flex-col border-r border-border bg-card/85">
      <div className="space-y-4 border-b border-border p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="grid h-10 w-10 place-items-center rounded-lg bg-primary text-primary-foreground">
              <MessageCircleMore className="h-5 w-5" />
            </div>
            <div>
              <p className="text-base font-bold">InChat</p>
              <p className="text-xs text-muted-foreground">Realtime workspace</p>
            </div>
          </div>
          <Button
            variant="outline"
            size="icon"
            className="h-9 w-9 rounded-lg"
            onClick={toggleTheme}
            aria-label="Toggle theme"
            title="Toggle theme"
          >
            {theme === "dark" ? (
              <SunMedium className="h-4 w-4" />
            ) : (
              <MoonStar className="h-4 w-4" />
            )}
          </Button>
        </div>

        <label className="relative block">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            className="h-10 w-full rounded-lg border border-input bg-background pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20"
            placeholder="Search chats"
          />
        </label>
      </div>

      <div className="pretty-scrollbar min-h-0 flex-1 space-y-5 overflow-y-auto p-3">
        {isLoadingRooms ? (
          <div className="rounded-lg border border-border bg-background p-4 text-sm text-muted-foreground">
            Dang tai cuoc tro chuyen...
          </div>
        ) : (
          <>
            <DirectMessageList
              rooms={directRooms}
              currentUser={user}
              usersById={usersById}
              selectedRoomId={selectedRoomId}
              onSelect={setSelectedRoomId}
            />
            <GroupChatList
              rooms={groupRooms}
              currentUser={user}
              usersById={usersById}
              selectedRoomId={selectedRoomId}
              onSelect={setSelectedRoomId}
            />
          </>
        )}
      </div>

      <div className="border-t border-border p-3">
        <NavUser user={user} />
      </div>
    </aside>
  );
};

export const ChatWindowLayout = ({ user }: { user: AuthUser }) => {
  const rooms = useChatStore((state) => state.rooms);
  const usersById = useChatStore((state) => state.usersById);
  const selectedRoomId = useChatStore((state) => state.selectedRoomId);
  const messagesByRoomId = useChatStore((state) => state.messagesByRoomId);
  const isLoadingMessages = useChatStore((state) => state.isLoadingMessages);

  const selectedRoom = rooms.find((room) => room.id === selectedRoomId) ?? null;
  const messages = selectedRoomId ? messagesByRoomId[selectedRoomId] ?? [] : [];

  if (!selectedRoom) {
    return <WelcomeScreen />;
  }

  return (
    <section className="flex h-full min-h-0 flex-col bg-background">
      <ChatWindowHeader room={selectedRoom} user={user} usersById={usersById} />
      <ChatWindowBody
        messages={messages}
        currentUser={user}
        usersById={usersById}
        loading={isLoadingMessages}
      />
      <MessageInput />
    </section>
  );
};

const WelcomeScreen = () => (
  <section className="grid h-full place-items-center bg-background p-6">
    <div className="max-w-md text-center">
      <div className="mx-auto grid h-14 w-14 place-items-center rounded-lg bg-accent text-accent-foreground">
        <Sparkles className="h-7 w-7" />
      </div>
      <h1 className="mt-5 text-2xl font-bold tracking-tight">
        Chon mot cuoc tro chuyen
      </h1>
      <p className="mt-2 text-sm leading-6 text-muted-foreground">
        Tin nhan, trang thai online va so unread se cap nhat realtime khi ban
        bat dau mo mot chat.
      </p>
    </div>
  </section>
);

export const ChatWindowHeader = ({
  room,
  user,
  usersById,
}: {
  room: ChatRoom;
  user: AuthUser;
  usersById: Record<string, ChatUser>;
}) => {
  const name = getRoomName(room, user, usersById);
  const online = getRoomOnline(room, user, usersById);
  const peer = getRoomPeer(room, user.id, usersById);

  return (
    <header className="flex items-center gap-3 border-b border-border bg-card/80 px-4 py-3 backdrop-blur">
      <UserAvatar
        name={name}
        src={getRoomAvatar(room, user, usersById)}
        online={online}
        size="lg"
      />
      <div className="min-w-0 flex-1">
        <h1 className="truncate text-base font-bold">{name}</h1>
        <div className="mt-1 flex items-center gap-2">
          <StatusBadge online={online} />
          {!online && peer?.lastSeenAt ? (
            <span className="text-xs text-muted-foreground">
              Last seen {formatRelativeTime(peer.lastSeenAt)}
            </span>
          ) : null}
        </div>
      </div>
    </header>
  );
};

export const ChatWindowBody = ({
  messages,
  currentUser,
  usersById,
  loading,
}: {
  messages: ChatMessage[];
  currentUser: AuthUser;
  usersById: Record<string, ChatUser>;
  loading: boolean;
}) => {
  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length]);

  return (
    <div className="pretty-scrollbar min-h-0 flex-1 overflow-y-auto px-4 py-5">
      {loading ? (
        <p className="rounded-lg border border-border bg-card p-4 text-sm text-muted-foreground">
          Dang tai tin nhan...
        </p>
      ) : messages.length === 0 ? (
        <p className="mx-auto max-w-sm rounded-lg border border-border bg-card p-4 text-center text-sm text-muted-foreground">
          Chua co tin nhan nao. Hay gui loi chao dau tien.
        </p>
      ) : (
        <div className="space-y-3">
          {messages.map((message) => (
            <MessageItem
              key={message.id}
              message={message}
              mine={message.senderId === currentUser.id}
              sender={usersById[message.senderId]}
            />
          ))}
        </div>
      )}
      <div ref={bottomRef} />
    </div>
  );
};

export const MessageItem = ({
  message,
  mine,
  sender,
}: {
  message: ChatMessage;
  mine: boolean;
  sender?: ChatUser;
}) => {
  const senderName = sender?.displayName || sender?.username || "User";

  return (
    <div className={cn("flex gap-2", mine && "justify-end")}>
      {!mine ? <UserAvatar name={senderName} src={sender?.avatar} size="sm" /> : null}
      <div className={cn("max-w-[78%]", mine && "text-right")}>
        {!mine ? (
          <p className="mb-1 px-1 text-xs font-medium text-muted-foreground">
            {senderName}
          </p>
        ) : null}
        <div
          className={cn(
            "rounded-lg px-4 py-2 text-sm leading-6 shadow-sm",
            mine
              ? "bg-primary text-primary-foreground"
              : "border border-border bg-card text-card-foreground",
          )}
        >
          {message.content}
        </div>
        <p className="mt-1 px-1 text-xs text-muted-foreground">
          {new Date(message.timestamp).toLocaleTimeString(undefined, {
            hour: "2-digit",
            minute: "2-digit",
          })}
          {mine ? ` · ${message.status.toLowerCase()}` : ""}
        </p>
      </div>
    </div>
  );
};

export const MessageInput = () => {
  const sendMessage = useChatStore((state) => state.sendMessage);
  const isSending = useChatStore((state) => state.isSending);
  const [value, setValue] = useState("");
  const [showEmoji, setShowEmoji] = useState(false);

  const sendCurrentValue = () => {
    const content = value.trim();

    if (!content) {
      return;
    }

    setValue("");
    setShowEmoji(false);
    void sendMessage(content);
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    sendCurrentValue();
  };

  return (
    <form
      onSubmit={submit}
      className="relative border-t border-border bg-card/80 p-3 backdrop-blur"
    >
      {showEmoji ? (
        <div className="absolute bottom-[76px] left-3 grid w-64 grid-cols-8 gap-1 rounded-lg border border-border bg-card p-2 shadow-soft">
          {emojiItems.map((emoji) => (
            <button
              key={emoji}
              type="button"
              className="grid h-8 w-8 place-items-center rounded-md text-lg transition hover:bg-muted"
              onClick={() => setValue((current) => `${current}${emoji}`)}
            >
              {emoji}
            </button>
          ))}
        </div>
      ) : null}

      <div className="flex items-end gap-2 rounded-lg border border-input bg-background p-2">
        <Button
          variant="ghost"
          size="icon"
          className="h-10 w-10 rounded-lg"
          onClick={() => setShowEmoji((current) => !current)}
          aria-label="Open emoji picker"
          title="Emoji"
        >
          <SmilePlus className="h-5 w-5" />
        </Button>
        <textarea
          value={value}
          onChange={(event) => setValue(event.target.value)}
          onKeyDown={(event: KeyboardEvent<HTMLTextAreaElement>) => {
            if (event.key === "Enter" && !event.shiftKey) {
              event.preventDefault();
              sendCurrentValue();
            }
          }}
          rows={1}
          className="max-h-32 min-h-10 flex-1 resize-none bg-transparent px-1 py-2 text-sm leading-6 outline-none"
          placeholder="Nhap tin nhan..."
        />
        <Button
          type="submit"
          size="icon"
          className="h-10 w-10 rounded-lg"
          disabled={isSending || !value.trim()}
          aria-label="Send message"
          title="Send"
        >
          <SendHorizontal className="h-5 w-5" />
        </Button>
      </div>
    </form>
  );
};
