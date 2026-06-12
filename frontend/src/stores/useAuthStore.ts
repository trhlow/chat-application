import { create } from "zustand";

import { authApi } from "@/services/authService";
import { authStorage } from "@/lib/auth-storage";
import { bindAuthSession } from "@/lib/axios";
import type {
  AuthUser,
  BackendAuthResponse,
  BackendAuthUser,
  SignInFormValues,
  SignUpFormValues,
} from "@/types/auth";

const mapAuthUser = (user: BackendAuthUser): AuthUser => ({
  id: user.id,
  fullName: user.displayName?.trim() || user.username,
  username: user.username,
  email: user.email,
  avatarUrl: user.avatarEndpoint ?? user.avatar,
  isOnline: user.online,
  lastSeenAt: user.lastSeenAt,
});

const applyAuthResponse = (
  response: BackendAuthResponse,
): { accessToken: string; user: AuthUser } => {
  authStorage.setRefreshToken(response.refreshToken);

  return {
    accessToken: response.accessToken,
    user: mapAuthUser(response.user),
  };
};

interface AuthState {
  accessToken: string | null;
  user: AuthUser | null;
  isBootstrapping: boolean;
  setAccessToken: (token: string | null) => void;
  setSession: (payload: { accessToken: string; user: AuthUser }) => void;
  updateUser: (payload: Partial<AuthUser>) => void;
  clearSession: () => void;
  signup: (values: SignUpFormValues) => Promise<void>;
  signin: (values: SignInFormValues) => Promise<void>;
  signout: () => Promise<void>;
  refreshAccessToken: () => Promise<string | null>;
  fetchCurrentUser: () => Promise<AuthUser>;
  bootstrap: () => Promise<void>;
}

let bootstrapPromise: Promise<void> | null = null;

export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  user: null,
  isBootstrapping: true,
  setAccessToken: (token) => {
    set({ accessToken: token });
  },
  setSession: ({ accessToken, user }) => {
    set({
      accessToken,
      user,
    });
  },
  updateUser: (payload) => {
    set((state) => ({
      user: state.user ? { ...state.user, ...payload } : state.user,
    }));
  },
  clearSession: () => {
    authStorage.clear();
    set({
      accessToken: null,
      user: null,
    });
  },
  signup: async (values) => {
    const response = await authApi.signup({
      displayName: `${values.firstName.trim()} ${values.lastName.trim()}`.trim(),
      username: values.username,
      email: values.email,
      password: values.password,
      avatar: null,
    });

    get().setSession(applyAuthResponse(response.data));
  },
  signin: async (values) => {
    const identity = values.emailOrUsername.trim();
    const response = await authApi.signin({
      ...(identity.includes("@")
        ? { email: identity.toLowerCase() }
        : { username: identity.toLowerCase() }),
      password: values.password,
    });

    get().setSession(applyAuthResponse(response.data));
  },
  signout: async () => {
    const refreshToken = authStorage.getRefreshToken();

    try {
      await authApi.signout(refreshToken);
    } finally {
      get().clearSession();
    }
  },
  refreshAccessToken: async () => {
    const refreshToken = authStorage.getRefreshToken();

    if (!refreshToken) {
      get().clearSession();
      return null;
    }

    try {
      const response = await authApi.refresh(refreshToken);
      get().setSession(applyAuthResponse(response.data));
      return response.data.accessToken;
    } catch (_error) {
      get().clearSession();
      return null;
    }
  },
  fetchCurrentUser: async () => {
    const response = await authApi.getMe();
    set((state) => ({
      user: mapAuthUser(response.data),
      accessToken: state.accessToken,
    }));
    return mapAuthUser(response.data);
  },
  bootstrap: async () => {
    if (bootstrapPromise) {
      return bootstrapPromise;
    }

    bootstrapPromise = (async () => {
      set({ isBootstrapping: true });

      try {
        if (!get().accessToken) {
          const refreshedAccessToken = await get().refreshAccessToken();

          if (!refreshedAccessToken) {
            return;
          }
        }

        try {
          await get().fetchCurrentUser();
        } catch (_error) {
          // The refresh response already contains a usable user snapshot.
          // Keep the session on transient profile/network failures.
        }
      } finally {
        set({ isBootstrapping: false });
        bootstrapPromise = null;
      }
    })();

    return bootstrapPromise;
  },
}));

bindAuthSession({
  getAccessToken: () => useAuthStore.getState().accessToken,
  setAccessToken: (token) => useAuthStore.getState().setAccessToken(token),
  clearSession: () => useAuthStore.getState().clearSession(),
  refreshAccessToken: () => useAuthStore.getState().refreshAccessToken(),
});
