package com.opsflow.events;

import com.opsflow.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Entity
@Table(name = "processed_events", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"eventId", "consumerGroup"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent extends BaseEntity {

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String consumerGroup;

    private String eventType;
}
