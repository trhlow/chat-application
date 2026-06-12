import { describe, expect, it } from "vitest";

import {
  encodeStompFrame,
  parsePresenceEvent,
  parseRealtimeMessage,
  parseStompFrame,
} from "@/lib/realtime-client";

describe("realtime client helpers", () => {
  it("encodes a valid STOMP frame", () => {
    expect(
      encodeStompFrame(
        "SEND",
        { destination: "/app/rooms/room-1/messages" },
        '{"content":"hello"}',
      ),
    ).toBe(
      'SEND\ndestination:/app/rooms/room-1/messages\n\n{"content":"hello"}\0',
    );
  });

  it("rejects malformed realtime payloads", () => {
    expect(parseRealtimeMessage("not-json")).toBeNull();
    expect(parsePresenceEvent("{")).toBeNull();
  });

  it("preserves blank lines inside a STOMP message body", () => {
    const frame =
      'MESSAGE\nsubscription:sub-1\n\n{"content":"line 1\\n\\nline 2"}';

    expect(parseStompFrame(frame)).toEqual({
      command: "MESSAGE",
      headers: { subscription: "sub-1" },
      body: '{"content":"line 1\\n\\nline 2"}',
    });
  });
});
