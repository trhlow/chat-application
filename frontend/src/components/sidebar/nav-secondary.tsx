import type { PropsWithChildren } from "react";

export const NavSecondary = ({ children }: PropsWithChildren) => (
  <nav className="mt-auto space-y-1">{children}</nav>
);
