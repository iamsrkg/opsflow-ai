package com.opsflow.request;

import com.opsflow.request.dto.RequestDTOs.*;
import com.opsflow.shared.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<RequestResponseDTO>> createRequest(@Valid @RequestBody CreateRequestDTO dto) {
        RequestResponseDTO created = requestService.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, "Request created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<RequestResponseDTO>>> searchRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<RequestResponseDTO> requests = requestService.searchRequests(status, category, priority, query, PageRequest.of(page, size, sort));

        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<RequestResponseDTO>> getRequestById(@PathVariable Long id) {
        RequestResponseDTO request = requestService.getRequestById(id);
        return ResponseEntity.ok(ApiResponse.success(request));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR')")
    public ResponseEntity<ApiResponse<RequestResponseDTO>> transitionStatus(
            @PathVariable Long id,
            @Valid @RequestBody TransitionDTO dto) {
        RequestResponseDTO updated = requestService.transitionStatus(id, dto.getTargetStatus(), dto.getComment());
        return ResponseEntity.ok(ApiResponse.success(updated, "Status updated successfully"));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR')")
    public ResponseEntity<ApiResponse<RequestResponseDTO>> assignRequest(
            @PathVariable Long id,
            @Valid @RequestBody AssignRequestDTO dto) {
        RequestResponseDTO updated = requestService.assignRequest(id, dto.getAssignedTo());
        return ResponseEntity.ok(ApiResponse.success(updated, "Request assigned successfully"));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR')")
    public ResponseEntity<ApiResponse<RequestResponseDTO>> escalateRequest(
            @PathVariable Long id,
            @Valid @RequestBody EscalateRequestDTO dto) {
        RequestResponseDTO updated = requestService.escalateRequest(id, dto.getReason());
        return ResponseEntity.ok(ApiResponse.success(updated, "Request escalated to CRITICAL priority"));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<RequestHistory>>> getRequestHistory(@PathVariable Long id) {
        List<RequestHistory> history = requestService.getRequestHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
