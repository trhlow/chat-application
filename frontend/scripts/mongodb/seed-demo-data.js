// Run against the hlow_chat database after scripts/seed-demo-data.mjs has created the demo users.
// Example:
//   mongosh "mongodb://root:root@localhost:27017/hlow_chat?authSource=admin" scripts/mongodb/seed-demo-data.js

const now = new Date();
const minutesAgo = (minutes) => new Date(now.getTime() - minutes * 60 * 1000);

const users = {};
["demo.tui", "demo.an", "demo.binh", "demo.chi", "demo.dung", "demo.linh"].forEach(username => {
  const user = db.users.findOne({ username });
  if (!user) throw new Error("Missing demo user: " + username + ". Run npm run seed:demo once first.");
  users[username] = String(user._id);
});

const orderedPair = (left, right) => left < right ? [left, right] : [right, left];

function ensureFriendship(left, right, ageMinutes) {
  const [userIdA, userIdB] = orderedPair(users[left], users[right]);
  db.friendships.updateOne(
    { userIdA, userIdB },
    { $setOnInsert: { userIdA, userIdB, userIds: [userIdA, userIdB], createdAt: minutesAgo(ageMinutes) } },
    { upsert: true }
  );
}

function ensurePendingRequest(requester, receiver, ageMinutes) {
  const requesterId = users[requester];
  const receiverId = users[receiver];
  const [userIdA, userIdB] = orderedPair(requesterId, receiverId);
  db.friend_requests.updateOne(
    { userIdA, userIdB, status: "PENDING" },
    { $setOnInsert: { requesterId, receiverId, userIdA, userIdB, status: "PENDING", createdAt: minutesAgo(ageMinutes), respondedAt: null } },
    { upsert: true }
  );
}

ensureFriendship("demo.tui", "demo.an", 720);
ensureFriendship("demo.tui", "demo.binh", 600);
ensureFriendship("demo.an", "demo.binh", 480);
ensurePendingRequest("demo.chi", "demo.tui", 30);
ensurePendingRequest("demo.tui", "demo.dung", 20);

const directMembers = [users["demo.tui"], users["demo.an"]].sort();
const directKey = directMembers.join(":");
const directRoom = db.rooms.findOneAndUpdate(
  { type: "direct", directKey },
  {
    $setOnInsert: {
      name: null,
      type: "direct",
      avatar: null,
      avatarPublicId: null,
      avatarProvider: null,
      directKey,
      memberIds: directMembers,
      admins: [],
      settings: {
        sendMessagePermission: "ALL",
        editGroupInfoPermission: "ADMIN_ONLY",
        inviteMemberPermission: "ALL",
        allowNewMemberReadHistory: true
      },
      createdBy: users["demo.tui"],
      ownerId: null,
      createdAt: minutesAgo(180),
      updatedAt: minutesAgo(2)
    },
    $set: {
      lastMessageAt: minutesAgo(2),
      lastMessagePreview: "Thử trạng thái sent, delivered và seen trong hội thoại này."
    }
  },
  { upsert: true, returnDocument: "after" }
);

const malformedGroup = db.rooms.findOne({
  type: "group",
  name: "Nh??m Test InChat",
  createdBy: users["demo.tui"]
});
if (malformedGroup) {
  const malformedRoomId = String(malformedGroup._id);
  db.messages.deleteMany({ roomId: malformedRoomId });
  db.group_join_requests.deleteMany({ roomId: malformedRoomId });
  db.rooms.deleteOne({ _id: malformedGroup._id });
}

