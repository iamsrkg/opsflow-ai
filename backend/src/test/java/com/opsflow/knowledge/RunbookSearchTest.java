package com.opsflow.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Search over the seeded runbooks, on the in-memory database. */
@SpringBootTest
class RunbookSearchTest {

    @Autowired
    private VectorStoreService search;

    @Test
    void aPaymentProblemFindsThePaymentRunbookFirst() {
        List<VectorStoreService.SearchResult> results = search.searchVectorStore("payment deducted but order status still pending", 3);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDocumentTitle().contains("Payment"), "got: " + results.get(0).getDocumentTitle());
        assertTrue(results.get(0).getScore() > 0 && results.get(0).getScore() <= 1);
    }

    @Test
    void aQueryThatSharesNothingWithAnyRunbookFindsNothing() {
        assertTrue(search.searchVectorStore("zzzz qqqq xxxx", 3).isEmpty());
    }
}
