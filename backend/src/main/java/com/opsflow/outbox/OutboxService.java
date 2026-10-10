package com.opsflow.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> void saveEvent(String aggregateType, String aggregateId, String eventType, T payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String correlationId = MDC.get("correlationId");

            OutboxEvent event = OutboxEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payloadJson(payloadJson)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .correlationId(correlationId)
                    .build();

            outboxRepository.save(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}
