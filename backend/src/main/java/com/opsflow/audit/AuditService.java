package com.opsflow.audit;

import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public AuditEvent recordEvent(String who, String what, String why, Long requestId, String action, String result, String metadataJson) {
        String correlationId = MDC.get("correlationId");
        
        AuditEvent auditEvent = AuditEvent.builder()
                .who(who)
                .what(what)
                .why(why)
                .requestId(requestId)
                .correlationId(correlationId)
                .action(action)
                .result(result)
                .metadataJson(metadataJson)
                .build();

        return auditRepository.save(auditEvent);
    }

    public List<AuditEvent> getAuditTrailForRequest(Long requestId) {
        return auditRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
    }

    public Page<AuditEvent> getAllAuditEvents(Pageable pageable) {
        return auditRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
