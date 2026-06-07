import { MessageCircleMore } from "lucide-react";
import type { PropsWithChildren, ReactNode } from "react";

interface AuthShellProps extends PropsWithChildren {
  mode: "signin" | "signup";
  title: string;
  description: string;
  footer: ReactNode;
}

export const AuthShell = ({
  title,
  description,
  footer,
  children,
}: AuthShellProps) => (
  <main className="min-h-[100dvh] bg-[#eaf4ff] px-4 py-10 text-slate-900 dark:bg-background dark:text-foreground sm:py-14">
    <div className="mx-auto w-full max-w-[420px]">
      <header className="mb-7 text-center">
        <div className="mx-auto flex items-center justify-center gap-2 text-primary">
          <MessageCircleMore className="h-9 w-9" strokeWidth={2.4} />
          <span className="text-4xl font-bold tracking-[-0.06em]">InChat</span>
        </div>
        <p className="mx-auto mt-4 max-w-xs text-sm leading-6 text-slate-600 dark:text-muted-foreground">
          {description}
        </p>
      </header>

      <section className="animate-rise-in overflow-hidden rounded-xl bg-white shadow-[0_14px_38px_rgba(37,99,235,0.13)] dark:border dark:border-border dark:bg-card">
        <h1 className="border-b border-slate-100 px-6 py-5 text-center text-base font-semibold dark:border-border">
          {title}
        </h1>
        <div className="px-7 py-7 sm:px-9">{children}</div>
      </section>

      <p className="px-5 pt-6 text-center text-xs leading-5 text-slate-500 dark:text-muted-foreground">
        {footer}
      </p>
    </div>
  </main>
);
