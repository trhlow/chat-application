import type { InputHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export const Input = ({
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement>) => (
  <input
    className={cn(
      "flex h-12 w-full rounded-2xl border border-border bg-background/85 px-4 py-3 text-sm text-foreground outline-none transition placeholder:text-muted-foreground focus:border-primary/60 focus:ring-2 focus:ring-primary/20",
      className,
    )}
    {...props}
  />
);
