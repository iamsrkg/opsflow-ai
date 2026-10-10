package com.opsflow.events;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotentConsumerService {

    private final ProcessedEventRepository repository;

    public IdempotentConsumerService(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    public boolean isAlreadyProcessed(String eventId, String consumerGroup) {
        return repository.existsByEventIdAndConsumerGroup(eventId, consumerGroup);
    }

    @Transactional
    public void markAsProcessed(String eventId, String consumerGroup, String eventType) {
        if (!isAlreadyProcessed(eventId, consumerGroup)) {
            ProcessedEvent processedEvent = ProcessedEvent.builder()
                    .eventId(eventId)
                    .consumerGroup(consumerGroup)
                    .eventType(eventType)
                    .build();
            repository.save(processedEvent);
        }
    }
}
