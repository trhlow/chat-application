// Report duplicate canonical friend records before creating unique indexes.
// Usage:
//   mongosh "$SPRING_DATA_MONGODB_URI" backend/api/scripts/mongodb/001_check_friend_duplicates.js

let duplicateCount = 0;

print("Checking duplicate friendships...");
db.friendships.aggregate([
  {
    $group: {
      _id: { userIdA: "$userIdA", userIdB: "$userIdB" },
      count: { $sum: 1 },
      ids: { $push: "$_id" }
    }
  },
  { $match: { count: { $gt: 1 } } }
]).forEach(doc => {
  duplicateCount++;
  printjson(doc);
});

print("Checking duplicate friend requests...");
db.friend_requests.aggregate([
  {
    $group: {
      _id: { userIdA: "$userIdA", userIdB: "$userIdB", status: "$status" },
      count: { $sum: 1 },
      ids: { $push: "$_id" }
    }
  },
  { $match: { count: { $gt: 1 } } }
]).forEach(doc => {
  duplicateCount++;
  printjson(doc);
});

print("Duplicate groups found: " + duplicateCount);
if (duplicateCount > 0) {
  print("Resolve duplicates before running 003_create_friend_indexes.js.");
}
