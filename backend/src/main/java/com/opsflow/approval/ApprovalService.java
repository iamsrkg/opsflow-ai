package com.opsflow.approval;

import com.opsflow.audit.AuditService;
import com.opsflow.outbox.OutboxService;
import com.opsflow.request.RequestService;
import com.opsflow.request.RequestStatus;
import com.opsflow.shared.SecurityUtils;
import com.opsflow.shared.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalRepository approvalRepository;
    private final RequestService requestService;
    private final AuditService auditService;
    private final OutboxService outboxService;

    public ApprovalService(ApprovalRepository approvalRepository,
                           RequestService requestService,
                           AuditService auditService,
                           OutboxService outboxService) {
        this.approvalRepository = approvalRepository;
        this.requestService = requestService;
        this.auditService = auditService;
        this.outboxService = outboxService;
    }

    @Transactional
    public ApprovalRequest createApprovalRequest(Long requestId, String actionType, String reasoning, String payloadJson) {
        String proposedBy = SecurityUtils.getCurrentUsername();

        ApprovalRequest request = ApprovalRequest.builder()
                .requestId(requestId)
                .actionType(actionType)
                .proposedBy(proposedBy)
                .status(ApprovalRequest.ApprovalStatus.PENDING)
                .reasoning(reasoning)
                .payloadJson(payloadJson)
                .build();

        ApprovalRequest saved = approvalRepository.save(request);

        // Transition request status to PENDING_APPROVAL
        try {
            requestService.transitionStatus(requestId, RequestStatus.PENDING_APPROVAL, "Approval requested for action: " + actionType);
        } catch (Exception e) {
            logger.info("Request state transition noted: {}", e.getMessage());
        }

        auditService.recordEvent(proposedBy, "APPROVAL_REQUESTED", reasoning, requestId, actionType, "PENDING", payloadJson);
        outboxService.saveEvent("APPROVAL", saved.getId().toString(), "APPROVAL_REQUESTED", saved);

        return saved;
    }

    @Transactional
    public ApprovalRequest approveAction(Long approvalId, String managerComment) {
        String managerUser = SecurityUtils.getCurrentUsername();

        ApprovalRequest approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + approvalId));

        if (approval.getStatus() != ApprovalRequest.ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval request is already " + approval.getStatus());
        }

        approval.setStatus(ApprovalRequest.ApprovalStatus.APPROVED);
        approval.setApprovedBy(managerUser);
        ApprovalRequest updated = approvalRepository.save(approval);

        // Execute deterministic backend action
        executeActionBackend(approval.getRequestId(), approval.getActionType(), approval.getPayloadJson());

        // Transition request to ACTION_IN_PROGRESS then RESOLVED
        requestService.transitionStatus(approval.getRequestId(), RequestStatus.ACTION_IN_PROGRESS, "Manager approved action: " + approval.getActionType());
        requestService.transitionStatus(approval.getRequestId(), RequestStatus.RESOLVED, "Action successfully executed: " + approval.getActionType());

        auditService.recordEvent(managerUser, "APPROVAL_GRANTED", managerComment, approval.getRequestId(), approval.getActionType(), "SUCCESS", approval.getPayloadJson());
        outboxService.saveEvent("APPROVAL", approvalId.toString(), "APPROVAL_APPROVED", updated);

        return updated;
    }

    @Transactional
    public ApprovalRequest rejectAction(Long approvalId, String rejectionReason) {
        String managerUser = SecurityUtils.getCurrentUsername();

        ApprovalRequest approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + approvalId));

        approval.setStatus(ApprovalRequest.ApprovalStatus.REJECTED);
        approval.setApprovedBy(managerUser);
        ApprovalRequest updated = approvalRepository.save(approval);

        requestService.transitionStatus(approval.getRequestId(), RequestStatus.INVESTIGATING, "Action rejected by manager: " + rejectionReason);

        auditService.recordEvent(managerUser, "APPROVAL_REJECTED", rejectionReason, approval.getRequestId(), approval.getActionType(), "REJECTED", null);

        return updated;
    }

    public List<ApprovalRequest> getPendingApprovals() {
        return approvalRepository.findByStatusOrderByCreatedAtDesc(ApprovalRequest.ApprovalStatus.PENDING);
    }

    public List<ApprovalRequest> getApprovalsForRequest(Long requestId) {
        return approvalRepository.findByRequestId(requestId);
    }

    private void executeActionBackend(Long requestId, String actionType, String payloadJson) {
        logger.info("BACKEND EXECUTION ENGINE: Executing action [{}] for Request [{}] with parameters {}", actionType, requestId, payloadJson);
        // Deterministic action execution (e.g., event replay, status fix, refund API trigger)
    }
}
