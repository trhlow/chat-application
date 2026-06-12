import type { FriendUser } from "@/types/chat";

import { UserAvatar } from "@/components/chat/UserAvatar";

interface IniviteSuggestionListProps {
  users: FriendUser[];
  selectedIds: string[];
  onToggle: (userId: string) => void;
}

export const IniviteSuggestionList = ({
  users,
  selectedIds,
  onToggle,
}: IniviteSuggestionListProps) => (
  <div className="space-y-2">
    {users.map((user) => (
      <button
        key={user.id}
        type="button"
        className="flex w-full items-center gap-3 rounded-lg border border-border p-3 text-left hover:bg-muted"
        onClick={() => onToggle(user.id)}
      >
        <input type="checkbox" readOnly checked={selectedIds.includes(user.id)} />
        <UserAvatar
          name={user.displayName || user.username}
          src={user.avatarEndpoint ?? user.avatar}
        />
        <span className="min-w-0 truncate text-sm font-semibold">
          {user.displayName || user.username}
        </span>
      </button>
    ))}
  </div>
);
