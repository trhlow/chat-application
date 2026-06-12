const REFRESH_TOKEN_STORAGE_KEY = "inchat.refreshToken";
const LEGACY_STORAGE_KEY = REFRESH_TOKEN_STORAGE_KEY;

const canUseStorage = () => typeof window !== "undefined";

export const authStorage = {
  getRefreshToken: () => {
    if (!canUseStorage()) {
      return null;
    }

    try {
      const token = window.sessionStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
      window.localStorage.removeItem(LEGACY_STORAGE_KEY);
      return token;
    } catch (_error) {
      return null;
    }
  },
  setRefreshToken: (refreshToken: string | null) => {
    if (!canUseStorage()) {
      return;
    }

    try {
      window.localStorage.removeItem(LEGACY_STORAGE_KEY);

      if (!refreshToken) {
        window.sessionStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
        return;
      }

      window.sessionStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
    } catch (_error) {
      // Storage can be unavailable in hardened/private browser contexts.
    }
  },
  clear: () => {
    if (!canUseStorage()) {
      return;
    }

    try {
      window.sessionStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      window.localStorage.removeItem(LEGACY_STORAGE_KEY);
    } catch (_error) {
      // Clearing an unavailable storage area is a no-op.
    }
  },
};
