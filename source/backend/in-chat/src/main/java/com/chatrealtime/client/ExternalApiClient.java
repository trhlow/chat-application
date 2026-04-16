package com.chatrealtime.client;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("status")
public interface ExternalApiClient {
    @GetExchange
    ExternalStatusResponse getStatus();

    record ExternalStatusResponse(String status) {
    }
}
