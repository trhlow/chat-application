// Dry-run cleanup for duplicate canonical friend records.
// Keeps earliest createdAt; ties are resolved by _id.
// Usage dry-run:
//   mongosh "$SPRING_DATA_MONGODB_URI" backend/api/scripts/mongodb/002_cleanup_friend_duplicates.js
// Usage execute:
//   mongosh "$SPRING_DATA_MONGODB_URI" --eval "var RUN_CLEANUP=true" backend/api/scripts/mongodb/002_cleanup_friend_duplicates.js

const runCleanup = typeof RUN_CLEANUP !== "undefined" && RUN_CLEANUP === true;

function sortSurvivorFirst(left, right) {
  const leftCreated = left.createdAt ? new Date(left.createdAt).getTime() : 0;
  const rightCreated = right.createdAt ? new Date(right.createdAt).getTime() : 0;
  if (leftCreated !== rightCreated) {
    return leftCreated - rightCreated;
  }
  return String(left._id).localeCompare(String(right._id));
}

function cleanupCollection(collectionName, groupIdFactory) {
  print((runCleanup ? "Cleaning" : "Dry-run checking") + " " + collectionName + " duplicates...");
  let duplicateGroups = 0;
  let deleteCount = 0;

  db.getCollection(collectionName).aggregate([
    {
      $group: {
        _id: groupIdFactory,
        count: { $sum: 1 },
        docs: {
          $push: {
            _id: "$_id",
            createdAt: "$createdAt"
          }
        }
      }
    },
    { $match: { count: { $gt: 1 } } }
  ]).forEach(group => {
    duplicateGroups++;
    const docs = group.docs.sort(sortSurvivorFirst);
    const survivor = docs[0];
    const duplicateIds = docs.slice(1).map(doc => doc._id);
    deleteCount += duplicateIds.length;

    printjson({
      collection: collectionName,
      duplicateKey: group._id,
      keep: survivor._id,
      delete: duplicateIds
    });

    if (runCleanup && duplicateIds.length > 0) {
      db.getCollection(collectionName).deleteMany({ _id: { $in: duplicateIds } });
    }
  });

  print(collectionName + " duplicate groups: " + duplicateGroups);
  print(collectionName + " documents " + (runCleanup ? "deleted: " : "that would be deleted: ") + deleteCount);
}

print("RUN_CLEANUP=" + runCleanup);
cleanupCollection("friendships", { userIdA: "$userIdA", userIdB: "$userIdB" });
cleanupCollection("friend_requests", { userIdA: "$userIdA", userIdB: "$userIdB", status: "$status" });
if (!runCleanup) {
  print("Dry-run only. Re-run with --eval \"var RUN_CLEANUP=true\" to delete duplicates.");
}
