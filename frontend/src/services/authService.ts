import type {
  BackendAuthResponse,
  BackendAuthUser,
  SignInRequestPayload,
  SignUpRequestPayload,
} from "@/types/auth";

import { apiClient, authClient } from "@/lib/axios";

export const authApi = {
  signup: (payload: SignUpRequestPayload) =>
    authClient.post<BackendAuthResponse>("/auth/register", payload),
  signin: (payload: SignInRequestPayload) =>
    authClient.post<BackendAuthResponse>("/auth/login", payload),
  signout: (refreshToken?: string | null) =>
    authClient.post<void>("/auth/logout", refreshToken ? { refreshToken } : {}),
  refresh: (refreshToken: string) =>
    authClient.post<BackendAuthResponse>("/auth/refresh", { refreshToken }),
  getMe: () => apiClient.get<BackendAuthUser>("/users/me"),
};
