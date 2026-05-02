// Backfill canonical friendship/friend request pairs before creating unique indexes.
// Usage:
//   mongosh "$SPRING_DATA_MONGODB_URI" backend/api/scripts/mongodb/000_backfill_friend_pairs.js

function orderedPair(first, second) {
  if (!first || !second) {
    return null;
  }
  return first <= second
    ? { userIdA: first, userIdB: second }
    : { userIdA: second, userIdB: first };
}

print("Backfilling friendships.userIdA/userIdB...");
let friendshipUpdates = 0;
db.friendships.find({
  $or: [
    { userIdA: { $exists: false } },
    { userIdB: { $exists: false } },
    { userIdA: null },
    { userIdB: null }
  ]
}).forEach(friendship => {
  if (!Array.isArray(friendship.userIds) || friendship.userIds.length < 2) {
    print("Skipping friendship without two userIds: " + friendship._id);
    return;
  }

  const pair = orderedPair(friendship.userIds[0], friendship.userIds[1]);
  if (!pair) {
    print("Skipping friendship with invalid userIds: " + friendship._id);
    return;
  }

  db.friendships.updateOne(
    { _id: friendship._id },
    { $set: { userIdA: pair.userIdA, userIdB: pair.userIdB } }
  );
  friendshipUpdates++;
});
print("Updated friendships: " + friendshipUpdates);

print("Backfilling friend_requests.userIdA/userIdB...");
let requestUpdates = 0;
db.friend_requests.find({
  $or: [
    { userIdA: { $exists: false } },
    { userIdB: { $exists: false } },
    { userIdA: null },
    { userIdB: null }
  ]
}).forEach(request => {
  const pair = orderedPair(request.requesterId, request.receiverId);
  if (!pair) {
    print("Skipping friend request with invalid requester/receiver: " + request._id);
    return;
  }

  db.friend_requests.updateOne(
    { _id: request._id },
    { $set: { userIdA: pair.userIdA, userIdB: pair.userIdB } }
  );
  requestUpdates++;
});
print("Updated friend_requests: " + requestUpdates);
