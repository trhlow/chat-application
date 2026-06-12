import { describe, expect, it, vi } from "vitest";

import { sendWithSingleFallback } from "@/lib/message-sender";

describe("sendWithSingleFallback", () => {
  it("does not call REST when realtime accepts the message", async () => {
    const sendRest = vi.fn(async () => ({ id: "rest-message" }));

    const result = await sendWithSingleFallback({
      sendRealtime: () => true,
      sendRest,
    });

    expect(result).toBeNull();
    expect(sendRest).not.toHaveBeenCalled();
  });

  it("calls REST exactly once when realtime is unavailable", async () => {
    const sendRest = vi.fn(async () => ({ id: "rest-message" }));

    const result = await sendWithSingleFallback({
      sendRealtime: () => false,
      sendRest,
    });

    expect(result).toEqual({ id: "rest-message" });
    expect(sendRest).toHaveBeenCalledTimes(1);
  });

  it("does not retry a failed REST request", async () => {
    const error = new Error("network unavailable");
    const sendRest = vi.fn(async () => {
      throw error;
    });

    await expect(
      sendWithSingleFallback({
        sendRealtime: () => false,
        sendRest,
      }),
    ).rejects.toBe(error);
    expect(sendRest).toHaveBeenCalledTimes(1);
  });
});
