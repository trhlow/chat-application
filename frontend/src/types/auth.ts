export interface AuthUser {
  id: string;
  fullName: string;
  username: string;
  email: string;
  avatarUrl: string | null;
  isOnline: boolean;
  lastSeenAt: string | null;
}

export interface BackendAuthUser {
  id: string;
  username: string;
  email: string;
  displayName: string | null;
  bio: string | null;
  phone: string | null;
  themePreference: string | null;
  avatarEndpoint?: string | null;
  avatar: string | null;
  avatarProvider: string | null;
  online: boolean;
  lastSeenAt: string | null;
}

export interface BackendAuthResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  accessExpiresInMs: number;
  refreshExpiresInMs: number;
  user: BackendAuthUser;
}

export interface SignUpFormValues {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
}

export interface SignUpRequestPayload {
  displayName: string;
  username: string;
  email: string;
  password: string;
  avatar?: string | null;
}

export interface SignInRequestPayload {
  username?: string;
  email?: string;
  password: string;
}

export interface SignInFormValues {
  emailOrUsername: string;
  password: string;
}
