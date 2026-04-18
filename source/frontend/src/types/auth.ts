export interface UserProfile {
  id: string
  username: string
  email: string
  displayName: string | null
  bio: string | null
  phone: string | null
  themePreference: string | null
  avatar: string | null
  avatarProvider: string | null
  online: boolean
  lastSeenAt: string | null
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  accessExpiresInMs: number
  refreshExpiresInMs: number
  user: UserProfile
}

export interface ApiErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors?: Record<string, string> | null
}

export interface RegisterPayload {
  username: string
  email: string
  password: string
  avatar?: string
}

export interface LoginPayload {
  username?: string
  email?: string
  password: string
}

export interface RefreshTokenPayload {
  refreshToken: string
}

export interface LogoutPayload {
  refreshToken?: string | null
}
