package com.opsflow.ai;

import com.opsflow.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "ai_investigations", indexes = {
    @Index(name = "idx_ai_req_id", columnList = "requestId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIInvestigation extends BaseEntity {

    @Column(nullable = false)
    private Long requestId;

    @Column(nullable = false)
    private String investigatedBy;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String evidenceJson; // Array of evidence items

    @Column(columnDefinition = "TEXT")
    private String likelyCause;

    private Double confidenceScore;

    private String recommendedAction;

    private Boolean requiresApproval;

    private String relevantRunbookTitle;
}
