package com.opsflow.request;

import com.opsflow.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "request_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestHistory extends BaseEntity {

    @Column(nullable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    private RequestStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus newStatus;

    @Column(nullable = false)
    private String changedBy;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
