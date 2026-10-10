package com.opsflow.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The rules the outbox depends on: nothing is marked sent until Kafka confirms it, and order is kept. */
class OutboxPublisherTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private OutboxRepository repository;
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(OutboxRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
    }

    private OutboxPublisher publisher(boolean kafkaEnabled) {
        return new OutboxPublisher(repository, kafkaTemplate, objectMapper, kafkaEnabled);
    }

    private static OutboxEvent pending(String eventId, String payloadJson) {
        return OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType("REQUEST")
                .aggregateId("42")
                .eventType("REQUEST_CREATED")
                .payloadJson(payloadJson)
                .status(OutboxEvent.OutboxStatus.PENDING)
                .build();
    }

    private void pendingRows(OutboxEvent... events) {
        when(repository.findByStatusOrderByCreatedAtAsc(eq(OutboxEvent.OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(events));
    }

    @SuppressWarnings("unchecked")
    private void brokerAcknowledges() {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));
    }

    @SuppressWarnings("unchecked")
    private void brokerIsDown() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker not available")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void anAcknowledgedEventIsMarkedPublished() {
        OutboxEvent event = pending("evt-1", "{\"title\":\"Payment stuck\"}");
        pendingRows(event);
        brokerAcknowledges();

        publisher(true).publishPendingEvents();

        assertEquals(OutboxEvent.OutboxStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getProcessedAt());
        assertNull(event.getErrorMessage());

        ArgumentCaptor<ProducerRecord<String, String>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(sent.capture());
        assertEquals("opsflow.request.events", sent.getValue().topic());
        assertEquals("42", sent.getValue().key(), "keyed by aggregate id, so one request's events stay in order");
        assertTrue(sent.getValue().value().contains("\"eventId\":\"evt-1\""), "the event id travels in the message");
    }

    @Test
    @SuppressWarnings("unchecked")
    void aBacklogLargerThanOneBatchIsClearedInOneRun() {
        List<OutboxEvent> backlog = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            backlog.add(pending("evt-" + i, "{}"));
        }
        // like the real query: the oldest 50 rows that are still PENDING
        when(repository.findByStatusOrderByCreatedAtAsc(eq(OutboxEvent.OutboxStatus.PENDING), any(Pageable.class)))
                .thenAnswer(call -> backlog.stream()
                        .filter(e -> e.getStatus() == OutboxEvent.OutboxStatus.PENDING).limit(50).toList());
        brokerAcknowledges();

        publisher(true).publishPendingEvents();

        assertTrue(backlog.stream().allMatch(e -> e.getStatus() == OutboxEvent.OutboxStatus.PUBLISHED));
        verify(kafkaTemplate, times(120)).send(any(ProducerRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenTheBrokerIsDownTheEventStaysPendingAndLaterEventsWait() {
        OutboxEvent first = pending("evt-1", "{}");
        OutboxEvent second = pending("evt-2", "{}");
        pendingRows(first, second);
        brokerIsDown();

        publisher(true).publishPendingEvents();

        assertEquals(OutboxEvent.OutboxStatus.PENDING, first.getStatus(), "not confirmed, so not published");
        assertEquals(1, first.getAttempts());
        assertTrue(first.getErrorMessage().contains("broker not available"));
        assertEquals(0, second.getAttempts(), "the second event must not overtake the first");
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aPayloadThatCannotBeSerializedIsMarkedFailedAndDoesNotBlockTheRest() {
        OutboxEvent broken = pending("evt-1", "{not json");
        OutboxEvent fine = pending("evt-2", "{}");
        pendingRows(broken, fine);
        brokerAcknowledges();

        publisher(true).publishPendingEvents();

        assertEquals(OutboxEvent.OutboxStatus.FAILED, broken.getStatus());
        assertEquals(OutboxEvent.OutboxStatus.PUBLISHED, fine.getStatus());
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void withKafkaSwitchedOffNothingIsSentAndNothingIsMarkedPublished() {
        publisher(false).publishPendingEvents();

        verify(repository, never()).findByStatusOrderByCreatedAtAsc(any(), any());
        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
    }
}
