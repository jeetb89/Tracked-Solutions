package com.example.ordermanagement.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfiguration {

    @Bean
    public NewTopic orderEventsTopic(@Value("${app.kafka.order-events-topic:order-events}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic orderEventsDeadLetterTopic(
            @Value("${app.kafka.order-events-dlt-topic:order-events-dlt}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, byte[]> deadLetterBytesProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new DefaultKafkaProducerFactory<>(
                properties, new StringSerializer(), new ByteArraySerializer());
    }

    @Bean
    public KafkaTemplate<String, byte[]> deadLetterBytesKafkaTemplate(
            ProducerFactory<String, byte[]> deadLetterBytesProducerFactory) {
        return new KafkaTemplate<>(deadLetterBytesProducerFactory);
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, OrderEvent> kafkaTemplate,
            KafkaTemplate<String, byte[]> deadLetterBytesKafkaTemplate,
            @Value("${app.kafka.order-events-dlt-topic:order-events-dlt}") String deadLetterTopic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                producerRecord -> producerRecord.value() instanceof byte[]
                        ? deadLetterBytesKafkaTemplate
                        : kafkaTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic, record.partition()));
        recoverer.setFailIfSendResultIsError(true);

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(2);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5_000L);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
