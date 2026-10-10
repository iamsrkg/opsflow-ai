package com.opsflow.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.events.DomainEvent;
import com.opsflow.events.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reads the events the outbox publishes. Anything this method throws is handled by the error
 * handler in KafkaConfig: retried, then moved to the dead-letter topic.
 */
@Component
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = {Topics.REQUEST, Topics.INVESTIGATION, Topics.APPROVAL},
            groupId = NotificationService.CONSUMER_GROUP,
            autoStartup = "${opsflow.kafka.enabled:false}")
    public void onEvent(String message) throws JsonProcessingException {
        DomainEvent<JsonNode> event = objectMapper.readValue(message, new TypeReference<>() {
        });
        if (event.getEventId() == null || event.getEventType() == null) {
            throw new IllegalArgumentException("Not an OpsFlow event: it has no id or type");
        }
        notificationService.handle(event);
    }
}
