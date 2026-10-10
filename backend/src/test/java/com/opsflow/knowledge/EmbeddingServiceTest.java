package com.opsflow.knowledge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingServiceTest {

    private final EmbeddingService embeddings = new EmbeddingService();

    @Test
    void theSameTextAlwaysGivesTheSameVector() {
        assertArrayEquals(embeddings.embed("payment deducted, order pending"), embeddings.embed("payment deducted, order pending"));
        assertEquals(EmbeddingService.DIMENSIONS, embeddings.embed("anything").length);
    }

    @Test
    void aTextIsCloserToARelatedTextThanToAnUnrelatedOne() {
        float[] query = embeddings.embed("customer payment was deducted but the order is still pending");
        float[] payments = embeddings.embed("Payment synchronization: payment deducted while the order status remains pending");
        float[] delivery = embeddings.embed("Courier marked the parcel delivered but the recipient never received it");

        assertTrue(EmbeddingService.cosine(query, payments) > EmbeddingService.cosine(query, delivery));
    }

    @Test
    void vectorsAreNormalisedSoATextMatchesItselfWithScoreOne() {
        float[] vector = embeddings.embed("kafka consumer lag and outbox recovery");
        assertEquals(1.0, EmbeddingService.cosine(vector, vector), 1e-5);
    }

    @Test
    void aVectorSurvivesBeingWrittenAsTextAndReadBack() {
        float[] vector = embeddings.embed("inventory lock timeout");
        assertArrayEquals(vector, Vectors.fromText(Vectors.toText(vector)));
    }
}
