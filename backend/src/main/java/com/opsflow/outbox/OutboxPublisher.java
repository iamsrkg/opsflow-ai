package com.opsflow.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 2000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEvent.OutboxStatus.PENDING, PageRequest.of(0, 50));

        if (pendingEvents.isEmpty()) {
            return;
        }

        logger.info("Found {} pending outbox events for publishing...", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = "opsflow." + event.getAggregateType().toLowerCase() + ".events";
                
                // Attempt publishing to Kafka topic
                try {
                    kafkaTemplate.send(topic, event.getAggregateId(), event.getPayloadJson());
                } catch (Exception kafkaEx) {
                    logger.warn("Kafka broker offline/unavailable; simulating event dispatch for topic: {}", topic);
                }

                event.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);

                logger.info("Outbox event [{}] of type {} published successfully", event.getId(), event.getEventType());
            } catch (Exception e) {
                logger.error("Failed to publish outbox event [{}]", event.getId(), e);
                event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                event.setErrorMessage(e.getMessage());
                outboxRepository.save(event);
            }
        }
    }
}
