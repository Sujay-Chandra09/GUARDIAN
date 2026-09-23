package com.guardian.backend.service;

import com.guardian.backend.dto.*;
import com.guardian.backend.entity.*;
import com.guardian.backend.exception.ResourceNotFoundException;
import com.guardian.backend.repository.RepositoryScanRepository;
import com.guardian.backend.repository.ScanFindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScanService {

    private final RepositoryScanRepository scanRepository;
    private final ScanFindingRepository findingRepository;
    private final MlServiceClient mlServiceClient;
    private final AuthService authService;

    public ScanService(RepositoryScanRepository scanRepository,
                       ScanFindingRepository findingRepository,
                       MlServiceClient mlServiceClient,
                       AuthService authService) {
        this.scanRepository = scanRepository;
        this.findingRepository = findingRepository;
        this.mlServiceClient = mlServiceClient;
        this.authService = authService;
    }

    @Transactional
    public ScanFindingDto analyzeSnippet(AnalyzeSnippetRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        // 1. Call ML Service
        MlPredictResponse mlResponse = mlServiceClient.predictBugRisk(
                request.getCodeSnippet(),
                request.getRepo(),
                request.getFilepath()
        );

        // 2. Map risk level
        RiskLevel riskLevel;
        try {
            riskLevel = RiskLevel.valueOf(mlResponse.getRiskLevel().toUpperCase());
        } catch (Exception e) {
            riskLevel = mlResponse.getRiskScore() >= 0.75 ? RiskLevel.HIGH :
                        (mlResponse.getRiskScore() >= 0.4 ? RiskLevel.MEDIUM : RiskLevel.LOW);
        }

        // 3. Find or create RepositoryScan
        RepositoryScan scan;
        if (request.getRepositoryScanId() != null) {
            scan = scanRepository.findById(request.getRepositoryScanId())
                    .orElseThrow(() -> new ResourceNotFoundException("RepositoryScan not found: " + request.getRepositoryScanId()));
        } else {
            scan = new RepositoryScan(
                    request.getRepo() != null ? request.getRepo() : "ad-hoc-analysis",
                    null,
                    "main",
                    null,
                    currentUser
            );
            scan.setStatus(ScanStatus.COMPLETED);
            scan = scanRepository.save(scan);
        }

        // 4. Create and persist ScanFinding
        ScanFinding finding = new ScanFinding(
                request.getFilepath(),
                request.getFunctionName(),
                request.getCodeSnippet(),
                mlResponse.getRiskScore(),
                riskLevel,
                mlResponse.isBuggy(),
                mlResponse.getExplanation(),
                mlResponse.getExplanation(), // Suggested fix included in markdown explanation
                mlResponse.getRetrievedContextCount()
        );

        scan.addFinding(finding);
        scan.setTotalFilesScanned(scan.getTotalFilesScanned() + 1);
        if (finding.isBuggy()) {
            scan.setBuggySnippetsFound(scan.getBuggySnippetsFound() + 1);
        }

        // Recalculate average risk score
        double currentTotal = scan.getAverageRiskScore() * (scan.getTotalFilesScanned() - 1);
        scan.setAverageRiskScore(Math.round(((currentTotal + finding.getRiskScore()) / scan.getTotalFilesScanned()) * 10000.0) / 10000.0);
        scan.setScanDurationMs(scan.getScanDurationMs() + (long) mlResponse.getLatencyMs());

        RepositoryScan savedScan = scanRepository.save(scan);
        ScanFinding savedFinding = savedScan.getFindings().get(savedScan.getFindings().size() - 1);

        return ScanFindingDto.fromEntity(savedFinding);
    }

    @Transactional
    public ScanResponseDto createRepositoryScan(CreateScanRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        long startTime = System.currentTimeMillis();

        RepositoryScan scan = new RepositoryScan(
                request.getRepoName(),
                request.getRepoUrl(),
                request.getBranch(),
                request.getCommitHash(),
                currentUser
        );
        scan.setStatus(ScanStatus.IN_PROGRESS);
        scan = scanRepository.save(scan);

        int buggyCount = 0;
        double sumRisk = 0.0;

        if (request.getSnippets() != null && !request.getSnippets().isEmpty()) {
            for (CreateScanRequest.SnippetItem item : request.getSnippets()) {
                MlPredictResponse mlResponse = mlServiceClient.predictBugRisk(
                        item.getCodeSnippet(),
                        request.getRepoName(),
                        item.getFilePath()
                );

                RiskLevel riskLevel;
                try {
                    riskLevel = RiskLevel.valueOf(mlResponse.getRiskLevel().toUpperCase());
                } catch (Exception e) {
                    riskLevel = mlResponse.getRiskScore() >= 0.75 ? RiskLevel.HIGH :
                                (mlResponse.getRiskScore() >= 0.4 ? RiskLevel.MEDIUM : RiskLevel.LOW);
                }

                ScanFinding finding = new ScanFinding(
                        item.getFilePath(),
                        item.getFunctionName(),
                        item.getCodeSnippet(),
                        mlResponse.getRiskScore(),
                        riskLevel,
                        mlResponse.isBuggy(),
                        mlResponse.getExplanation(),
                        mlResponse.getExplanation(),
                        mlResponse.getRetrievedContextCount()
                );

                scan.addFinding(finding);
                sumRisk += mlResponse.getRiskScore();
                if (mlResponse.isBuggy()) {
                    buggyCount++;
                }
            }
            scan.setTotalFilesScanned(request.getSnippets().size());
            scan.setBuggySnippetsFound(buggyCount);
            scan.setAverageRiskScore(Math.round((sumRisk / request.getSnippets().size()) * 10000.0) / 10000.0);
        }

        scan.setStatus(ScanStatus.COMPLETED);
        scan.setScanDurationMs(System.currentTimeMillis() - startTime);
        RepositoryScan savedScan = scanRepository.save(scan);

        return ScanResponseDto.fromEntity(savedScan, true);
    }

    public List<ScanResponseDto> getUserScans() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        List<RepositoryScan> scans;
        if (currentUser != null) {
            scans = scanRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        } else {
            scans = scanRepository.findAllByOrderByCreatedAtDesc();
        }
        return scans.stream()
                .map(s -> ScanResponseDto.fromEntity(s, false))
                .collect(Collectors.toList());
    }

    public ScanResponseDto getScanById(Long id) {
        RepositoryScan scan = scanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RepositoryScan not found: " + id));
        return ScanResponseDto.fromEntity(scan, true);
    }

    public ScanSummaryDto getScanSummary(Long id) {
        RepositoryScan scan = scanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RepositoryScan not found: " + id));

        long high = findingRepository.countByRepositoryScanIdAndRiskLevel(id, RiskLevel.HIGH);
        long medium = findingRepository.countByRepositoryScanIdAndRiskLevel(id, RiskLevel.MEDIUM);
        long low = findingRepository.countByRepositoryScanIdAndRiskLevel(id, RiskLevel.LOW);
        long totalBuggy = findingRepository.countByRepositoryScanIdAndIsBuggyTrue(id);

        String healthStatus;
        if (high > 0 || scan.getAverageRiskScore() >= 0.70) {
            healthStatus = "CRITICAL";
        } else if (medium > 0 || scan.getAverageRiskScore() >= 0.40) {
            healthStatus = "WARNING";
        } else {
            healthStatus = "HEALTHY";
        }

        ScanSummaryDto summary = new ScanSummaryDto();
        summary.setScanId(scan.getId());
        summary.setRepoName(scan.getRepoName());
        summary.setTotalFindings(scan.getTotalFilesScanned());
        summary.setHighRiskCount(high);
        summary.setMediumRiskCount(medium);
        summary.setLowRiskCount(low);
        summary.setTotalBuggyCount(totalBuggy);
        summary.setAverageRiskScore(scan.getAverageRiskScore());
        summary.setOverallHealthStatus(healthStatus);

        return summary;
    }

    public GlobalStatsDto getGlobalStats() {
        long totalScans = scanRepository.countTotalScans();
        long totalBugs = scanRepository.sumBuggySnippets();
        double avgRisk = scanRepository.averageRiskScoreAcrossScans();
        long activeScans = scanRepository.countByStatus(ScanStatus.IN_PROGRESS);

        return new GlobalStatsDto(
                totalScans,
                totalBugs,
                Math.round(avgRisk * 10000.0) / 10000.0,
                activeScans
        );
    }
}
