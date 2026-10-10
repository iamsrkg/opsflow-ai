package com.opsflow.ai;

import com.opsflow.request.Category;
import com.opsflow.request.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public interface AIProvider {

    ClassificationResult classify(String title, String description);

    InvestigationResult investigate(Long requestId, String title, String description);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ClassificationResult {
        private Category category;
        private Priority priority;
        private String summary;
        private double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class InvestigationResult {
        private String summary;
        private List<String> evidence;
        private String likelyCause;
        private double confidence;
        private String recommendedAction;
        private boolean requiresApproval;
        private String relevantRunbook;
    }
}
