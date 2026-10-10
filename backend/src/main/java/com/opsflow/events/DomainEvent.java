package com.opsflow.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent<T> {
    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String correlationId;
    private LocalDateTime timestamp;
    private T data;
}
