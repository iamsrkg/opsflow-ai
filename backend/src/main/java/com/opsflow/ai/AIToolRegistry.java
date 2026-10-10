package com.opsflow.ai;

import com.opsflow.audit.AuditService;
import com.opsflow.knowledge.VectorStoreService;
import com.opsflow.request.Request;
import com.opsflow.request.RequestService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AIToolRegistry {

    private final RequestService requestService;
    private final AuditService auditService;
    private final VectorStoreService vectorStoreService;

    public AIToolRegistry(RequestService requestService, AuditService auditService, VectorStoreService vectorStoreService) {
        this.requestService = requestService;
        this.auditService = auditService;
        this.vectorStoreService = vectorStoreService;
    }

    public Request getRequestDetails(Long requestId) {
        return requestService.getRequestEntity(requestId);
    }

    public Map<String, Object> getAuditHistory(Long requestId) {
        Map<String, Object> map = new HashMap<>();
        map.put("events", auditService.getAuditTrailForRequest(requestId));
        map.put("history", requestService.getRequestHistory(requestId));
        return map;
    }

    public List<VectorStoreService.SearchResult> searchRunbooks(String query) {
        return vectorStoreService.searchVectorStore(query, 3);
    }

    public Map<String, Object> getPaymentStatus(String reference) {
        Map<String, Object> status = new HashMap<>();
        status.put("gatewayTxId", "TXN-99482104");
        status.put("authorizationStatus", "SUCCESS");
        status.put("gatewayTimestamp", "10:31:22");
        status.put("orderSyncState", "PENDING_LAG");
        status.put("consumerGroupLag", 142);
        return status;
    }

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("databaseStatus", "UP");
        health.put("redisCache", "UP");
        health.put("kafkaCluster", "HEALTHY");
        health.put("paymentSyncConsumerLag", "ELEVATED (142 msgs)");
        return health;
    }
}
