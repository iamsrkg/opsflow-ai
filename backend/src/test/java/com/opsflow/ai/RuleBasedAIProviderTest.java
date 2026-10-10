package com.opsflow.ai;

import com.opsflow.request.Category;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedAIProviderTest {

    private final ActionPolicy policy = new ActionPolicy();
    private final RuleBasedAIProvider provider = new RuleBasedAIProvider(policy);

    @Test
    void keywordsDecideTheCategory() {
        assertEquals(Category.PAYMENT, provider.classify("Charged twice", "my card was debited two times").getCategory());
        assertEquals(Category.DELIVERY, provider.classify("Where is my parcel", "courier says delivered").getCategory());
        assertEquals(Category.TECHNICAL, provider.classify("504 from the API", "gateway timeout").getCategory());
    }

    /** The examples in the README's "What it is for" table. */
    @Test
    void theReadmeExamplesAreClassifiedAndGivenTheActionTheTableSays() {
        assertExample("Charged ₹500 but my order still says payment pending", Category.PAYMENT, "REPLAY_PAYMENT_EVENT");
        assertExample("Flight booking failed at checkout but my account was debited", Category.BOOKING, "RELEASE_INVENTORY_AND_REFUND");
        assertExample("Driver marked the order delivered, the customer never received it", Category.DELIVERY, "OPEN_COURIER_INVESTIGATION");
        assertExample("Our integration gets 504 Gateway Timeout from the API", Category.TECHNICAL, "ESCALATE_TO_ON_CALL");
    }

    private void assertExample(String title, Category category, String action) {
        assertEquals(category, provider.classify(title, "").getCategory(), title);
        assertEquals(action, policy.usualActionFor(category));
        assertTrue(policy.requiresApproval(action));
    }

    @Test
    void textNoRuleMatchesIsLeftForAPersonWithLowConfidence() {
        AIProvider.ClassificationResult result = provider.classify("Hello", "just saying hi");
        assertEquals(Category.OTHER, result.getCategory());
        assertTrue(result.getConfidence() < 0.5);
    }

    @Test
    void theModelCannotInventAnActionOrApproveItsOwn() {
        assertFalse(policy.isKnown("DELETE_ALL_ORDERS"));
        assertFalse(policy.isKnown(null));
        assertTrue(policy.requiresApproval("REPLAY_PAYMENT_EVENT"));
        assertFalse(policy.requiresApproval(ActionPolicy.REQUEST_MORE_INFORMATION));
    }
}
