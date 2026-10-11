package com.opsflow.knowledge;

/**
 * Turns text into a vector. Two implementations: feature hashing (the default, no network)
 * and an embedding model behind an OpenAI-compatible API.
 */
public interface TextEmbedder {

    /** Stored with each embedding, so vectors made by a different embedder are replaced, not compared. */
    String name();

    int dimensions();

    /** Below this similarity a runbook is not close enough to the query to be worth showing. */
    double minimumScore();

    /** A vector of length 1, so the dot product of two of them is their cosine similarity. */
    float[] embed(String text);
}
