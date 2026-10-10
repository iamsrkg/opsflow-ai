package com.opsflow.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.approval.ApprovalService;
import com.opsflow.audit.AuditService;
import com.opsflow.outbox.OutboxService;
import com.opsflow.request.Request;
import com.opsflow.request.RequestRepository;
import com.opsflow.request.RequestService;
import com.opsflow.request.RequestStatus;
import com.opsflow.shared.SecurityUtils;
import com.opsflow.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AIInvestigationService {

    private final AIProvider aiProvider;
    private final RequestRepository requestRepository;
    private final RequestService requestService;
    private final AIInvestigationRepository investigationRepository;
    private final ApprovalService approvalService;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public AIInvestigationService(AIProvider aiProvider,
                                  RequestRepository requestRepository,
                                  RequestService requestService,
                                  AIInvestigationRepository investigationRepository,
                                  ApprovalService approvalService,
                                  AuditService auditService,
                                  OutboxService outboxService,
                                  ObjectMapper objectMapper) {
        this.aiProvider = aiProvider;
        this.requestRepository = requestRepository;
        this.requestService = requestService;
        this.investigationRepository = investigationRepository;
        this.approvalService = approvalService;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Request classifyRequest(Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        AIProvider.ClassificationResult result = aiProvider.classify(request.getTitle(), request.getDescription());

        request.setCategory(result.getCategory());
        request.setPriority(result.getPriority());
        request.setClassificationSummary(result.getSummary());
        request.setConfidenceScore(result.getConfidence());

        Request updated = requestRepository.save(request);

        if (request.getStatus() == RequestStatus.CREATED) {
            requestService.transitionStatus(requestId, RequestStatus.CLASSIFIED, "AI Classification applied with confidence " + (int)(result.getConfidence() * 100) + "%");
        }

        auditService.recordEvent("ai-assistant", "AI_CLASSIFICATION", result.getSummary(), requestId, "CLASSIFY", "SUCCESS", null);
        outboxService.saveEvent("REQUEST", requestId.toString(), "REQUEST_CLASSIFIED", updated);

        return updated;
    }

    @Transactional
    public AIInvestigation investigateRequest(Long requestId) {
        String currentUser = SecurityUtils.getCurrentUsername();
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        // Transition status to INVESTIGATING if needed
        if (request.getStatus() == RequestStatus.CREATED || request.getStatus() == RequestStatus.CLASSIFIED || request.getStatus() == RequestStatus.ASSIGNED) {
            requestService.transitionStatus(requestId, RequestStatus.INVESTIGATING, "AI Investigation initiated by " + currentUser);
        }

        AIProvider.InvestigationResult result = aiProvider.investigate(requestId, request.getTitle(), request.getDescription());

        String evidenceJson;
        try {
            evidenceJson = objectMapper.writeValueAsString(result.getEvidence());
        } catch (Exception e) {
            evidenceJson = "[]";
        }

        AIInvestigation investigation = AIInvestigation.builder()
                .requestId(requestId)
                .investigatedBy(currentUser)
                .summary(result.getSummary())
                .evidenceJson(evidenceJson)
                .likelyCause(result.getLikelyCause())
                .confidenceScore(result.getConfidence())
                .recommendedAction(result.getRecommendedAction())
                .requiresApproval(result.isRequiresApproval())
                .relevantRunbookTitle(result.getRelevantRunbook())
                .build();

        AIInvestigation saved = investigationRepository.save(investigation);

        // If action requires approval, create Human-in-the-Loop Approval Request
        if (result.isRequiresApproval()) {
            approvalService.createApprovalRequest(
                    requestId,
                    result.getRecommendedAction(),
                    "AI Investigation recommended action: " + result.getRecommendedAction() + " (Confidence: " + (int)(result.getConfidence() * 100) + "%). Reason: " + result.getLikelyCause(),
                    String.format("{\"recommendedAction\":\"%s\",\"requestId\":%d}", result.getRecommendedAction(), requestId)
            );
        }

        auditService.recordEvent(currentUser, "AI_INVESTIGATION_COMPLETED", result.getSummary(), requestId, "INVESTIGATE_AI", "SUCCESS", evidenceJson);
        outboxService.saveEvent("INVESTIGATION", saved.getId().toString(), "INVESTIGATION_COMPLETED", saved);

        return saved;
    }

    public List<AIInvestigation> getInvestigationsForRequest(Long requestId) {
        return investigationRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
    }
}
