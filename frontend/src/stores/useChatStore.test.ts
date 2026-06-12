import { describe, expect, it } from "vitest";

import { getNextUnreadCount } from "@/stores/useChatStore";

describe("getNextUnreadCount", () => {
  it("does not increment for a duplicate message", () => {
    expect(
      getNextUnreadCount({
        currentCount: 4,
        isSelected: false,
        alreadyKnown: true,
      }),
    ).toBe(4);
  });

  it("clears unread for the selected room", () => {
    expect(
      getNextUnreadCount({
        currentCount: 4,
        isSelected: true,
        alreadyKnown: false,
      }),
    ).toBe(0);
  });

  it("increments for a new background message", () => {
    expect(
      getNextUnreadCount({
        currentCount: 4,
        isSelected: false,
        alreadyKnown: false,
      }),
    ).toBe(5);
  });
});
