package com.opsflow.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.notification.Notification;
import com.opsflow.notification.NotificationRepository;
import com.opsflow.notification.NotificationService;
import com.opsflow.outbox.OutboxEvent;
import com.opsflow.outbox.OutboxRepository;
import com.opsflow.request.RequestService;
import com.opsflow.request.dto.RequestDTOs.CreateRequestDTO;
import com.opsflow.request.dto.RequestDTOs.RequestResponseDTO;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole asynchronous path against a real (in-process) Kafka broker:
 * request saved -> outbox row -> published -> consumed once -> notification.
 */
@SpringBootTest(properties = {
        "opsflow.kafka.enabled=true",
        "opsflow.outbox.poll-ms=200",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 3, topics = {
        Topics.REQUEST, Topics.REQUEST + Topics.DEAD_LETTER_SUFFIX,
        Topics.INVESTIGATION, Topics.INVESTIGATION + Topics.DEAD_LETTER_SUFFIX,
        Topics.APPROVAL, Topics.APPROVAL + Topics.DEAD_LETTER_SUFFIX
})
@DirtiesContext
class KafkaFlowTest {

    private static final Duration PATIENCE = Duration.ofSeconds(30);

    @Autowired
    private RequestService requestService;
    @Autowired
    private OutboxRepository outboxRepository;
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EmbeddedKafkaBroker broker;

    private RequestResponseDTO newRequest() {
        CreateRequestDTO dto = new CreateRequestDTO();
        dto.setTitle("Charged but order still pending");
        dto.setDescription("Customer paid, order did not update.");
        return requestService.createRequest(dto);
    }

    private OutboxEvent outboxEvent(long requestId, String eventType) {
        return outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(String.valueOf(requestId)) && e.getEventType().equals(eventType))
                .findFirst()
                .orElseThrow();
    }

    private boolean handled(String eventId) {
        return processedEventRepository.existsByEventIdAndConsumerGroup(eventId, NotificationService.CONSUMER_GROUP);
    }

    private String envelope(String eventId, String eventType, String aggregateId, Map<String, Object> data) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "eventId", eventId, "eventType", eventType,
                "aggregateType", "REQUEST", "aggregateId", aggregateId, "data", data));
    }

    @Test
    void anAssignmentIsPublishedConsumedAndTurnedIntoOneNotification() {
        RequestResponseDTO request = newRequest();
        requestService.assignRequest(request.getId(), "agent");
        OutboxEvent assigned = outboxEvent(request.getId(), "REQUEST_ASSIGNED");

        await().atMost(PATIENCE).until(() -> handled(assigned.getEventId()));

        assertEquals(OutboxEvent.OutboxStatus.PUBLISHED,
                outboxRepository.findById(assigned.getId()).orElseThrow().getStatus());
        List<Notification> notifications = notificationRepository.findAll().stream()
                .filter(n -> n.getEventId().equals(assigned.getEventId()))
                .toList();
        assertEquals(1, notifications.size());
        assertEquals("agent", notifications.get(0).getRecipient());
        assertTrue(notifications.get(0).getMessage().contains(request.getExternalReference()));
    }

    @Test
    void theSameEventDeliveredTwiceIsHandledOnce() throws Exception {
        String key = "dup-" + UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();
        String duplicate = envelope(eventId, "REQUEST_ESCALATED", key,
                Map.of("externalReference", "REQ-DUP", "priority", "CRITICAL"));

        kafkaTemplate.send(Topics.REQUEST, key, duplicate).get();
        kafkaTemplate.send(Topics.REQUEST, key, duplicate).get();
        // a later event with the same key lands on the same partition, so once it has been
        // handled both copies above have been read too
        String marker = UUID.randomUUID().toString();
        kafkaTemplate.send(Topics.REQUEST, key, envelope(marker, "REQUEST_CREATED", key, Map.of())).get();

        await().atMost(PATIENCE).until(() -> handled(marker));

        assertEquals(1, notificationRepository.countByEventId(eventId), "one notification, not two");
        assertEquals(1, processedEventRepository.findAll().stream()
                .filter(p -> p.getEventId().equals(eventId)).count());
    }

    @Test
    void aMessageThatCannotBeReadGoesToTheDeadLetterTopicAndTheConsumerCarriesOn() throws Exception {
        String key = "bad-" + UUID.randomUUID();
        kafkaTemplate.send(Topics.REQUEST, key, "this is not an event").get();
        String after = UUID.randomUUID().toString();
        kafkaTemplate.send(Topics.REQUEST, key, envelope(after, "REQUEST_CREATED", key, Map.of())).get();

        await().atMost(PATIENCE).until(() -> handled(after));   // the bad message did not block the partition

        Map<String, Object> props = KafkaTestUtils.consumerProps("dead-letter-reader", "true", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> reader = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(reader, Topics.REQUEST + Topics.DEAD_LETTER_SUFFIX);
            boolean found = false;
            for (ConsumerRecord<String, String> record : KafkaTestUtils.getRecords(reader, PATIENCE)) {
                found |= key.equals(record.key()) && "this is not an event".equals(record.value());
            }
            assertTrue(found, "the unreadable message should be waiting on the dead-letter topic");
        }
    }
}
