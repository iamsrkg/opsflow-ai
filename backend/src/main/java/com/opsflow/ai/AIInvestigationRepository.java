package com.opsflow.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIInvestigationRepository extends JpaRepository<AIInvestigation, Long> {
    List<AIInvestigation> findByRequestIdOrderByCreatedAtDesc(Long requestId);
}
