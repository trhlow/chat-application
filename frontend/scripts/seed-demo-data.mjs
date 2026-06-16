const API_URL = (process.env.SEED_API_URL ?? "http://localhost:8080/api").replace(/\/$/, "");
const PASSWORD = process.env.SEED_PASSWORD ?? "Test@123456";

const demoUsers = [
  { key: "me", username: "demo.tui", email: "demo.tui@inchat.local", displayName: "Tui Demo" },
  { key: "an", username: "demo.an", email: "demo.an@inchat.local", displayName: "Nguyễn An" },
  { key: "binh", username: "demo.binh", email: "demo.binh@inchat.local", displayName: "Trần Bình" },
  { key: "chi", username: "demo.chi", email: "demo.chi@inchat.local", displayName: "Lê Chi" },
  { key: "dung", username: "demo.dung", email: "demo.dung@inchat.local", displayName: "Phạm Dũng" },
  { key: "linh", username: "demo.linh", email: "demo.linh@inchat.local", displayName: "Hoàng Linh" },
];

const sessions = new Map();
const mongoSeedPath = fileURLToPath(new URL("./mongodb/seed-demo-data.js", import.meta.url));

const pause = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

const request = async (path, { token, method = "GET", body, allow = [] } = {}) => {
  for (let attempt = 0; attempt < 4; attempt += 1) {
    const response = await fetch(`${API_URL}${path}`, {
      method,
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(body ? { "Content-Type": "application/json" } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });

    if (response.status === 429 && attempt < 3) {
      console.log("  Rate limit reached, waiting 22 seconds...");
      await pause(22_000);
      continue;
    }

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok && !allow.includes(response.status)) {
      throw new Error(`${method} ${path} failed (${response.status}): ${text}`);
    }
    return { status: response.status, data };
  }
};

const login = async (user) => {
  const response = await request("/auth/login", {
    method: "POST",
    body: { username: user.username, password: PASSWORD },
  });
  return { ...user, id: response.data.user.id, token: response.data.accessToken };
};

const ensureUser = async (user) => {
  try {
    const session = await login(user);
    sessions.set(user.key, session);
    console.log(`  Reused @${user.username}`);
    return;
  } catch (_error) {
    await request("/auth/register", {
      method: "POST",
      body: { ...user, password: PASSWORD, avatar: null },
    });
    const session = await login(user);
    sessions.set(user.key, session);
    console.log(`  Created @${user.username}`);
  }
};

const session = (key) => sessions.get(key);

const getFriends = async (key) =>
  (await request("/friends", { token: session(key).token })).data;

const ensureFriendship = async (requesterKey, receiverKey) => {
  const requester = session(requesterKey);
  const receiver = session(receiverKey);
  const friends = await getFriends(requesterKey);
  if (friends.some((item) => item.friend.id === receiver.id)) return;

  const outgoing = (await request("/friends/requests/outgoing", { token: requester.token })).data;
  let friendRequest = outgoing.find((item) => item.receiver.id === receiver.id && item.status === "PENDING");
  if (!friendRequest) {
    friendRequest = (await request("/friends/requests", {
      token: requester.token,
      method: "POST",
      body: { receiverId: receiver.id },
    })).data;
  }
  await request(`/friends/requests/${friendRequest.id}/accept`, {
    token: receiver.token,
    method: "POST",
  });
};

const ensurePendingRequest = async (requesterKey, receiverKey) => {
  const requester = session(requesterKey);
  const receiver = session(receiverKey);
  const outgoing = (await request("/friends/requests/outgoing", { token: requester.token })).data;
  if (outgoing.some((item) => item.receiver.id === receiver.id && item.status === "PENDING")) return;
  await request("/friends/requests", {
    token: requester.token,
    method: "POST",
    body: { receiverId: receiver.id },
    allow: [400, 409],
  });
};

const getRooms = async (key) => (await request("/rooms", { token: session(key).token })).data;

const ensureRoom = async (ownerKey, type, memberKeys, name) => {
  const owner = session(ownerKey);
  const memberIds = memberKeys.map((key) => session(key).id);
  const rooms = await getRooms(ownerKey);
  const existing = rooms.find((room) =>
    type === "DIRECT"
      ? room.type === "DIRECT" && memberIds.every((id) => room.memberIds.includes(id))
      : room.type === "GROUP" && room.name === name,
  );
  if (existing) return existing;
  return (await request("/rooms", {
    token: owner.token,
    method: "POST",
    body: { type, name: name ?? null, memberIds },
  })).data;
};

