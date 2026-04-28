import type { LabelHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export const Label = ({
  className,
  ...props
}: LabelHTMLAttributes<HTMLLabelElement>) => (
  <label
    className={cn("text-sm font-medium text-foreground", className)}
    {...props}
  />
);
