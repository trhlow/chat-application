import type { PropsWithChildren } from "react";

import { Navigate } from "react-router-dom";

import { FullScreenLoader } from "@/components/full-screen-loader";
import { useAuthStore } from "@/store/auth-store";

export const ProtectedRoute = ({ children }: PropsWithChildren) => {
  const isBootstrapping = useAuthStore((state) => state.isBootstrapping);
  const user = useAuthStore((state) => state.user);

  if (isBootstrapping) {
    return <FullScreenLoader />;
  }

  if (!user) {
    return <Navigate to="/signin" replace />;
  }

  return children;
};
