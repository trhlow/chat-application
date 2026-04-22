    ---
    name: chat-realtime-websocket
    description: Use for STOMP/WebSocket features, room message delivery, message-status broadcast, presence lifecycle, realtime controllers, websocket security, broker destinations, and any task about online/offline or live chat event flow.
    ---

    # chat-realtime-websocket

## Scope
Use this skill for:
- STOMP endpoint changes
- room message send/broadcast flow
- message status realtime updates
- presence online/offline events
- websocket controller and interceptor work
- destination naming consistency

## Relevant Packages
- `controller/RealtimeController.java`
- `event/PresenceWebSocketEventListener.java`
- `service/PresenceService.java`
- `security/WebSocketAuthChannelInterceptor.java`
- websocket/security config classes under `config/`
- message/room services if the realtime flow persists data before broadcasting

## Working Rules
1. Keep transport concerns in realtime controller/interceptor/config; keep business decisions in services.
2. Ensure REST and WebSocket flows agree on identity, room membership assumptions, and message status semantics.
3. Prefer explicit destination naming; do not scatter magic strings.
4. Presence should be lifecycle-oriented, not controller-driven.
5. If changing status delivery or room messaging, trace the flow end-to-end:
   - inbound STOMP destination
   - auth principal resolution
   - service call
   - persistence
   - outbound topic
6. Handle unauthorized or malformed websocket frames conservatively.

## Design Checklist
- Does the sender have the right to publish to that room?
- Are room topics consistent across send and subscribe flows?
- Is persistence ordered correctly relative to broadcast?
- Are duplicate events possible?
- Does reconnect/disconnect affect presence correctness?

## Tests / Verification
Where feasible, add focused tests for:
- authorization-related message handling
- topic naming consistency
- presence side effects
- service behavior behind realtime endpoints
