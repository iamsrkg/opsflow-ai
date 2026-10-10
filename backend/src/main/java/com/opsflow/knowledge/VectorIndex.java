package com.opsflow.knowledge;

import java.util.List;

/** Where chunk embeddings are kept and searched. One implementation per database. */
public interface VectorIndex {

    void store(long chunkId, float[] embedding);

    /** The chunks closest to the query vector, best first. */
    List<Hit> nearest(float[] query, int limit);

    /** Chunks that have no embedding yet. */
    List<Long> chunksWithoutEmbedding();

    record Hit(long chunkId, double score) {
    }
}
