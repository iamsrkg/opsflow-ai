package com.opsflow.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(ApprovalRequest.ApprovalStatus status);
    List<ApprovalRequest> findByRequestId(Long requestId);
}
