import { useEffect } from "react";

import { AppSidebar, ChatWindowLayout } from "@/components/chat/chat-shell";
import { useAuthStore } from "@/store/auth-store";
import { useChatStore } from "@/store/chat-store";

export const ChatPage = () => {
  const user = useAuthStore((state) => state.user);
  const accessToken = useAuthStore((state) => state.accessToken);
  const fetchConversations = useChatStore((state) => state.fetchConversations);
  const fetchFriends = useChatStore((state) => state.fetchFriends);
  const fetchProfile = useChatStore((state) => state.fetchProfile);
  const connectRealtime = useChatStore((state) => state.connectRealtime);
  const disconnectRealtime = useChatStore((state) => state.disconnectRealtime);

  useEffect(() => {
    void fetchConversations();
    void fetchFriends();
    void fetchProfile();
  }, [fetchConversations, fetchFriends, fetchProfile]);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    connectRealtime(accessToken);

    return () => {
      disconnectRealtime();
    };
  }, [accessToken, connectRealtime, disconnectRealtime]);

  if (!user) {
    return null;
  }

  return (
    <main className="h-screen overflow-hidden bg-background text-foreground">
      <div className="grid h-full grid-cols-1 md:grid-cols-[340px_minmax(0,1fr)]">
        <div className="hidden min-h-0 md:block">
          <AppSidebar user={user} />
        </div>
        <div className="grid min-h-0 grid-rows-[minmax(260px,42vh)_minmax(0,1fr)] md:block">
          <div className="min-h-0 border-b border-border md:hidden">
            <AppSidebar user={user} />
          </div>
          <ChatWindowLayout user={user} />
        </div>
      </div>
    </main>
  );
};
