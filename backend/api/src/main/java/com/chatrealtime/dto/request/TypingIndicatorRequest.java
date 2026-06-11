package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotNull;

public record TypingIndicatorRequest(
        @NotNull(message = "typing is required")
        Boolean typing
) {
}
