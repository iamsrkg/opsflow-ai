package com.opsflow.knowledge;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorStoreService {

    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;

    public VectorStoreService(KnowledgeChunkRepository chunkRepository, KnowledgeDocumentRepository documentRepository) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
    }

    public List<SearchResult> searchVectorStore(String query, int topK) {
        List<KnowledgeChunk> allChunks = chunkRepository.findAll();
        Map<Long, KnowledgeDocument> docMap = documentRepository.findAll().stream()
                .collect(Collectors.toMap(KnowledgeDocument::getId, doc -> doc));

        String[] queryTerms = query.toLowerCase().split("\\s+");

        List<SearchResult> results = new ArrayList<>();
        for (KnowledgeChunk chunk : allChunks) {
            String textLower = chunk.getText().toLowerCase();
            KnowledgeDocument doc = docMap.get(chunk.getDocumentId());
            String titleLower = (doc != null ? doc.getTitle() : "").toLowerCase();

            double score = 0.0;
            for (String term : queryTerms) {
                if (term.length() < 3) continue;
                if (titleLower.contains(term)) score += 0.4;
                if (textLower.contains(term)) score += 0.2;
            }

            if (score > 0.1) {
                results.add(new SearchResult(
                        chunk.getId(),
                        chunk.getDocumentId(),
                        doc != null ? doc.getTitle() : "Runbook Document",
                        chunk.getText(),
                        Math.min(1.0, score + 0.5) // Normalized score
                ));
            }
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results.stream().limit(topK).collect(Collectors.toList());
    }

    public static class SearchResult {
        private Long chunkId;
        private Long documentId;
        private String documentTitle;
        private String snippet;
        private double score;

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