const ensureMessage = async (senderKey, roomId, content) => {
  const sender = session(senderKey);
  const messages = (await request(`/messages?roomId=${roomId}&limit=40`, { token: sender.token })).data.items;
  if (messages.some((message) => message.content === content)) return;
  await request("/messages", {
    token: sender.token,
    method: "POST",
    body: { roomId, content, type: "TEXT", clientMessageId: `seed-${senderKey}-${content.length}` },
  });
};

const main = async () => {
  console.log(`Seeding InChat demo data via ${API_URL}`);
  console.log("Creating/reusing demo accounts...");
  for (const user of demoUsers) await ensureUser(user);

  console.log("Refreshing demo sessions...");
  sessions.clear();
  for (const user of demoUsers) {
    sessions.set(user.key, await login(user));
  }

  console.log("Creating friendships and pending requests...");
  await ensureFriendship("me", "an");
  await ensureFriendship("me", "binh");
  await ensureFriendship("an", "binh");
  await ensurePendingRequest("chi", "me");
  await ensurePendingRequest("me", "dung");

  console.log("Creating direct conversation...");
  const directRoom = await ensureRoom("me", "DIRECT", ["an"], null);
  await ensureMessage("me", directRoom.id, "Chào An, đây là tin nhắn mẫu để test chat 1-1.");
  await ensureMessage("an", directRoom.id, "Mình đã nhận được. Realtime hoạt động tốt nhé!");
  await ensureMessage("me", directRoom.id, "Thử trạng thái sent, delivered và seen trong hội thoại này.");

  console.log("Creating group and members...");
  let group = await ensureRoom("me", "GROUP", ["an", "binh"], "Nhóm Test InChat");
  if (!group.memberIds.includes(session("chi").id)) {
    group = (await request(`/rooms/${group.id}/members`, {
      token: session("me").token,
      method: "POST",
      body: { memberIds: [session("chi").id] },
    })).data;
  }
  await request(`/rooms/${group.id}/admins/${session("an").id}`, {
    token: session("me").token,
    method: "POST",
    allow: [400, 409],
  });
  await request(`/rooms/${group.id}/join-requests`, {
    token: session("me").token,
    method: "POST",
    body: { targetUserId: session("linh").id },
    allow: [400, 409],
  });

  await ensureMessage("me", group.id, "Chào mọi người, đây là nhóm mẫu để test quản lý thành viên.");
  await ensureMessage("an", group.id, "An đang là quản trị viên của nhóm.");
  await ensureMessage("binh", group.id, "Bình đã tham gia và gửi tin nhắn thành công.");
  await ensureMessage("chi", group.id, "Chi vừa được thêm vào nhóm.");

  console.log("\nDemo data is ready.");
  console.log(`Main account: demo.tui / ${PASSWORD}`);
  console.log(`Other accounts: demo.an, demo.binh, demo.chi, demo.dung, demo.linh / ${PASSWORD}`);
  console.log("Scenarios:");
  console.log("- demo.tui is friends with demo.an and demo.binh");
  console.log("- demo.tui has an incoming request from demo.chi");
  console.log("- demo.tui has an outgoing request to demo.dung");
  console.log("- Direct chat exists between demo.tui and demo.an");
  console.log("- Group 'Nhóm Test InChat' contains demo.tui, demo.an, demo.binh and demo.chi");
  console.log("- Group has a pending invite/join request for demo.linh");
};

const seedViaMongo = () => {
  console.log("\nProtected APIs rejected the new token. Falling back to MongoDB seed...");
  const copy = spawnSync("docker", ["cp", mongoSeedPath, "chat-application-mongodb-1:/tmp/seed-demo-data.js"], {
    encoding: "utf8",
    shell: process.platform === "win32",
  });
  if (copy.status !== 0) throw new Error(copy.stderr || "Could not copy MongoDB seed script.");

  const run = spawnSync("docker", [
    "exec",
    "chat-application-mongodb-1",
    "mongosh",
    "--quiet",
    "mongodb://root:root@localhost:27017/hlow_chat?authSource=admin",
    "/tmp/seed-demo-data.js",
  ], {
    encoding: "utf8",
    shell: process.platform === "win32",
  });
  if (run.status !== 0) throw new Error(run.stderr || "Could not run MongoDB seed script.");
  console.log(run.stdout.trim());
};

main().catch((error) => {
  if (error.message.includes("(401)")) {
    try {
      seedViaMongo();
      return;
    } catch (fallbackError) {
      console.error("\nMongoDB fallback failed:", fallbackError.message);
    }
  }
  console.error("\nSeed failed:", error.message);
  console.error("Make sure the backend is running at", API_URL);
  process.exitCode = 1;
});
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
