package com.opsflow.ai;

import com.opsflow.request.Category;
import com.opsflow.request.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The part that can be swapped: rules by default, a language model when one is configured.
 * A provider only writes a classification or a recommendation. It never changes a request,
 * and what it recommends is checked against ActionPolicy before anything is stored.
 */
public interface AIProvider {

    /** A short name stored with each investigation, so it is clear what produced it. */
    String name();

    ClassificationResult classify(String title, String description);

    Recommendation recommend(Evidence evidence);

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
    class Recommendation {
        private String summary;
        private String likelyCause;
        private String recommendedAction;
        private double confidence;
        /** True when a model was asked but its answer could not be used, and the rules answered instead. */
        private boolean fellBack;
    }
}
