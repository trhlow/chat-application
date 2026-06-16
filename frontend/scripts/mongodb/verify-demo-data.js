const ids = db.users.find({ username: { $regex: "^demo\\." } }, { _id: 1 })
  .toArray()
  .map(user => String(user._id));

printjson({
  demoUsers: ids.length,
  friendships: db.friendships.countDocuments({ userIdA: { $in: ids } }),
  pendingRequests: db.friend_requests.countDocuments({
    status: "PENDING",
    $or: [{ requesterId: { $in: ids } }, { receiverId: { $in: ids } }]
  }),
  rooms: db.rooms.countDocuments({ memberIds: { $in: ids } }),
  messages: db.messages.countDocuments({ senderId: { $in: ids } }),
  notifications: db.notifications.countDocuments({ userId: { $in: ids } }),
  joinRequests: db.group_join_requests.countDocuments({ targetUserId: { $in: ids } })
});

printjson(db.rooms.find(
  { createdBy: ids.find(id => id === String(db.users.findOne({ username: "demo.tui" })._id)) },
  { name: 1, type: 1, memberIds: 1 }
).toArray());
