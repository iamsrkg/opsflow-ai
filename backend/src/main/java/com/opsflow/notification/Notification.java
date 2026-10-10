package com.opsflow.notification;

import com.opsflow.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Something a person should be told, written by the Kafka consumer after the request itself was saved. */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_recipient", columnList = "recipient")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    /** A username, or a role such as MANAGER. */
    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    private String aggregateId;

    private String correlationId;
}
