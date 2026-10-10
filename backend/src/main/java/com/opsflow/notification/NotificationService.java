package com.opsflow.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.opsflow.events.DomainEvent;
import com.opsflow.events.IdempotentConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    public static final String CONSUMER_GROUP = "opsflow-notifications";

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final IdempotentConsumerService idempotency;

    public NotificationService(NotificationRepository notificationRepository, IdempotentConsumerService idempotency) {
        this.notificationRepository = notificationRepository;
        this.idempotency = idempotency;
    }

    /**
     * Handles one event once per consumer group, even if Kafka delivers it again.
     * The notification and the "processed" marker are written in one transaction: either both
     * are saved or neither is, so a crash in between can't cause a second notification.
     */
    @Transactional
    public void handle(DomainEvent<JsonNode> event) {
        if (idempotency.isAlreadyProcessed(event.getEventId(), CONSUMER_GROUP)) {
            logger.info("Event {} was already handled; skipping the redelivery", event.getEventId());
            return;
        }
        toNotification(event).ifPresent(notificationRepository::save);
        idempotency.markAsProcessed(event.getEventId(), CONSUMER_GROUP, event.getEventType());
    }

    @Transactional(readOnly = true)
    public List<Notification> latest() {
        return notificationRepository.findTop50ByOrderByCreatedAtDesc();
    }

    /** Only some events are worth telling someone about. The rest are still marked as processed. */
    private Optional<Notification> toNotification(DomainEvent<JsonNode> event) {
        JsonNode data = event.getData();
        String reference = text(data, "externalReference", "request " + event.getAggregateId());

        String recipient;
        String message;
        switch (event.getEventType()) {
            case "REQUEST_ASSIGNED" -> {
                recipient = text(data, "assignedTo", null);
                message = reference + " was assigned to you: " + text(data, "title", "");
            }
            case "REQUEST_ESCALATED" -> {
                recipient = "MANAGER";
                message = reference + " was escalated and is now " + text(data, "priority", "CRITICAL");
            }
            case "APPROVAL_REQUESTED" -> {
                recipient = "MANAGER";
                message = "Approval needed for " + text(data, "actionType", "an action")
                        + " on request " + text(data, "requestId", event.getAggregateId());
            }
            case "APPROVAL_APPROVED" -> {
                recipient = text(data, "proposedBy", null);
                message = "Your proposed action " + text(data, "actionType", "") + " was approved";
            }
            default -> {
                return Optional.empty();
            }
        }
        if (recipient == null || recipient.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Notification.builder()
                .recipient(recipient)
                .message(message.length() > 500 ? message.substring(0, 500) : message)
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .aggregateId(event.getAggregateId())
                .correlationId(event.getCorrelationId())
                .build());
    }

    private static String text(JsonNode data, String field, String fallback) {
        JsonNode value = data == null ? null : data.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }
}
