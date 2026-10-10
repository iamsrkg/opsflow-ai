package com.opsflow.approval;

import com.opsflow.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getPendingApprovals() {
        return ResponseEntity.ok(ApiResponse.success(approvalService.getPendingApprovals()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ApprovalRequest>> approveAction(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.getOrDefault("comment", "Approved by manager") : "Approved by manager";
        ApprovalRequest approved = approvalService.approveAction(id, comment);
        return ResponseEntity.ok(ApiResponse.success(approved, "Action approved and executed successfully"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ApprovalRequest>> rejectAction(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Rejected by manager") : "Rejected by manager";
        ApprovalRequest rejected = approvalService.rejectAction(id, reason);
        return ResponseEntity.ok(ApiResponse.success(rejected, "Action request rejected"));
    }
}
