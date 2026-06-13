db.user_blocks.createIndex(
  { blockerId: 1, blockedId: 1 },
  { unique: true, name: "uk_user_block_pair" }
);

db.user_blocks.createIndex(
  { blockerId: 1, createdAt: -1 },
  { name: "idx_user_blocks_blocker_created_at" }
);
