package com.opsflow.ai;

import com.opsflow.request.Request;
import com.opsflow.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/requests/{requestId}")
public class AIController {

    private final AIInvestigationService aiInvestigationService;

    public AIController(AIInvestigationService aiInvestigationService) {
        this.aiInvestigationService = aiInvestigationService;
    }

    @PostMapping("/classify")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR')")
    public ResponseEntity<ApiResponse<Request>> classifyRequest(@PathVariable Long requestId) {
        Request classified = aiInvestigationService.classifyRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(classified, "Request successfully classified by AI"));
    }

    @PostMapping("/investigate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR')")
    public ResponseEntity<ApiResponse<AIInvestigation>> investigateRequest(@PathVariable Long requestId) {
        AIInvestigation investigation = aiInvestigationService.investigateRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(investigation, "AI Investigation completed with evidence breakdown"));
    }

    @GetMapping("/investigations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<AIInvestigation>>> getInvestigations(@PathVariable Long requestId) {
        List<AIInvestigation> list = aiInvestigationService.getInvestigationsForRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
