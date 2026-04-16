package com.chatrealtime.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> items,
        LocalDateTime nextBefore,
        boolean hasMore
) {
}


