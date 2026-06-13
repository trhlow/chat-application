import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";

import { API_URL } from "./config";

interface SessionBindings {
  getAccessToken: () => string | null;
  setAccessToken: (token: string | null) => void;
  clearSession: () => void;
  refreshAccessToken: () => Promise<string | null>;
}

const skippedRefreshUrls = [
  "/auth/register",
  "/auth/login",
  "/auth/logout",
  "/auth/refresh",
];

let sessionBindings: SessionBindings = {
  getAccessToken: () => null,
  setAccessToken: () => undefined,
  clearSession: () => undefined,
  refreshAccessToken: async () => null,
};

let refreshPromise: Promise<string | null> | null = null;

export const apiClient = axios.create({
  baseURL: API_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

export const authClient = axios.create({
  baseURL: API_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

const shouldSkipRefresh = (url?: string) =>
  skippedRefreshUrls.some((endpoint) => url?.includes(endpoint));

apiClient.interceptors.request.use((config) => {
  const accessToken = sessionBindings.getAccessToken();

  if (accessToken) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined;

    if (
      !originalRequest ||
      error.response?.status !== 401 ||
      originalRequest._retry ||
      shouldSkipRefresh(originalRequest.url)
    ) {
      throw error;
    }

    originalRequest._retry = true;

    try {
      if (!refreshPromise) {
        refreshPromise = sessionBindings
          .refreshAccessToken()
          .finally(() => {
            refreshPromise = null;
          });
      }

      const newAccessToken = await refreshPromise;

      if (!newAccessToken) {
        sessionBindings.clearSession();
        throw error;
      }

      originalRequest.headers = originalRequest.headers ?? {};
      originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

      return apiClient(originalRequest);
    } catch (refreshError) {
      sessionBindings.clearSession();
      throw refreshError;
    }
  },
);

export const bindAuthSession = (bindings: Partial<SessionBindings>) => {
  sessionBindings = {
    ...sessionBindings,
    ...bindings,
  };
};
