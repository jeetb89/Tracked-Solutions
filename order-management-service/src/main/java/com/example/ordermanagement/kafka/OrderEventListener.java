package com.example.ordermanagement.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);

    @KafkaListener(topics = "${app.kafka.order-events-topic:order-events}")
    public void onOrderEvent(OrderEvent event) {
        if (event == null || event.eventType() == null || event.orderNumber() == null) {
            throw new IllegalArgumentException("Order event must include an event type and order number");
        }

        logger.info("Received order event {} for order {} at {}",
                event.eventType(), event.orderNumber(), event.occurredAt());
    }

}
