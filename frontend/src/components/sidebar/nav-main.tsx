import type { PropsWithChildren } from "react";

export const NavMain = ({ children }: PropsWithChildren) => (
  <nav className="space-y-1">{children}</nav>
);
