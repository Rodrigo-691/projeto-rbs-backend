package com.rbs.planeta.repository;

import com.rbs.planeta.model.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findBySponsorIdOrderByCreatedAtDesc(UUID sponsorId, Pageable pageable);

    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
