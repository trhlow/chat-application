db.group_join_requests.createIndex(
  { roomId: 1, status: 1, createdAt: -1 },
  { name: "idx_group_join_request_room_status" }
);

db.group_join_requests.createIndex(
  { roomId: 1, targetUserId: 1, status: 1 },
  {
    unique: true,
    partialFilterExpression: { status: "PENDING" },
    name: "uk_group_join_request_pending_target"
  }
);
