package com.guardian.backend.repository;

import com.guardian.backend.entity.RepositoryScan;
import com.guardian.backend.entity.ScanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryScanRepository extends JpaRepository<RepositoryScan, Long> {

    List<RepositoryScan> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RepositoryScan> findAllByOrderByCreatedAtDesc();

    long countByStatus(ScanStatus status);

    @Query("SELECT COUNT(s) FROM RepositoryScan s")
    long countTotalScans();

    @Query("SELECT COALESCE(SUM(s.buggySnippetsFound), 0) FROM RepositoryScan s")
    long sumBuggySnippets();

    @Query("SELECT COALESCE(AVG(s.averageRiskScore), 0.0) FROM RepositoryScan s WHERE s.status = 'COMPLETED'")
    double averageRiskScoreAcrossScans();
}
