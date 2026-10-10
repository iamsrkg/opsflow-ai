package com.opsflow.knowledge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Used with the in-memory database. Embeddings are saved as text on the chunk row and every
 * search compares the query with all of them, which is fine for a handful of runbooks.
 */
@Component
@ConditionalOnProperty(name = "opsflow.vector.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryVectorIndex implements VectorIndex {

    private final KnowledgeChunkRepository chunkRepository;

    public InMemoryVectorIndex(KnowledgeChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    @Override
    public void store(long chunkId, float[] embedding) {
        chunkRepository.findById(chunkId).ifPresent(chunk -> {
            chunk.setVectorEmbedding(Vectors.toText(embedding));
            chunkRepository.save(chunk);
        });
    }

    @Override
    public List<Hit> nearest(float[] query, int limit) {
        return chunkRepository.findAll().stream()
                .filter(chunk -> chunk.getVectorEmbedding() != null)
                .map(chunk -> new Hit(chunk.getId(), EmbeddingService.cosine(query, Vectors.fromText(chunk.getVectorEmbedding()))))
                .sorted(Comparator.comparingDouble(Hit::score).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<Long> chunksWithoutEmbedding() {
        return chunkRepository.findAll().stream()
                .filter(chunk -> chunk.getVectorEmbedding() == null)
                .map(KnowledgeChunk::getId)
                .toList();
    }
}
