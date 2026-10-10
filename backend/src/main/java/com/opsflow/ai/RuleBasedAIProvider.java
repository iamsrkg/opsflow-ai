package com.opsflow.ai;

import com.opsflow.knowledge.VectorStoreService;
import com.opsflow.request.Category;
import com.opsflow.request.Priority;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The default provider: no model, just rules. It classifies by keywords and recommends the
 * usual action for the category. Its answers are predictable, which makes the workflow around
 * it testable, and it is also what the model-backed provider falls back to when the model
 * can't be reached.
 */
@Component
public class RuleBasedAIProvider implements AIProvider {

    private record Rule(Category category, Priority priority, String summary, List<String> keywords) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule(Category.PAYMENT, Priority.HIGH, "Looks like a payment that was taken but not reflected.",
                    List.of("pay", "charged", "card", "debit", "refund")),
            new Rule(Category.BOOKING, Priority.HIGH, "Looks like a booking or reservation that failed.",
                    List.of("book", "flight", "hotel", "seat", "reservation")),
            new Rule(Category.DELIVERY, Priority.MEDIUM, "Looks like a delivery that did not arrive as recorded.",
                    List.of("deliver", "driver", "package", "parcel", "courier")),
            new Rule(Category.TECHNICAL, Priority.CRITICAL, "Looks like a service or API failure.",
                    List.of("timeout", "504", "500", "api", "outage", "down")),
            new Rule(Category.ORDER, Priority.MEDIUM, "Looks like an order whose status is wrong.",
                    List.of("order", "track", "item")));

    private final ActionPolicy policy;

    public RuleBasedAIProvider(ActionPolicy policy) {
        this.policy = policy;
    }

    @Override
    public String name() {
        return "rules";
    }

    @Override
    public ClassificationResult classify(String title, String description) {
        String text = (title + " " + description).toLowerCase(Locale.ROOT);

        Map<Rule, Integer> matches = new LinkedHashMap<>();
        for (Rule rule : RULES) {
            int hits = (int) rule.keywords().stream().filter(text::contains).count();
            if (hits > 0) {
                matches.put(rule, hits);
            }
        }
        if (matches.isEmpty()) {
            return ClassificationResult.builder()
                    .category(Category.OTHER).priority(Priority.MEDIUM)
                    .summary("No rule matched; a person should classify this.")
                    .confidence(0.3)
                    .build();
        }
        Map.Entry<Rule, Integer> best = matches.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
        int total = matches.values().stream().mapToInt(Integer::intValue).sum();
        // the share of keyword hits that belong to the winning category: 1.0 when nothing else matched
        double confidence = Math.round((0.5 + 0.5 * best.getValue() / total) * 100.0) / 100.0;
        return ClassificationResult.builder()
                .category(best.getKey().category()).priority(best.getKey().priority())
                .summary(best.getKey().summary())
                .confidence(confidence)
                .build();
    }

    @Override
    public Recommendation recommend(Evidence evidence) {
        Category category = evidence.request().getCategory();
        String action = policy.usualActionFor(category);
        VectorStoreService.SearchResult runbook = evidence.bestRunbook();

        String cause = runbook == null
                ? "No matching runbook, so the cause is not known yet."
                : "Most likely the situation described in \"" + runbook.getDocumentTitle() + "\".";
        // only as sure as the runbook match is close
        double confidence = runbook == null ? 0.3 : Math.round(Math.min(0.95, 0.4 + runbook.getScore()) * 100.0) / 100.0;

        return Recommendation.builder()
                .summary("A " + category + " request. The usual action for this category is " + action + ".")
                .likelyCause(cause)
                .recommendedAction(action)
                .confidence(confidence)
                .build();
    }
}
