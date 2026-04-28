import axios from "axios";

export const getErrorMessage = (
  error: unknown,
  fallback = "Something went wrong. Please try again.",
) => {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;

    if (typeof message === "string" && message.length > 0) {
      return message;
    }
  }

  if (error instanceof Error && error.message.length > 0) {
    return error.message;
  }

  return fallback;
};
