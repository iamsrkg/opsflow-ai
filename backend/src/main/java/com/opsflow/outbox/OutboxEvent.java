package com.opsflow.outbox;

import com.opsflow.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status", columnList = "status"),
    @Index(name = "idx_outbox_created", columnList = "createdAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent extends BaseEntity {

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }

    /** Travels with the event to Kafka, so consumers can recognise a redelivery. */
    @Column(nullable = false, unique = true, updatable = false)
    private String eventId;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    private String correlationId;

    private LocalDateTime processedAt;

    private String errorMessage;

    /** How many times publishing has been tried. */
    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;
}
