import axios, {
  AxiosError,
  AxiosHeaders,
  type InternalAxiosRequestConfig,
} from "axios"

interface InterceptorHandlers {
  getAccessToken: () => string | null
  refreshAccessToken: () => Promise<string | null>
  onUnauthorized: () => void | Promise<void>
}

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
  skipAuthRefresh?: boolean
}

const DEFAULT_TIMEOUT_MS = 10000

function resolveApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

  if (configuredBaseUrl) {
    return configuredBaseUrl
  }

  if (typeof window === "undefined") {
    return "/api"
  }

  const { protocol, hostname, port, origin } = window.location
  const isLocalHost = hostname === "localhost" || hostname === "127.0.0.1"

  if (isLocalHost && port !== "8080") {
    return `${protocol}//${hostname}:8080/api`
  }

  return `${origin}/api`
}

const baseURL = resolveApiBaseUrl()

let requestInterceptorId: number | null = null
let responseInterceptorId: number | null = null
let refreshPromise: Promise<string | null> | null = null

export const publicApi = axios.create({
  baseURL,
  timeout: DEFAULT_TIMEOUT_MS,
  headers: {
    Accept: "application/json",
    "Content-Type": "application/json",
  },
})

export const api = axios.create({
  baseURL,
  timeout: DEFAULT_TIMEOUT_MS,
  headers: {
    Accept: "application/json",
    "Content-Type": "application/json",
  },
})

function hasAuthorizationHeader(headers: InternalAxiosRequestConfig["headers"]) {
  if (!headers) {
    return false
  }

  return AxiosHeaders.from(headers).has("Authorization")
}

function withAuthorizationHeader(
  headers: InternalAxiosRequestConfig["headers"],
  token: string
) {
  const authorizationValue = `Bearer ${token}`
  const nextHeaders = AxiosHeaders.from(headers ?? {})
  nextHeaders.set("Authorization", authorizationValue)
  return nextHeaders
}

function isRefreshRequest(url?: string) {
  return typeof url === "string" && url.includes("/auth/refresh")
}

export function configureApiInterceptors(handlers: InterceptorHandlers) {
  if (requestInterceptorId !== null) {
    api.interceptors.request.eject(requestInterceptorId)
  }

  if (responseInterceptorId !== null) {
    api.interceptors.response.eject(responseInterceptorId)
  }

  requestInterceptorId = api.interceptors.request.use((config) => {
    const accessToken = handlers.getAccessToken()

    if (accessToken && !hasAuthorizationHeader(config.headers)) {
      config.headers = withAuthorizationHeader(config.headers, accessToken)
    }

    return config
  })

  responseInterceptorId = api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const originalRequest = error.config as RetryableRequestConfig | undefined

      if (
        error.response?.status !== 401 ||
        !originalRequest ||
        originalRequest._retry ||
        originalRequest.skipAuthRefresh ||
        isRefreshRequest(originalRequest.url)
      ) {
        return Promise.reject(error)
      }

      originalRequest._retry = true

      try {
        refreshPromise ??= handlers
          .refreshAccessToken()
          .finally(() => {
            refreshPromise = null
          })

        const nextAccessToken = await refreshPromise

        if (!nextAccessToken) {
          throw error
        }

        originalRequest.headers = withAuthorizationHeader(
          originalRequest.headers,
          nextAccessToken
        )

        return api(originalRequest)
      } catch (refreshError) {
        await handlers.onUnauthorized()
        return Promise.reject(refreshError)
      }
    }
  )
}
