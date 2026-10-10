package com.opsflow.approval;

import com.opsflow.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "approval_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest extends BaseEntity {

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    @Column(nullable = false)
    private Long requestId;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private String proposedBy;

    private String approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;
}
