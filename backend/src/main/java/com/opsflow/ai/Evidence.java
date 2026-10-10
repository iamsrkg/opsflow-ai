package com.opsflow.ai;

import com.opsflow.knowledge.VectorStoreService;
import com.opsflow.request.Request;

import java.util.ArrayList;
import java.util.List;

/**
 * What was found out about a request before any recommendation is made. Every line comes from
 * the application's own data, so the evidence shown to the approving manager is fact, whichever
 * provider writes the recommendation.
 */
public record Evidence(Request request,
                       int auditEvents,
                       int statusChanges,
                       long eventsWaiting,
                       long eventsPublished,
                       List<VectorStoreService.SearchResult> runbooks) {

    public VectorStoreService.SearchResult bestRunbook() {
        return runbooks.isEmpty() ? null : runbooks.get(0);
    }

    public List<String> asLines() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("Request %s is %s, category %s, priority %s.",
                request.getExternalReference(), request.getStatus(), request.getCategory(), request.getPriority()));
        lines.add(String.format("It has %d audit entries and has changed status %d times.", auditEvents, statusChanges));
        lines.add(String.format("Its events: %d acknowledged by Kafka, %d still waiting in the outbox.", eventsPublished, eventsWaiting));
        if (runbooks.isEmpty()) {
            lines.add("No runbook is close enough to this request.");
        } else {
            for (VectorStoreService.SearchResult runbook : runbooks) {
                lines.add(String.format("Runbook \"%s\" matches with similarity %.2f.", runbook.getDocumentTitle(), runbook.getScore()));
            }
        }
        return lines;
    }
}
