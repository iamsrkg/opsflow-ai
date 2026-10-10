package com.opsflow.request.dto;

import com.opsflow.request.Category;
import com.opsflow.request.Priority;
import com.opsflow.request.RequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class RequestDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequestDTO {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Description is required")
        private String description;

        private Category category;
        private Priority priority;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequestDTO {
        private String title;
        private String description;
        private Category category;
        private Priority priority;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignRequestDTO {
        @NotBlank(message = "Assigned user is required")
        private String assignedTo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EscalateRequestDTO {
        @NotBlank(message = "Escalation reason is required")
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolveRequestDTO {
        @NotBlank(message = "Resolution summary is required")
        private String resolutionSummary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitionDTO {
        @NotNull(message = "Target status is required")
        private RequestStatus targetStatus;
        private String comment;
        /** The version the client last read. If the request has changed since, the call is refused with 409. */
        private Long expectedVersion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestResponseDTO {
        private Long id;
        private String externalReference;
        private String title;
        private String description;
        private Category category;
        private Priority priority;
        private RequestStatus status;
        private String createdBy;
        private String assignedTo;
        private String classificationSummary;
        private Double confidenceScore;
        private Long version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime resolvedAt;
    }
}
