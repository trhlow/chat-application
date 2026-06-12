import type { FriendUser } from "@/types/chat";

interface SelectedUsersListProps {
  users: FriendUser[];
  onRemove: (userId: string) => void;
}

export const SelectedUsersList = ({ users, onRemove }: SelectedUsersListProps) => (
  <div className="flex flex-wrap gap-2">
    {users.map((user) => (
      <button
        key={user.id}
        type="button"
        className="rounded-full bg-muted px-3 py-1.5 text-xs font-medium"
        onClick={() => onRemove(user.id)}
      >
        {user.displayName || user.username} ×
      </button>
    ))}
  </div>
);
