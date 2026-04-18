import { Navigate, Outlet, useLocation } from "react-router"

import { AuthStatusScreen } from "@/components/auth-status-screen"
import { useAuthStore } from "@/store/auth-store"

export function PublicOnlyRoute() {
  const isBootstrapping = useAuthStore((state) => state.isBootstrapping)
  const status = useAuthStore((state) => state.status)
  const location = useLocation()

  const redirectTo =
    (
      location.state as
        | {
            from?: {
              pathname?: string
            }
          }
        | null
    )?.from?.pathname ?? "/"

  if (isBootstrapping) {
    return (
      <AuthStatusScreen
        title="Dang khoi tao session"
        description="Public route tam doi trong luc ung dung kiem tra token da luu."
      />
    )
  }

  if (status === "authenticated") {
    return <Navigate to={redirectTo} replace />
  }

  return <Outlet />
}
