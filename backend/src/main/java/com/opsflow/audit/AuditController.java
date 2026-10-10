package com.opsflow.audit;

import com.opsflow.shared.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<AuditEvent>>> getAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditEvent> events = auditService.getAllAuditEvents(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/requests/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<AuditEvent>>> getRequestAuditTrail(@PathVariable Long requestId) {
        List<AuditEvent> auditTrail = auditService.getAuditTrailForRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(auditTrail));
    }
}
