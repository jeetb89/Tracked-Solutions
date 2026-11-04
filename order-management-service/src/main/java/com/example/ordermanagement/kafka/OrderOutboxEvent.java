package com.example.ordermanagement.kafka;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_event_outbox")
public class OrderOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String eventId;

    @Column(nullable = false, length = 32, updatable = false)
    private String eventType;

    @Column(nullable = false, length = 36, updatable = false)
    private String orderNumber;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(length = 2000)
    private String lastError;

    protected OrderOutboxEvent() {
    }

    public static OrderOutboxEvent pending(String eventType, String orderNumber) {
        OrderOutboxEvent event = new OrderOutboxEvent();
        event.eventId = UUID.randomUUID().toString();
        event.eventType = eventType;
        event.orderNumber = orderNumber;
        event.occurredAt = Instant.now();
        event.nextAttemptAt = event.occurredAt;
        return event;
    }

    public OrderEvent toMessage() {
        return new OrderEvent(eventId, eventType, orderNumber, occurredAt);
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(String error, Instant retryAt) {
        attempts++;
        lastError = error == null ? "Unknown Kafka publish failure" : error.substring(0, Math.min(error.length(), 2000));
        nextAttemptAt = retryAt;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getOrderNumber() { return orderNumber; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
}
