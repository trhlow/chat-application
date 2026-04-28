const REFRESH_TOKEN_STORAGE_KEY = "inchat.refreshToken";

const canUseStorage = () => typeof window !== "undefined";

export const authStorage = {
  getRefreshToken: () => {
    if (!canUseStorage()) {
      return null;
    }

    return window.localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
  },
  setRefreshToken: (refreshToken: string | null) => {
    if (!canUseStorage()) {
      return;
    }

    if (!refreshToken) {
      window.localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      return;
    }

    window.localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
  },
  clear: () => {
    if (!canUseStorage()) {
      return;
    }

    window.localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
  },
};
