import { MessageCircleMore, RefreshCw, ShieldCheck, UserRound } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ThemeToggle } from "@/components/theme-toggle";
import { useAuthStore } from "@/store/auth-store";

export const ChatPage = () => {
  const user = useAuthStore((state) => state.user);
  const fetchCurrentUser = useAuthStore((state) => state.fetchCurrentUser);
  const refreshAccessToken = useAuthStore((state) => state.refreshAccessToken);
  const signout = useAuthStore((state) => state.signout);

  if (!user) {
    return null;
  }

  return (
    <main className="min-h-screen bg-background px-5 py-6 text-foreground sm:px-8">
      <div className="mx-auto max-w-6xl space-y-6">
        <header className="flex flex-col gap-4 rounded-[32px] border border-border/70 bg-card/90 p-6 shadow-soft backdrop-blur sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-primary">
              <ShieldCheck className="h-3.5 w-3.5" />
              Protected route
            </div>
            <h1 className="text-3xl font-semibold tracking-tight">
              Welcome, {user.fullName}
            </h1>
            <p className="text-sm leading-6 text-muted-foreground">
              Your chat shell is protected by JWT auth, Zustand session state,
              and a refresh-token flow backed by the Spring Boot API.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <ThemeToggle />
            <Button variant="outline" onClick={() => void signout()}>
              Sign out
            </Button>
          </div>
        </header>

        <section className="grid gap-6 lg:grid-cols-[1.15fr_0.85fr]">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <MessageCircleMore className="h-5 w-5 text-primary" />
                Session overview
              </CardTitle>
              <CardDescription>
                The frontend restores the session by calling refresh first, then
                fetching `/users/me` with the fresh access token automatically
                attached in the Authorization header.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="rounded-[24px] border border-border/70 bg-background/60 p-4">
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-muted-foreground">
                    Email
                  </p>
                  <p className="mt-3 text-base font-medium">{user.email}</p>
                </div>
                <div className="rounded-[24px] border border-border/70 bg-background/60 p-4">
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-muted-foreground">
                    Username
                  </p>
                  <p className="mt-3 text-base font-medium">@{user.username}</p>
                </div>
              </div>

              <div className="rounded-[24px] border border-border/70 bg-background/60 p-4">
                <p className="text-xs font-semibold uppercase tracking-[0.22em] text-muted-foreground">
                  Why this route is protected
                </p>
                <ul className="mt-4 space-y-3 text-sm leading-6 text-muted-foreground">
                  <li>Only authenticated users can reach `/app`.</li>
                  <li>401 responses trigger refresh and request replay automatically.</li>
                  <li>Reloading the page rehydrates the session from the stored refresh token.</li>
                </ul>
              </div>

              <div className="flex flex-wrap gap-3">
                <Button
                  className="gap-2"
                  onClick={() => void fetchCurrentUser()}
                >
                  <UserRound className="h-4 w-4" />
                  Fetch current user
                </Button>
                <Button
                  variant="outline"
                  className="gap-2"
                  onClick={() => void refreshAccessToken()}
                >
                  <RefreshCw className="h-4 w-4" />
                  Refresh access token
                </Button>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Refresh flow checklist</CardTitle>
              <CardDescription>
                This mirrors the auth tasks you requested for the frontend.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4 text-sm leading-6 text-muted-foreground">
              <div className="rounded-[24px] border border-border/70 bg-background/60 p-4">
                `Zod + react-hook-form` validate signin and signup payloads before
                they ever leave the browser.
              </div>
              <div className="rounded-[24px] border border-border/70 bg-background/60 p-4">
                Zustand stores the access token in memory and clears it safely on
                signout or failed refresh.
              </div>
              <div className="rounded-[24px] border border-border/70 bg-background/60 p-4">
                Axios interceptor injects `Authorization: Bearer ...` for
                protected endpoints and retries once after refresh.
              </div>
            </CardContent>
          </Card>
        </section>
      </div>
    </main>
  );
};
