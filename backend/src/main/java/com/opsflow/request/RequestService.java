package com.opsflow.request;

import com.opsflow.audit.AuditService;
import com.opsflow.outbox.OutboxService;
import com.opsflow.request.dto.RequestDTOs.*;
import com.opsflow.shared.SecurityUtils;
import com.opsflow.shared.exceptions.ResourceNotFoundException;
import com.opsflow.workflow.WorkflowStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RequestService {

    private static final Logger logger = LoggerFactory.getLogger(RequestService.class);

    private final RequestRepository requestRepository;
    private final RequestHistoryRepository historyRepository;
    private final WorkflowStateMachine stateMachine;
    private final AuditService auditService;
    private final OutboxService outboxService;

    public RequestService(RequestRepository requestRepository,
                          RequestHistoryRepository historyRepository,
                          WorkflowStateMachine stateMachine,
                          AuditService auditService,
                          OutboxService outboxService) {
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.stateMachine = stateMachine;
        this.auditService = auditService;
        this.outboxService = outboxService;
    }

    @Transactional
    public RequestResponseDTO createRequest(CreateRequestDTO dto) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        String externalRef = "REQ-" + System.currentTimeMillis() % 1000000 + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Request request = Request.builder()
                .externalReference(externalRef)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory() != null ? dto.getCategory() : Category.OTHER)
                .priority(dto.getPriority() != null ? dto.getPriority() : Priority.MEDIUM)
                .status(RequestStatus.CREATED)
                .createdBy(currentUsername)
                .build();

        Request saved = requestRepository.save(request);

        // Record initial history
        recordHistory(saved.getId(), null, RequestStatus.CREATED, currentUsername, "Request created");

        // Record Audit Event
        auditService.recordEvent(currentUsername, "REQUEST_CREATED", dto.getTitle(), saved.getId(),
                "CREATE_REQUEST", "SUCCESS", String.format("{\"externalRef\":\"%s\"}", externalRef));

        // Save Transactional Outbox Event
        outboxService.saveEvent("REQUEST", saved.getId().toString(), "REQUEST_CREATED", saved);

        logger.info("Request [{}] created by {}", externalRef, currentUsername);
        return mapToDTO(saved);
    }

    @Transactional
    public RequestResponseDTO transitionStatus(Long id, RequestStatus targetStatus, String comment) {
        Request request = getRequestEntity(id);
        RequestStatus currentStatus = request.getStatus();
        String currentUsername = SecurityUtils.getCurrentUsername();

        // Validate Workflow State Machine
        stateMachine.validateTransition(currentStatus, targetStatus);

        request.setStatus(targetStatus);
        if (targetStatus == RequestStatus.RESOLVED || targetStatus == RequestStatus.CLOSED) {
            request.setResolvedAt(LocalDateTime.now());
        }

        Request updated = requestRepository.save(request);

        recordHistory(id, currentStatus, targetStatus, currentUsername, comment);

        auditService.recordEvent(currentUsername, "STATUS_TRANSITION",
                String.format("Transitioned status from %s to %s. Comment: %s", currentStatus, targetStatus, comment),
                id, "STATUS_TRANSITION", "SUCCESS",
                String.format("{\"from\":\"%s\",\"to\":\"%s\"}", currentStatus, targetStatus));

        outboxService.saveEvent("REQUEST", id.toString(), "REQUEST_TRANSITIONED", updated);

        return mapToDTO(updated);
    }

    @Transactional
    public RequestResponseDTO assignRequest(Long id, String assignToUser) {
        Request request = getRequestEntity(id);
        String currentUsername = SecurityUtils.getCurrentUsername();

        request.setAssignedTo(assignToUser);
        if (request.getStatus() == RequestStatus.CREATED || request.getStatus() == RequestStatus.CLASSIFIED) {
            request.setStatus(RequestStatus.ASSIGNED);
        }

        Request updated = requestRepository.save(request);

        recordHistory(id, request.getStatus(), request.getStatus(), currentUsername, "Assigned request to " + assignToUser);

        auditService.recordEvent(currentUsername, "REQUEST_ASSIGNED", "Assigned to " + assignToUser, id, "ASSIGN", "SUCCESS", null);
        outboxService.saveEvent("REQUEST", id.toString(), "REQUEST_ASSIGNED", updated);

        return mapToDTO(updated);
    }

    @Transactional
    public RequestResponseDTO escalateRequest(Long id, String reason) {
        Request request = getRequestEntity(id);
        RequestStatus previousStatus = request.getStatus();
        String currentUsername = SecurityUtils.getCurrentUsername();

        stateMachine.validateTransition(previousStatus, RequestStatus.ESCALATED);

        request.setStatus(RequestStatus.ESCALATED);
        request.setPriority(Priority.CRITICAL);

        Request updated = requestRepository.save(request);

        recordHistory(id, previousStatus, RequestStatus.ESCALATED, currentUsername, "Escalated: " + reason);

        auditService.recordEvent(currentUsername, "REQUEST_ESCALATED", reason, id, "ESCALATE", "SUCCESS", null);
        outboxService.saveEvent("REQUEST", id.toString(), "REQUEST_ESCALATED", updated);

        return mapToDTO(updated);
    }

    @Transactional(readOnly = true)
    public Page<RequestResponseDTO> searchRequests(RequestStatus status, Category category, Priority priority, String query, Pageable pageable) {
        return requestRepository.searchRequests(status, category, priority, query, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public RequestResponseDTO getRequestById(Long id) {
        return mapToDTO(getRequestEntity(id));
    }

    @Transactional(readOnly = true)
    public List<RequestHistory> getRequestHistory(Long requestId) {
        return historyRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
    }

    public Request getRequestEntity(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));
    }

    private void recordHistory(Long requestId, RequestStatus prev, RequestStatus next, String user, String comment) {
        RequestHistory history = RequestHistory.builder()
                .requestId(requestId)
                .previousStatus(prev)
                .newStatus(next)
                .changedBy(user)
                .comment(comment)
                .build();
        historyRepository.save(history);
    }

    public RequestResponseDTO mapToDTO(Request r) {
        return RequestResponseDTO.builder()
                .id(r.getId())
                .externalReference(r.getExternalReference())
                .title(r.getTitle())
                .description(r.getDescription())
                .category(r.getCategory())
                .priority(r.getPriority())
                .status(r.getStatus())
                .createdBy(r.getCreatedBy())
                .assignedTo(r.getAssignedTo())
                .classificationSummary(r.getClassificationSummary())
                .confidenceScore(r.getConfidenceScore())
                .version(r.getVersion())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .resolvedAt(r.getResolvedAt())
                .build();
    }
}
