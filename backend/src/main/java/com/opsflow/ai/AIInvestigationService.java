package com.opsflow.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Set;

@Service
public class AIInvestigationService {

    /** From these states an investigation can start. */
    private static final Set<RequestStatus> CAN_START_INVESTIGATION =
            Set.of(RequestStatus.CLASSIFIED, RequestStatus.ASSIGNED, RequestStatus.ESCALATED, RequestStatus.WAITING_FOR_USER);

    private final RequestRepository requestRepository;
    private final RequestService requestService;
    private final AIProvider aiProvider;
    private final EvidenceCollector evidenceCollector;
    private final ActionPolicy actionPolicy;
    private final AIInvestigationRepository investigationRepository;
    private final ApprovalService approvalService;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public AIInvestigationService(RequestRepository requestRepository,
                                  RequestService requestService,
                                  AIProvider aiProvider,
                                  EvidenceCollector evidenceCollector,
                                  ActionPolicy actionPolicy,
                                  AIInvestigationRepository investigationRepository,
                                  ApprovalService approvalService,
                                  AuditService auditService,
                                  OutboxService outboxService,
                                  ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.requestService = requestService;
        this.aiProvider = aiProvider;
        this.evidenceCollector = evidenceCollector;
        this.actionPolicy = actionPolicy;
        this.investigationRepository = investigationRepository;
        this.approvalService = approvalService;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Request classifyRequest(Long requestId) {
        Request request = find(requestId);

        AIProvider.ClassificationResult result = aiProvider.classify(request.getTitle(), request.getDescription());
        request.setCategory(result.getCategory());
        request.setPriority(result.getPriority());
        request.setClassificationSummary(result.getSummary());
        request.setConfidenceScore(result.getConfidence());
        Request updated = requestRepository.save(request);

        if (request.getStatus() == RequestStatus.CREATED) {
            requestService.transitionStatus(requestId, RequestStatus.CLASSIFIED,
                    "Classified by " + aiProvider.name() + " with confidence " + Math.round(result.getConfidence() * 100) + "%");
        }
        auditService.recordEvent(aiProvider.name(), "AI_CLASSIFICATION", result.getSummary(), requestId, "CLASSIFY", "SUCCESS", null);
        outboxService.saveEvent("REQUEST", requestId.toString(), "REQUEST_CLASSIFIED", updated);
        return updated;
    }

    /**
     * Collects the evidence, asks the provider for a recommendation, and stores both. If the
     * recommended action is one that changes something, it becomes an approval request and
     * waits for a manager; nothing is executed here.
     */
    @Transactional
    public AIInvestigation investigateRequest(Long requestId) {
        String currentUser = SecurityUtils.getCurrentUsername();

        // a request that was never classified (new, or assigned by hand straight away) is classified first:
        // the recommendation depends on the category, and a new request may not skip a workflow step
        if (find(requestId).getClassificationSummary() == null) {
            classifyRequest(requestId);
        }
        if (CAN_START_INVESTIGATION.contains(find(requestId).getStatus())) {
            requestService.transitionStatus(requestId, RequestStatus.INVESTIGATING, "Investigation started by " + currentUser);
        }

        Evidence evidence = evidenceCollector.collect(requestId);
        AIProvider.Recommendation recommendation = aiProvider.recommend(evidence);

        // the provider suggests; the policy decides what is allowed and what needs a manager
        String action = actionPolicy.isKnown(recommendation.getRecommendedAction())
                ? recommendation.getRecommendedAction()
                : ActionPolicy.NEEDS_HUMAN_REVIEW;
        boolean needsApproval = actionPolicy.requiresApproval(action);
        String evidenceJson = toJson(evidence.asLines());

        AIInvestigation saved = investigationRepository.save(AIInvestigation.builder()
                .requestId(requestId)
                .investigatedBy(currentUser + " via " + aiProvider.name())
                .summary(recommendation.getSummary())
                .evidenceJson(evidenceJson)
                .likelyCause(recommendation.getLikelyCause())
                .confidenceScore(recommendation.getConfidence())
                .recommendedAction(action)
                .requiresApproval(needsApproval)
                .relevantRunbookTitle(evidence.bestRunbook() == null ? null : evidence.bestRunbook().getDocumentTitle())
                .build());

        if (needsApproval) {
            approvalService.createApprovalRequest(
                    requestId,
                    action,
                    "Recommended by " + aiProvider.name() + " with confidence " + Math.round(recommendation.getConfidence() * 100)
                            + "%. " + recommendation.getLikelyCause(),
                    String.format("{\"recommendedAction\":\"%s\",\"requestId\":%d}", action, requestId));
        }

        auditService.recordEvent(currentUser, "AI_INVESTIGATION_COMPLETED", recommendation.getSummary(), requestId,
                "INVESTIGATE_AI", "SUCCESS", evidenceJson);
        outboxService.saveEvent("INVESTIGATION", saved.getId().toString(), "INVESTIGATION_COMPLETED", saved);
        return saved;
    }

    public List<AIInvestigation> getInvestigationsForRequest(Long requestId) {
        return investigationRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
    }

    private Request find(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));
    }

    private String toJson(List<String> lines) {
        try {
            return objectMapper.writeValueAsString(lines);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
