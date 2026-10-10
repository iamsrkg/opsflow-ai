package com.opsflow.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.events.DomainEvent;
import com.opsflow.events.Topics;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sends outbox rows to Kafka.
 *
 * A row is marked PUBLISHED only after the broker has acknowledged it. If the broker can't be
 * reached the row stays PENDING and is tried again on the next run, so an event is never lost
 * and never reported as sent when it wasn't. The same event can therefore be delivered more
 * than once (for example if the app stops between the acknowledgement and the status update),
 * which is why consumers check the event id.
 */
@Component
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 50;
    private static final long ACK_TIMEOUT_SECONDS = 5;
    private static final long MAX_RUN_NANOS = TimeUnit.SECONDS.toNanos(10);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final boolean kafkaEnabled;

    public OutboxPublisher(OutboxRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           ObjectMapper objectMapper,
                           @Value("${opsflow.kafka.enabled:false}") boolean kafkaEnabled) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.kafkaEnabled = kafkaEnabled;
    }

    @Scheduled(fixedDelayString = "${opsflow.outbox.poll-ms:2000}")
    public void publishPendingEvents() {
        if (!kafkaEnabled) {
            return;   // no broker configured: events wait in the outbox
        }
        // Keep taking batches until the outbox is empty. Stopping after one batch would cap
        // publishing at one batch per poll (25 events a second), however fast the broker is.
        long stopAt = System.nanoTime() + MAX_RUN_NANOS;
        List<OutboxEvent> pending;
        do {
            pending = outboxRepository.findByStatusOrderByCreatedAtAsc(
                    OutboxEvent.OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
            for (OutboxEvent event : pending) {
                if (!publish(event)) {
                    return;   // keep the order: don't send later events past one that failed
                }
            }
        } while (pending.size() == BATCH_SIZE && System.nanoTime() < stopAt);
    }

    /** @return false if the broker did not acknowledge the event and the batch should stop */
    boolean publish(OutboxEvent event) {
        String message;
        try {
            message = objectMapper.writeValueAsString(toEnvelope(event));
        } catch (JsonProcessingException e) {
            // retrying can't fix a payload that doesn't serialize
            event.setStatus(OutboxEvent.OutboxStatus.FAILED);
            event.setErrorMessage(shorten("Could not build the event: " + e.getOriginalMessage()));
            outboxRepository.save(event);
            logger.error("Outbox event {} can't be serialized and was marked FAILED", event.getEventId(), e);
            return true;   // nothing to wait for; carry on with the next event
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(
                Topics.forAggregate(event.getAggregateType()), event.getAggregateId(), message);
        record.headers().add("eventId", event.getEventId().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));

        try {
            kafkaTemplate.send(record).get(ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            event.setAttempts(event.getAttempts() + 1);
            event.setErrorMessage(shorten(e.getClass().getSimpleName() + ": " + e.getMessage()));
            outboxRepository.save(event);
            logger.warn("Kafka did not acknowledge outbox event {} (attempt {}); it stays PENDING",
                    event.getEventId(), event.getAttempts());
            return false;
        }

        event.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
        event.setProcessedAt(LocalDateTime.now());
        event.setErrorMessage(null);
        outboxRepository.save(event);
        return true;
    }

    /** The error column holds 255 characters. */
    private static String shorten(String message) {
        return message.length() <= 250 ? message : message.substring(0, 250);
    }

    private DomainEvent<JsonNode> toEnvelope(OutboxEvent event) throws JsonProcessingException {
        return DomainEvent.<JsonNode>builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .correlationId(event.getCorrelationId())
                .timestamp(event.getCreatedAt())
                .data(objectMapper.readTree(event.getPayloadJson()))
                .build();
    }
}
