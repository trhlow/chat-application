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
  signout: () => apiClient.post<void>("/auth/logout"),
  refresh: () => authClient.post<BackendAuthResponse>("/auth/refresh"),
  getMe: () => apiClient.get<BackendAuthUser>("/users/me"),
};
