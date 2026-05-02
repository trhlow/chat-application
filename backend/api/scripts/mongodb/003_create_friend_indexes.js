// Create unique canonical friend indexes after backfill and duplicate cleanup.
// Usage:
//   mongosh "$SPRING_DATA_MONGODB_URI" backend/api/scripts/mongodb/003_create_friend_indexes.js

print("Creating uk_friendship_pair...");
db.friendships.createIndex(
  { userIdA: 1, userIdB: 1 },
  {
    unique: true,
    name: "uk_friendship_pair"
  }
);

print("Creating uk_friend_request_pair_status...");
db.friend_requests.createIndex(
  { userIdA: 1, userIdB: 1, status: 1 },
  {
    unique: true,
    name: "uk_friend_request_pair_status"
  }
);

print("Friend unique indexes are ready.");
