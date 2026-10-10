package com.opsflow.ai;

import com.opsflow.knowledge.VectorStoreService;
import com.opsflow.request.Category;
import com.opsflow.request.Priority;
import com.opsflow.request.Request;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MockAIProvider implements AIProvider {

    private final AIToolRegistry toolRegistry;

    public MockAIProvider(AIToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public ClassificationResult classify(String title, String description) {
        String combined = (title + " " + description).toLowerCase();

        Category category = Category.OTHER;
        Priority priority = Priority.MEDIUM;
        String summary = "Automated classification of operational request.";
        double confidence = 0.92;

        if (combined.contains("pay") || combined.contains("charged") || combined.contains("card") || combined.contains("debit")) {
            category = Category.PAYMENT;
            priority = Priority.HIGH;
            summary = "Payment authorization or status synchronization issue detected.";
            confidence = 0.95;
        } else if (combined.contains("book") || combined.contains("flight") || combined.contains("hotel")) {
            category = Category.BOOKING;
            priority = Priority.HIGH;
            summary = "Booking inventory reservation failure.";
            confidence = 0.91;
        } else if (combined.contains("delivery") || combined.contains("driver") || combined.contains("package")) {
            category = Category.DELIVERY;
            priority = Priority.MEDIUM;
            summary = "Order delivery exception or driver status mismatch.";
            confidence = 0.88;
        } else if (combined.contains("order") || combined.contains("track") || combined.contains("item")) {
            category = Category.ORDER;
            priority = Priority.MEDIUM;
            summary = "Order fulfillment or status discrepancy.";
            confidence = 0.89;
        } else if (combined.contains("down") || combined.contains("api") || combined.contains("error") || combined.contains("fail")) {
            category = Category.TECHNICAL;
            priority = Priority.CRITICAL;
            summary = "Technical service degradation or API endpoint failure.";
            confidence = 0.96;
        }

        return ClassificationResult.builder()
                .category(category)
                .priority(priority)
                .summary(summary)
                .confidence(confidence)
                .build();
    }

    @Override
    public InvestigationResult investigate(Long requestId, String title, String description) {
        // Step 1: Tool invocation - Fetch Request & Audit history
        Request req = toolRegistry.getRequestDetails(requestId);
        Map<String, Object> audit = toolRegistry.getAuditHistory(requestId);

        // Step 2: Tool invocation - Search Runbooks via Vector Store
        List<VectorStoreService.SearchResult> runbooks = toolRegistry.searchRunbooks(title + " " + description);
        String topRunbook = !runbooks.isEmpty() ? runbooks.get(0).getDocumentTitle() : "Payment Synchronization Troubleshooting";

        // Step 3: Tool invocation - Check mock payment/system status
        Map<String, Object> paymentStatus = toolRegistry.getPaymentStatus(req.getExternalReference());

        List<String> evidence = new ArrayList<>();
        evidence.add(String.format("Payment authorization TXN-99482104 succeeded at 10:31:22 in Payment Gateway."));
        evidence.add(String.format("Order status remained PENDING at 10:31:25 due to Kafka consumer group lag."));
        evidence.add(String.format("Kafka consumer lag observed on topic 'opsflow.payment.events' (142 uncommitted messages)."));
        evidence.add(String.format("Matched Runbook '%s' with 94%% vector similarity score.", topRunbook));

        return InvestigationResult.builder()
                .summary("Investigation indicates payment authorization completed successfully at gateway, but downstream order status synchronization was delayed by Kafka consumer group lag.")
                .evidence(evidence)
                .likelyCause("Payment-to-order event synchronization delay due to temporary Kafka consumer lag.")
                .confidence(0.89)
                .recommendedAction("REPLAY_PAYMENT_EVENT")
                .requiresApproval(true)
                .relevantRunbook(topRunbook)
                .build();
    }
}
