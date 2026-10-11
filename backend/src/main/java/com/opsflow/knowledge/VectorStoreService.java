package com.opsflow.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Runbook search: the query is embedded and compared with the stored chunk embeddings. */
@Service
public class VectorStoreService {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final TextEmbedder embedder;
    private final VectorIndex vectorIndex;

    public VectorStoreService(KnowledgeChunkRepository chunkRepository,
                              KnowledgeDocumentRepository documentRepository,
                              TextEmbedder embedder,
                              VectorIndex vectorIndex) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.embedder = embedder;
        this.vectorIndex = vectorIndex;
    }

    @Transactional(readOnly = true)
    public List<SearchResult> searchVectorStore(String query, int topK) {
        float[] queryVector;
        try {
            queryVector = embedder.embed(query);
        } catch (RuntimeException e) {
            // an embedding model that is down must not take investigations down with it
            logger.warn("Runbook search is unavailable, {} could not embed the query: {}", embedder.name(), e.getMessage());
            return List.of();
        }
        List<VectorIndex.Hit> hits = vectorIndex.nearest(queryVector, topK);

        Map<Long, KnowledgeChunk> chunks = chunkRepository.findAllById(hits.stream().map(VectorIndex.Hit::chunkId).toList())
                .stream().collect(Collectors.toMap(KnowledgeChunk::getId, Function.identity()));
        Map<Long, KnowledgeDocument> documents = documentRepository.findAllById(
                        chunks.values().stream().map(KnowledgeChunk::getDocumentId).distinct().toList())
                .stream().collect(Collectors.toMap(KnowledgeDocument::getId, Function.identity()));

        List<SearchResult> results = new ArrayList<>();
        for (VectorIndex.Hit hit : hits) {
            KnowledgeChunk chunk = chunks.get(hit.chunkId());
            if (chunk == null || hit.score() < embedder.minimumScore()) {
                continue;
            }
            KnowledgeDocument document = documents.get(chunk.getDocumentId());
            results.add(new SearchResult(chunk.getId(), chunk.getDocumentId(),
                    document != null ? document.getTitle() : "Runbook",
                    chunk.getText(),
                    Math.round(hit.score() * 100.0) / 100.0));
        }
        return results;
    }

    /**
     * Gives every chunk that has none an embedding. Runs after the seed data is in and before the
     * application reports itself ready, so a search never runs against a half-built index. Not
     * one transaction: with an embedding model each chunk is a network call, and each stored
     * vector should be usable as soon as it is written.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void indexMissingChunks() {
        List<Long> missing = vectorIndex.chunksWithoutEmbedding();
        try {
            for (KnowledgeChunk chunk : chunkRepository.findAllById(missing)) {
                KnowledgeDocument document = documentRepository.findById(chunk.getDocumentId()).orElse(null);
                // the title and tags describe the runbook better than any one paragraph, so they are embedded with it
                String text = document == null ? chunk.getText()
                        : document.getTitle() + ". " + document.getTags() + ". " + chunk.getText();
                vectorIndex.store(chunk.getId(), embedder.embed(text));
            }
        } catch (RuntimeException e) {
            // the application still starts; search finds nothing for these chunks until the next start
            logger.error("Could not embed the runbooks with {}: {}", embedder.name(), e.getMessage());
            return;
        }
        if (!missing.isEmpty()) {
            logger.info("Embedded {} runbook chunks with {}", missing.size(), embedder.name());
        }
    }

    public static class SearchResult {
        private final Long chunkId;
        private final Long documentId;
        private final String documentTitle;
        private final String snippet;
        private final double score;

        public SearchResult(Long chunkId, Long documentId, String documentTitle, String snippet, double score) {
            this.chunkId = chunkId;
            this.documentId = documentId;
            this.documentTitle = documentTitle;
            this.snippet = snippet;
            this.score = score;
        }

        public Long getChunkId() { return chunkId; }
        public Long getDocumentId() { return documentId; }
        public String getDocumentTitle() { return documentTitle; }
        public String getSnippet() { return snippet; }
        public double getScore() { return score; }
    }
}
