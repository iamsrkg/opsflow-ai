package com.opsflow.request;

import com.opsflow.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests", indexes = {
    @Index(name = "idx_req_external_ref", columnList = "externalReference"),
    @Index(name = "idx_req_status", columnList = "status"),
    @Index(name = "idx_req_category", columnList = "category"),
    @Index(name = "idx_req_priority", columnList = "priority")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String externalReference;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(nullable = false)
    private String createdBy;

    private String assignedTo;

    @Column(columnDefinition = "TEXT")
    private String classificationSummary;

    private Double confidenceScore;

    @Version
    private Long version;

    private LocalDateTime resolvedAt;
}
