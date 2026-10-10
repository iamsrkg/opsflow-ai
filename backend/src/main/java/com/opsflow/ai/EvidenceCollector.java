package com.opsflow.ai;

import com.opsflow.outbox.OutboxEvent;
import com.opsflow.request.Request;
import org.springframework.stereotype.Component;

import java.util.List;

/** Gathers the evidence for a request, using only the tools in the registry. */
@Component
public class EvidenceCollector {

    private final AIToolRegistry tools;

    public EvidenceCollector(AIToolRegistry tools) {
        this.tools = tools;
    }

    public Evidence collect(Long requestId) {
        Request request = tools.getRequestDetails(requestId);
        List<OutboxEvent> events = tools.getEvents(requestId);
        long published = events.stream().filter(e -> e.getStatus() == OutboxEvent.OutboxStatus.PUBLISHED).count();
        return new Evidence(
                request,
                tools.getAuditTrail(requestId).size(),
                tools.getStatusHistory(requestId).size(),
                events.size() - published,
                published,
                tools.searchRunbooks(request.getTitle() + " " + request.getDescription()));
    }
}
