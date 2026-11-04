package com.example.ordermanagement.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OrderEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final int BATCH_SIZE = 50;
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final OrderOutboxRepository outboxRepository;
    private final String topic;

    public OrderEventPublisher(
            KafkaTemplate<String, OrderEvent> kafkaTemplate,
            OrderOutboxRepository outboxRepository,
            @Value("${app.kafka.order-events-topic:order-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepository = outboxRepository;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.poll-interval:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OrderOutboxEvent> pending = outboxRepository.findPendingForUpdate(PageRequest.of(0, BATCH_SIZE));
        for (OrderOutboxEvent event : pending) {
            Instant now = Instant.now();
            if (event.getNextAttemptAt().isAfter(now)) {
                return;
            }

            try {
                kafkaTemplate.send(topic, event.getOrderNumber(), event.toMessage())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.markPublished(Instant.now());
                logger.debug("Published order event {} for order {}", event.getEventId(), event.getOrderNumber());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                recordFailure(event, exception);
                return;
            } catch (ExecutionException | TimeoutException | RuntimeException exception) {
                recordFailure(event, exception);
                return;
            }
        }
    }

    private void recordFailure(OrderOutboxEvent event, Exception exception) {
        long retryDelaySeconds = Math.min(300L, 1L << Math.min(event.getAttempts(), 8));
        event.markFailed(exception.getMessage(), Instant.now().plusSeconds(retryDelaySeconds));
        logger.error("Failed to publish order event {}; retrying in {} seconds",
                event.getEventId(), retryDelaySeconds, exception);
    }
}
