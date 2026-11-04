package com.example.ordermanagement.kafka;

import java.time.Instant;

public record OrderEvent(String eventId, String eventType, String orderNumber, Instant occurredAt) { }
