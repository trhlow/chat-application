import { api, publicApi } from "@/lib/api"

import type {
  AuthResponse,
  LoginPayload,
  LogoutPayload,
  RefreshTokenPayload,
  RegisterPayload,
  UserProfile,
} from "@/types/auth"

export async function registerUser(payload: RegisterPayload) {
  const response = await publicApi.post<AuthResponse>("/auth/register", payload)
  return response.data
}

export async function loginUser(payload: LoginPayload) {
  const response = await publicApi.post<AuthResponse>("/auth/login", payload)
  return response.data
}

export async function refreshSession(payload: RefreshTokenPayload) {
  const response = await publicApi.post<AuthResponse>("/auth/refresh", payload)
  return response.data
}

export async function logoutUser(payload?: LogoutPayload) {
  await api.post("/auth/logout", payload)
}

export async function fetchCurrentUser() {
  const response = await api.get<UserProfile>("/users/me")
  return response.data
}
