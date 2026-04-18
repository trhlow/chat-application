import { Navigate, Outlet, useLocation } from "react-router"

import { AuthStatusScreen } from "@/components/auth-status-screen"
import { useAuthStore } from "@/store/auth-store"

export function ProtectedRoute() {
  const isBootstrapping = useAuthStore((state) => state.isBootstrapping)
  const status = useAuthStore((state) => state.status)
  const location = useLocation()

  if (isBootstrapping || status === "loading") {
    return (
      <AuthStatusScreen
        title="Dang xac thuc nguoi dung"
        description="Protected route chi duoc render sau khi xac nhan session hop le."
      />
    )
  }

  if (status !== "authenticated") {
    return <Navigate to="/signin" replace state={{ from: location }} />
  }

  return <Outlet />
}
