package com.chatrealtime.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "uk_user_block_pair", def = "{'blockerId': 1, 'blockedId': 1}", unique = true)
@Document(collection = "user_blocks")
public class UserBlock {
    @Id
    private String id;

    private String blockerId;
    private String blockedId;
    private Instant createdAt;
}
