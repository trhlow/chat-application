import axios from "axios"

import type { ApiErrorResponse } from "@/types/auth"

export function getApiErrorResponse(error: unknown) {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return null
  }

  return error.response?.data ?? null
}

export function getErrorMessage(
  error: unknown,
  fallback = "Da co loi xay ra. Vui long thu lai."
) {
  return getApiErrorResponse(error)?.message ?? fallback
}
