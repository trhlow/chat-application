import { create } from "zustand"
import { createJSONStorage, persist } from "zustand/middleware"

import {
  fetchCurrentUser,
  loginUser,
  logoutUser,
  refreshSession,
  registerUser,
} from "@/services/auth-api"

import type {
  AuthResponse,
  LoginPayload,
  RegisterPayload,
  UserProfile,
} from "@/types/auth"

type AuthStatus = "idle" | "loading" | "authenticated" | "unauthenticated"

interface AuthStore {
  accessToken: string | null
  refreshToken: string | null
  user: UserProfile | null
  status: AuthStatus
  isBootstrapping: boolean
  initialize: () => Promise<void>
  signIn: (payload: LoginPayload) => Promise<void>
  signUp: (payload: RegisterPayload) => Promise<void>
  signOut: () => Promise<void>
  refreshAccessToken: () => Promise<string | null>
  fetchCurrentUser: () => Promise<UserProfile>
  clearSession: () => void
}

const AUTH_STORAGE_KEY = "chat-application.auth"

const emptySession = {
  accessToken: null,
  refreshToken: null,
  user: null,
}

let initializePromise: Promise<void> | null = null

function toAuthenticatedState(payload: AuthResponse) {
  return {
    accessToken: payload.accessToken,
    refreshToken: payload.refreshToken,
    user: payload.user,
    status: "authenticated" as const,
  }
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      ...emptySession,
      status: "idle",
      isBootstrapping: true,

      clearSession: () => {
        set({
          ...emptySession,
          status: "unauthenticated",
          isBootstrapping: false,
        })
      },

      signIn: async (payload) => {
        set({ status: "loading" })

        try {
          const response = await loginUser(payload)
          set(toAuthenticatedState(response))
        } catch (error) {
          set({ status: "unauthenticated" })
          throw error
        }
      },

      signUp: async (payload) => {
        set({ status: "loading" })

        try {
          const response = await registerUser(payload)
          set(toAuthenticatedState(response))
        } catch (error) {
          set({ status: "unauthenticated" })
          throw error
        }
      },

      signOut: async () => {
        const { accessToken, refreshToken } = get()

        try {
          if (accessToken) {
            await logoutUser(refreshToken ? { refreshToken } : undefined)
          }
        } catch {
          // Local session must still be cleared even if the backend rejects logout.
        }

        get().clearSession()
      },

      refreshAccessToken: async () => {
        const currentRefreshToken = get().refreshToken

        if (!currentRefreshToken) {
          get().clearSession()
          return null
        }

        try {
          const response = await refreshSession({
            refreshToken: currentRefreshToken,
          })

          set(toAuthenticatedState(response))
          return response.accessToken
        } catch (error) {
          get().clearSession()
          throw error
        }
      },

      fetchCurrentUser: async () => {
        const user = await fetchCurrentUser()
        set({
          user,
          status: "authenticated",
        })
        return user
      },

      initialize: async () => {
        if (initializePromise) {
          return initializePromise
        }

        initializePromise = (async () => {
          const { accessToken, refreshToken } = get()

          if (!accessToken && !refreshToken) {
            set({
              ...emptySession,
              status: "unauthenticated",
              isBootstrapping: false,
            })
            return
          }

          set({ isBootstrapping: true })

          try {
            if (!accessToken && refreshToken) {
              await get().refreshAccessToken()
            }

            await get().fetchCurrentUser()
          } catch {
            get().clearSession()
          } finally {
            set({ isBootstrapping: false })
          }
        })().finally(() => {
          initializePromise = null
        })

        return initializePromise
      },
    }),
    {
      name: AUTH_STORAGE_KEY,
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    }
  )
)
