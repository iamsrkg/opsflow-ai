package com.opsflow.ai;

import com.opsflow.request.Category;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * The actions a recommendation may name, and which of them need a manager first.
 *
 * This is decided here, in code, and never by the AI provider: a model can suggest an action
 * but it can't invent a new one and it can't mark its own suggestion as safe to run.
 */
@Component
public class ActionPolicy {

    public static final String NEEDS_HUMAN_REVIEW = "NEEDS_HUMAN_REVIEW";
    public static final String REQUEST_MORE_INFORMATION = "REQUEST_MORE_INFORMATION";

    /** Actions that change something outside this system: money, stock, a courier, a pager. */
    private static final Set<String> NEEDS_APPROVAL = Set.of(
            "REPLAY_PAYMENT_EVENT", "RELEASE_INVENTORY_AND_REFUND", "OPEN_COURIER_INVESTIGATION", "ESCALATE_TO_ON_CALL");

    private static final Set<String> NO_APPROVAL = Set.of(REQUEST_MORE_INFORMATION, NEEDS_HUMAN_REVIEW);

    private static final Map<Category, String> USUAL_ACTION = Map.of(
            Category.PAYMENT, "REPLAY_PAYMENT_EVENT",
            Category.BOOKING, "RELEASE_INVENTORY_AND_REFUND",
            Category.DELIVERY, "OPEN_COURIER_INVESTIGATION",
            Category.TECHNICAL, "ESCALATE_TO_ON_CALL");

    public boolean isKnown(String action) {
        return action != null && (NEEDS_APPROVAL.contains(action) || NO_APPROVAL.contains(action));
    }

    public boolean requiresApproval(String action) {
        return NEEDS_APPROVAL.contains(action);
    }

    public String usualActionFor(Category category) {
        return USUAL_ACTION.getOrDefault(category, REQUEST_MORE_INFORMATION);
    }

    /** What each action means, in the words given to a language model so it can choose between them. */
    public Map<String, String> describedActions() {
        Map<String, String> actions = new java.util.LinkedHashMap<>();
        actions.put("REPLAY_PAYMENT_EVENT", "the payment succeeded at the gateway but the order or account was not updated; send the payment event again");
        actions.put("RELEASE_INVENTORY_AND_REFUND", "a booking or reservation failed or was double-allocated after the customer paid; free the seat, room or stock and refund");
        actions.put("OPEN_COURIER_INVESTIGATION", "a parcel is missing, misrouted, damaged, or marked delivered but not received; open a case with the courier");
        actions.put("ESCALATE_TO_ON_CALL", "a technical fault in our own systems (timeouts, errors, outages, lag); page the on-call engineer");
        actions.put(REQUEST_MORE_INFORMATION, "the report does not say enough to decide; ask the customer for details");
        actions.put(NEEDS_HUMAN_REVIEW, "none of the above fits");
        return actions;
    }

    public Set<String> allActions() {
        return Set.of("REPLAY_PAYMENT_EVENT", "RELEASE_INVENTORY_AND_REFUND", "OPEN_COURIER_INVESTIGATION",
                "ESCALATE_TO_ON_CALL", REQUEST_MORE_INFORMATION, NEEDS_HUMAN_REVIEW);
    }
}
