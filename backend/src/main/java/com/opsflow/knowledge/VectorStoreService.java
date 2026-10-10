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
    /** Below this similarity a chunk shares too little with the query to be worth showing. */
    private static final double MINIMUM_SCORE = 0.05;

    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final EmbeddingService embeddingService;
    private final VectorIndex vectorIndex;

    public VectorStoreService(KnowledgeChunkRepository chunkRepository,
                              KnowledgeDocumentRepository documentRepository,
                              EmbeddingService embeddingService,
                              VectorIndex vectorIndex) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.embeddingService = embeddingService;
        this.vectorIndex = vectorIndex;
    }

    @Transactional(readOnly = true)
    public List<SearchResult> searchVectorStore(String query, int topK) {
        List<VectorIndex.Hit> hits = vectorIndex.nearest(embeddingService.embed(query), topK);

        Map<Long, KnowledgeChunk> chunks = chunkRepository.findAllById(hits.stream().map(VectorIndex.Hit::chunkId).toList())
                .stream().collect(Collectors.toMap(KnowledgeChunk::getId, Function.identity()));
        Map<Long, KnowledgeDocument> documents = documentRepository.findAllById(
                        chunks.values().stream().map(KnowledgeChunk::getDocumentId).distinct().toList())
                .stream().collect(Collectors.toMap(KnowledgeDocument::getId, Function.identity()));

        List<SearchResult> results = new ArrayList<>();
        for (VectorIndex.Hit hit : hits) {
            KnowledgeChunk chunk = chunks.get(hit.chunkId());
            if (chunk == null || hit.score() < MINIMUM_SCORE) {
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

    /** Gives every chunk that has none an embedding. Runs once the application, and its seed data, are ready. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void indexMissingChunks() {
        List<Long> missing = vectorIndex.chunksWithoutEmbedding();
        for (KnowledgeChunk chunk : chunkRepository.findAllById(missing)) {
            KnowledgeDocument document = documentRepository.findById(chunk.getDocumentId()).orElse(null);
            // the title and tags describe the runbook better than any one paragraph, so they are embedded with it
            String text = document == null ? chunk.getText()
                    : document.getTitle() + " " + document.getTags() + " " + chunk.getText();
            vectorIndex.store(chunk.getId(), embeddingService.embed(text));
        }
        if (!missing.isEmpty()) {
            logger.info("Embedded {} runbook chunks", missing.size());
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
