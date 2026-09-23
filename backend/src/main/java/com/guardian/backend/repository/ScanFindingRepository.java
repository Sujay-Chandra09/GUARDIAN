package com.guardian.backend.repository;

import com.guardian.backend.entity.RiskLevel;
import com.guardian.backend.entity.ScanFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanFindingRepository extends JpaRepository<ScanFinding, Long> {

    List<ScanFinding> findByRepositoryScanId(Long repositoryScanId);

    long countByRepositoryScanIdAndIsBuggyTrue(Long repositoryScanId);

    long countByRepositoryScanIdAndRiskLevel(Long repositoryScanId, RiskLevel riskLevel);
}
