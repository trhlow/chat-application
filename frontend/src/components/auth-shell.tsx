import { CheckCircle2, MessageCircleMore, ShieldCheck, Zap } from "lucide-react";
import type { PropsWithChildren, ReactNode } from "react";

import { ThemeToggle } from "@/components/theme-toggle";

interface AuthShellProps extends PropsWithChildren {
  mode: "signin" | "signup";
  title: string;
  description: string;
  footer: ReactNode;
}

const benefits = [
  { icon: Zap, text: "Tin nhắn và trạng thái được cập nhật theo thời gian thực" },
  { icon: ShieldCheck, text: "Không gian trò chuyện riêng tư cho bạn và bạn bè" },
  { icon: CheckCircle2, text: "Quản lý hội thoại, nhóm và lời mời tại một nơi" },
];

export const AuthShell = ({
  mode,
  title,
  description,
  footer,
  children,
}: AuthShellProps) => (
  <main className="relative min-h-[100dvh] overflow-hidden bg-background">
    <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_18%_12%,hsl(var(--primary)/0.10),transparent_30%),radial-gradient(circle_at_84%_82%,hsl(var(--primary)/0.07),transparent_28%)]" />
    <div className="absolute right-5 top-5 z-20">
      <ThemeToggle />
    </div>

    <div className="relative mx-auto grid min-h-[100dvh] max-w-7xl lg:grid-cols-[1.05fr_0.95fr]">
      <section className="hidden border-r border-border px-12 py-10 lg:flex lg:flex-col lg:justify-between xl:px-20">
        <div className="flex items-center gap-3">
          <span className="grid h-11 w-11 place-items-center rounded-xl bg-primary text-primary-foreground shadow-sm">
            <MessageCircleMore className="h-5 w-5" />
          </span>
          <span>
            <strong className="block text-lg tracking-tight">InChat</strong>
            <span className="text-xs text-muted-foreground">Gần nhau hơn, mỗi ngày</span>
          </span>
        </div>

        <div className="max-w-xl pb-8">
          <p className="mb-5 text-sm font-semibold text-primary">
            {mode === "signin" ? "Chào mừng bạn trở lại" : "Bắt đầu một cuộc trò chuyện mới"}
          </p>
          <h2 className="max-w-lg text-4xl font-semibold leading-[1.12] tracking-[-0.035em] xl:text-5xl">
            Kết nối tự nhiên, trò chuyện không gián đoạn.
          </h2>
          <p className="mt-5 max-w-lg text-base leading-7 text-muted-foreground">
            Một không gian tập trung để nhắn tin, theo dõi bạn bè và giữ liên lạc với những người quan trọng.
          </p>

          <div className="mt-10 space-y-5">
            {benefits.map(({ icon: Icon, text }) => (
              <div key={text} className="flex items-center gap-3 text-sm text-muted-foreground">
                <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-accent text-accent-foreground">
                  <Icon className="h-4 w-4" />
                </span>
                <span>{text}</span>
              </div>
            ))}
          </div>
        </div>

        <p className="text-xs text-muted-foreground">InChat · Realtime messaging</p>
      </section>

      <section className="flex min-h-[100dvh] items-center justify-center px-5 py-16 sm:px-8">
        <div className="animate-rise-in w-full max-w-md">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <span className="grid h-10 w-10 place-items-center rounded-xl bg-primary text-primary-foreground">
              <MessageCircleMore className="h-5 w-5" />
            </span>
            <strong className="text-lg">InChat</strong>
          </div>

          <div className="rounded-2xl border border-border bg-card p-6 shadow-soft sm:p-8">
            <header className="mb-7">
              <h1 className="text-3xl font-semibold tracking-[-0.03em]">{title}</h1>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">{description}</p>
            </header>
            {children}
          </div>

          <p className="px-4 pt-5 text-center text-xs leading-5 text-muted-foreground">
            {footer}
          </p>
        </div>
      </section>
    </div>
  </main>
);
