package com.opsflow.ai;

import com.opsflow.audit.AuditEvent;
import com.opsflow.audit.AuditService;
import com.opsflow.knowledge.VectorStoreService;
import com.opsflow.outbox.OutboxEvent;
import com.opsflow.outbox.OutboxRepository;
import com.opsflow.request.Request;
import com.opsflow.request.RequestHistory;
import com.opsflow.request.RequestService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Everything the AI step is allowed to look at. It has no repository and no SQL of its own:
 * if a lookup isn't a method here, the investigation can't do it. All of these only read.
 */
@Component
public class AIToolRegistry {

    private final RequestService requestService;
    private final AuditService auditService;
    private final VectorStoreService vectorStoreService;
    private final OutboxRepository outboxRepository;

    public AIToolRegistry(RequestService requestService, AuditService auditService,
                          VectorStoreService vectorStoreService, OutboxRepository outboxRepository) {
        this.requestService = requestService;
        this.auditService = auditService;
        this.vectorStoreService = vectorStoreService;
        this.outboxRepository = outboxRepository;
    }

    public Request getRequestDetails(Long requestId) {
        return requestService.getRequestEntity(requestId);
    }

    public List<AuditEvent> getAuditTrail(Long requestId) {
        return auditService.getAuditTrailForRequest(requestId);
    }

    public List<RequestHistory> getStatusHistory(Long requestId) {
        return requestService.getRequestHistory(requestId);
    }

    public List<VectorStoreService.SearchResult> searchRunbooks(String query) {
        return vectorStoreService.searchVectorStore(query, 3);
    }

    /** The events this request has produced, and whether Kafka has acknowledged them. */
    public List<OutboxEvent> getEvents(Long requestId) {
        return outboxRepository.findByAggregateTypeAndAggregateId("REQUEST", String.valueOf(requestId));
    }
}
