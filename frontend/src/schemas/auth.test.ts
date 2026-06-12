import { describe, expect, it } from "vitest";

import { signInSchema, signUpSchema } from "@/schemas/auth";

describe("auth schemas", () => {
  it("accepts a valid signup payload", () => {
    expect(
      signUpSchema.safeParse({
        firstName: "An",
        lastName: "Nguyen",
        username: "nguyen_an",
        email: "an@example.com",
        password: "password123",
      }).success,
    ).toBe(true);
  });

  it("rejects invalid usernames and short passwords", () => {
    expect(
      signUpSchema.safeParse({
        firstName: "An",
        lastName: "Nguyen",
        username: "an!",
        email: "an@example.com",
        password: "short",
      }).success,
    ).toBe(false);
    expect(
      signInSchema.safeParse({
        emailOrUsername: "an",
        password: "short",
      }).success,
    ).toBe(false);
  });
});
