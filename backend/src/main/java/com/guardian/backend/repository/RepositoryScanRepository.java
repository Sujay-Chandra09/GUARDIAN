package com.guardian.backend.repository;

import com.guardian.backend.entity.RepositoryScan;
import com.guardian.backend.entity.ScanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryScanRepository extends JpaRepository<RepositoryScan, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<RepositoryScan> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<RepositoryScan> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user"})
    Page<RepositoryScan> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<RepositoryScan> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "findings"})
    @Query("SELECT s FROM RepositoryScan s WHERE s.id = :id")
    Optional<RepositoryScan> findDetailedById(@Param("id") Long id);

    long countByStatus(ScanStatus status);

    @Query("SELECT COUNT(s) FROM RepositoryScan s")
    long countTotalScans();

    @Query("SELECT COALESCE(SUM(s.buggySnippetsFound), 0) FROM RepositoryScan s")
    long sumBuggySnippets();

    @Query("SELECT COALESCE(AVG(s.averageRiskScore), 0.0) FROM RepositoryScan s WHERE s.status = com.guardian.backend.entity.ScanStatus.COMPLETED AND SIZE(s.findings) > 0")
    double averageRiskScoreAcrossScans();

    // User-scoped queries
    long countByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(s.buggySnippetsFound), 0) FROM RepositoryScan s WHERE s.user.id = :userId")
    long sumBuggySnippetsByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(AVG(s.averageRiskScore), 0.0) FROM RepositoryScan s WHERE s.user.id = :userId AND s.status = com.guardian.backend.entity.ScanStatus.COMPLETED AND SIZE(s.findings) > 0")
    double averageRiskScoreByUserId(@Param("userId") Long userId);

    long countByUserIdAndStatus(Long userId, ScanStatus status);
}
