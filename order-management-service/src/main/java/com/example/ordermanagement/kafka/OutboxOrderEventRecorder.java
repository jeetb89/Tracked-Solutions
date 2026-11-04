package com.example.ordermanagement.kafka;

import com.example.ordermanagement.order.OrderEventRecorder;
import org.springframework.stereotype.Component;

@Component
public class OutboxOrderEventRecorder implements OrderEventRecorder {

    private final OrderOutboxRepository outboxRepository;

    public OutboxOrderEventRecorder(OrderOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void recordCreated(String orderNumber) {
        record("ORDER_CREATED", orderNumber);
    }

    @Override
    public void recordUpdated(String orderNumber) {
        record("ORDER_UPDATED", orderNumber);
    }

    @Override
    public void recordDeleted(String orderNumber) {
        record("ORDER_DELETED", orderNumber);
    }

    private void record(String eventType, String orderNumber) {
        outboxRepository.save(OrderOutboxEvent.pending(eventType, orderNumber));
    }
}