const groupRoom = db.rooms.findOneAndUpdate(
  { type: "group", name: "Nhóm Test InChat", createdBy: users["demo.tui"] },
  {
    $setOnInsert: {
      name: "Nhóm Test InChat",
      type: "group",
      avatar: null,
      avatarPublicId: null,
      avatarProvider: null,
      directKey: null,
      memberIds: [users["demo.tui"], users["demo.an"], users["demo.binh"], users["demo.chi"]],
      admins: [users["demo.an"]],
      settings: {
        sendMessagePermission: "ALL",
        editGroupInfoPermission: "ADMIN_ONLY",
        inviteMemberPermission: "ALL",
        allowNewMemberReadHistory: true
      },
      createdBy: users["demo.tui"],
      ownerId: users["demo.tui"],
      createdAt: minutesAgo(120),
      updatedAt: minutesAgo(1)
    },
    $set: {
      lastMessageAt: minutesAgo(1),
      lastMessagePreview: "Chi vừa được thêm vào nhóm."
    }
  },
  { upsert: true, returnDocument: "after" }
);

function ensureMessage(roomId, sender, content, ageMinutes, readBy = []) {
  const senderId = users[sender];
  const clientMessageId = `seed-${sender.replace(".", "-")}-${ageMinutes}`;
  db.messages.updateOne(
    { roomId: String(roomId), senderId, clientMessageId },
    {
      $set: {
        roomId: String(roomId),
        senderId,
        content,
        type: "TEXT",
        replyToMessageId: null,
        clientMessageId,
        timestamp: minutesAgo(ageMinutes),
        status: readBy.length ? "seen" : "sent",
        deliveredToUserIds: readBy,
        readByUserIds: readBy,
        recalled: false,
        recalledAt: null,
        deletedForUserIds: [],
        updatedAt: minutesAgo(ageMinutes)
      }
    },
    { upsert: true }
  );
}

ensureMessage(directRoom._id, "demo.tui", "Chào An, đây là tin nhắn mẫu để test chat 1-1.", 12, [users["demo.an"]]);
ensureMessage(directRoom._id, "demo.an", "Mình đã nhận được. Realtime hoạt động tốt nhé!", 8, [users["demo.tui"]]);
ensureMessage(directRoom._id, "demo.tui", "Thử trạng thái sent, delivered và seen trong hội thoại này.", 2);

ensureMessage(groupRoom._id, "demo.tui", "Chào mọi người, đây là nhóm mẫu để test quản lý thành viên.", 10, [users["demo.an"], users["demo.binh"]]);
ensureMessage(groupRoom._id, "demo.an", "An đang là quản trị viên của nhóm.", 7, [users["demo.tui"]]);
ensureMessage(groupRoom._id, "demo.binh", "Bình đã tham gia và gửi tin nhắn thành công.", 4);
ensureMessage(groupRoom._id, "demo.chi", "Chi vừa được thêm vào nhóm.", 1);

db.group_join_requests.updateOne(
  { roomId: String(groupRoom._id), targetUserId: users["demo.linh"], status: "PENDING" },
  {
    $setOnInsert: {
      roomId: String(groupRoom._id),
      requesterId: users["demo.tui"],
      targetUserId: users["demo.linh"],
      status: "PENDING",
      createdAt: minutesAgo(15),
      respondedAt: null,
      respondedBy: null
    }
  },
  { upsert: true }
);

[
  {
    key: "incoming-friend",
    type: "friend_request",
    title: "Lời mời kết bạn mới",
    message: "Lê Chi đã gửi lời mời kết bạn cho bạn.",
    relatedId: users["demo.chi"],
    read: false,
    age: 30
  },
  {
    key: "group-message",
    type: "group_message",
    title: "Tin nhắn nhóm mới",
    message: "Nhóm Test InChat đang có tin nhắn mới.",
    relatedId: String(groupRoom._id),
    read: false,
    age: 1
  }
].forEach(item => {
  db.notifications.updateOne(
    { userId: users["demo.tui"], seedKey: item.key },
    {
      $set: {
        userId: users["demo.tui"],
        seedKey: item.key,
        type: item.type,
        title: item.title,
        message: item.message,
        relatedId: item.relatedId,
        read: item.read,
        createdAt: minutesAgo(item.age)
      }
    },
    { upsert: true }
  );
});

print("Demo MongoDB data is ready.");
print("Main account: demo.tui / Test@123456");
print("Direct room: " + directRoom._id);
print("Group room: " + groupRoom._id);
