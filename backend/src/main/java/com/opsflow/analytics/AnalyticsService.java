package com.opsflow.analytics;

import com.opsflow.ai.AIInvestigation;
import com.opsflow.ai.AIInvestigationRepository;
import com.opsflow.events.ProcessedEventRepository;
import com.opsflow.outbox.OutboxEvent;
import com.opsflow.outbox.OutboxRepository;
import com.opsflow.approval.ApprovalRepository;
import com.opsflow.approval.ApprovalRequest;
import com.opsflow.request.Category;
import com.opsflow.request.Priority;
import com.opsflow.request.RequestRepository;
import com.opsflow.request.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

@Service
public class AnalyticsService {

    private final RequestRepository requestRepository;
    private final ApprovalRepository approvalRepository;
    private final OutboxRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final AIInvestigationRepository investigationRepository;

    public AnalyticsService(RequestRepository requestRepository,
                            ApprovalRepository approvalRepository,
                            OutboxRepository outboxRepository,
                            ProcessedEventRepository processedEventRepository,
                            AIInvestigationRepository investigationRepository) {
        this.requestRepository = requestRepository;
        this.approvalRepository = approvalRepository;
        this.outboxRepository = outboxRepository;
        this.processedEventRepository = processedEventRepository;
        this.investigationRepository = investigationRepository;
    }

    private Double averageConfidence() {
        OptionalDouble average = investigationRepository.findAll().stream()
                .map(AIInvestigation::getConfidenceScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        return average.isPresent() ? Math.round(average.getAsDouble() * 100.0) / 100.0 : null;
    }

    public DashboardMetricsDTO getDashboardMetrics() {
        long total = requestRepository.count();
        long resolved = requestRepository.countByStatus(RequestStatus.RESOLVED) + requestRepository.countByStatus(RequestStatus.CLOSED);
        long pendingApprovalCount = approvalRepository.findByStatusOrderByCreatedAtDesc(ApprovalRequest.ApprovalStatus.PENDING).size();

        Map<Priority, Long> priorityDistribution = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) {
            priorityDistribution.put(p, requestRepository.countByPriority(p));
        }

        Map<RequestStatus, Long> statusDistribution = new EnumMap<>(RequestStatus.class);
        for (RequestStatus s : RequestStatus.values()) {
            statusDistribution.put(s, requestRepository.countByStatus(s));
        }

        Map<Category, Long> categoryDistribution = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            categoryDistribution.put(c, requestRepository.findAll().stream().filter(r -> r.getCategory() == c).count());
        }

        double resolutionRate = total > 0 ? ((double) resolved / total) * 100.0 : 100.0;

        return DashboardMetricsDTO.builder()
                .totalRequests(total)
                .activeRequests(total - resolved)
                .resolvedRequests(resolved)
                .pendingApprovals(pendingApprovalCount)
                .resolutionRatePercentage(Math.round(resolutionRate * 10.0) / 10.0)
                .outboxPending(outboxRepository.countByStatus(OutboxEvent.OutboxStatus.PENDING))
                .eventsPublished(outboxRepository.countByStatus(OutboxEvent.OutboxStatus.PUBLISHED))
                .eventsConsumed(processedEventRepository.count())
                .aiAvgConfidence(averageConfidence())
                .priorityDistribution(priorityDistribution)
                .statusDistribution(statusDistribution)
                .categoryDistribution(categoryDistribution)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardMetricsDTO {
        private long totalRequests;
        private long activeRequests;
        private long resolvedRequests;
        private long pendingApprovals;
        private double resolutionRatePercentage;
        /** Events written but not yet acknowledged by Kafka. */
        private long outboxPending;
        private long eventsPublished;
        /** Events a consumer has finished handling. */
        private long eventsConsumed;
        /** Average confidence of the stored investigations, or null if there are none yet. */
        private Double aiAvgConfidence;
        private Map<Priority, Long> priorityDistribution;
        private Map<RequestStatus, Long> statusDistribution;
        private Map<Category, Long> categoryDistribution;
    }
}
