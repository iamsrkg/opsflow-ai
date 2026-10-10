package com.opsflow.request;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    Optional<Request> findByExternalReference(String externalReference);

    Page<Request> findByStatus(RequestStatus status, Pageable pageable);

    Page<Request> findByCategory(Category category, Pageable pageable);

    Page<Request> findByPriority(Priority priority, Pageable pageable);

    Page<Request> findByAssignedTo(String assignedTo, Pageable pageable);

    @Query("SELECT r FROM Request r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:category IS NULL OR r.category = :category) AND " +
           "(:priority IS NULL OR r.priority = :priority) AND " +
           "(:query IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Request> searchRequests(RequestStatus status, Category category, Priority priority, String query, Pageable pageable);

    long countByStatus(RequestStatus status);

    long countByPriority(Priority priority);
}
