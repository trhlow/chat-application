import { useEffect, useState } from "react";

import { ChatWindowLayout } from "@/components/chat/ChatWindowLayout";
import { AppSidebar, FriendsWorkspace } from "@/components/sidebar/app-sidebar";
import { useAuthStore } from "@/stores/useAuthStore";
import { useChatStore } from "@/stores/useChatStore";

export const ChatAppPage = () => {
  const [activeView, setActiveView] = useState<"chats" | "friends">("chats");
  const [friendsSection, setFriendsSection] = useState<"list" | "groups" | "requests" | "groupRequests">("list");
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
    <main className="min-h-[100dvh] overflow-hidden bg-background p-0 text-foreground">
      <div className="app-frame mx-auto grid h-[100dvh] max-w-[1920px] grid-cols-1 overflow-hidden bg-card md:grid-cols-[350px_minmax(0,1fr)] xl:grid-cols-[370px_minmax(0,1fr)]">
        <div className="hidden min-h-0 md:block">
          <AppSidebar
            user={user}
            activeView={activeView}
            onViewChange={setActiveView}
            friendsSection={friendsSection}
            onFriendsSectionChange={setFriendsSection}
          />
        </div>
        <div className="grid min-h-0 grid-rows-[minmax(240px,40dvh)_minmax(0,1fr)] md:block">
          <div className="min-h-0 border-b border-border md:hidden">
            <AppSidebar
              user={user}
              activeView={activeView}
              onViewChange={setActiveView}
              friendsSection={friendsSection}
              onFriendsSectionChange={setFriendsSection}
            />
          </div>
          {activeView === "friends" ? (
            <FriendsWorkspace section={friendsSection} onOpenChat={() => setActiveView("chats")} />
          ) : (
            <ChatWindowLayout user={user} />
          )}
        </div>
      </div>
    </main>
  );
};
