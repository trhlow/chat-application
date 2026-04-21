package com.chatrealtime.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("payments")
public interface PaymentServiceClient {
    @PostExchange
    PaymentResponse createPayment(@RequestBody PaymentRequest request);

    record PaymentRequest(String userId, long amount, String currency) {
    }

    record PaymentResponse(String id, String status) {
    }
}
