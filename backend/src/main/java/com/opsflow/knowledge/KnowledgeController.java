package com.opsflow.knowledge;

import com.opsflow.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeDocumentRepository documentRepository;
    private final VectorStoreService vectorStoreService;

    public KnowledgeController(KnowledgeDocumentRepository documentRepository, VectorStoreService vectorStoreService) {
        this.documentRepository = documentRepository;
        this.vectorStoreService = vectorStoreService;
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<KnowledgeDocument>>> getAllDocuments() {
        return ResponseEntity.ok(ApiResponse.success(documentRepository.findAll()));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<VectorStoreService.SearchResult>>> searchKnowledge(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        List<VectorStoreService.SearchResult> results = vectorStoreService.searchVectorStore(query, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
