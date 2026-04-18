import { useEffect } from "react"

import { useAuthStore } from "@/store/auth-store"

import type { ReactNode } from "react"

interface AuthBootstrapProps {
  children: ReactNode
}

export function AuthBootstrap({ children }: AuthBootstrapProps) {
  const initialize = useAuthStore((state) => state.initialize)

  useEffect(() => {
    void initialize()
  }, [initialize])

  return <>{children}</>
}
