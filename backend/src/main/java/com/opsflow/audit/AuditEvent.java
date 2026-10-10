package com.opsflow.audit;

import com.opsflow.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_req_id", columnList = "requestId"),
    @Index(name = "idx_audit_who", columnList = "who"),
    @Index(name = "idx_audit_corr_id", columnList = "correlationId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent extends BaseEntity {

    @Column(nullable = false)
    private String who;

    @Column(nullable = false)
    private String what;

    @Column(columnDefinition = "TEXT")
    private String why;

    private Long requestId;

    private String correlationId;

    private String action;

    @Column(nullable = false)
    private String result;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;
}
