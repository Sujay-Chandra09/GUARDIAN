package com.guardian.backend.service;

import com.guardian.backend.dto.*;
import com.guardian.backend.entity.*;
import com.guardian.backend.exception.ResourceNotFoundException;
import com.guardian.backend.repository.RepositoryScanRepository;
import com.guardian.backend.repository.ScanFindingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
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

    private User getRequiredCurrentUser() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser == null) {
            throw new AuthenticationCredentialsNotFoundException("User is not authenticated");
        }
        return currentUser;
    }

    private void checkScanOwnership(RepositoryScan scan, User currentUser) {
        if (currentUser.getRole() != Role.ROLE_ADMIN) {
            if (scan.getUser() == null || !scan.getUser().getId().equals(currentUser.getId())) {
                throw new ResourceNotFoundException("RepositoryScan not found: " + scan.getId());
            }
        }
    }

    private RiskLevel mapRiskLevel(MlPredictResponse mlResponse) {
        try {
            if (mlResponse.getRiskLevel() != null) {
                return RiskLevel.valueOf(mlResponse.getRiskLevel().toUpperCase());
            }
        } catch (Exception ignored) {
        }
        return mlResponse.getRiskScore() >= 0.75 ? RiskLevel.HIGH :
                (mlResponse.getRiskScore() >= 0.4 ? RiskLevel.MEDIUM : RiskLevel.LOW);
    }

    @Transactional
    public ScanFindingDto analyzeSnippet(AnalyzeSnippetRequest request) {
        User currentUser = getRequiredCurrentUser();

        // 1. Call ML Service
        MlPredictResponse mlResponse = mlServiceClient.predictBugRisk(
                request.getCodeSnippet(),
                request.getRepo(),
                request.getFilepath()
        );

        // 2. Map risk level
        RiskLevel riskLevel = mapRiskLevel(mlResponse);

        // 3. Find or create RepositoryScan
        RepositoryScan scan;
        if (request.getRepositoryScanId() != null) {
            scan = scanRepository.findById(request.getRepositoryScanId())
                    .orElseThrow(() -> new ResourceNotFoundException("RepositoryScan not found: " + request.getRepositoryScanId()));
            checkScanOwnership(scan, currentUser);
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

    public ScanResponseDto createRepositoryScan(CreateScanRequest request) {
        User currentUser = getRequiredCurrentUser();
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
            try {
                for (CreateScanRequest.SnippetItem item : request.getSnippets()) {
                    MlPredictResponse mlResponse = mlServiceClient.predictBugRisk(
                            item.getCodeSnippet(),
                            request.getRepoName(),
                            item.getFilePath()
                    );

                    RiskLevel riskLevel = mapRiskLevel(mlResponse);

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
                scan.setStatus(ScanStatus.COMPLETED);
            } catch (Exception ex) {
                scan.setStatus(ScanStatus.FAILED);
                scan.setScanDurationMs(System.currentTimeMillis() - startTime);
                scanRepository.save(scan);
                throw ex;
            }
        } else {
            scan.setStatus(ScanStatus.COMPLETED);
        }

        scan.setScanDurationMs(System.currentTimeMillis() - startTime);
        RepositoryScan savedScan = scanRepository.save(scan);

        ScanResponseDto dto = ScanResponseDto.fromEntity(savedScan, true);
        if (currentUser != null) {
            dto.setUserId(currentUser.getId());
            dto.setUsername(currentUser.getUsername());
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<ScanResponseDto> getUserScans(int page, int size, boolean all) {
        User currentUser = getRequiredCurrentUser();
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<RepositoryScan> scanPage;
        if (all) {
            if (currentUser.getRole() != Role.ROLE_ADMIN) {
                throw new AccessDeniedException("Access denied: only administrators can view all scans");
            }
            scanPage = scanRepository.findAll(pageable);
        } else {
            scanPage = scanRepository.findByUserId(currentUser.getId(), pageable);
        }
        return scanPage.map(s -> ScanResponseDto.fromEntity(s, false));
    }

    @Transactional(readOnly = true)
    public List<ScanResponseDto> getUserScans() {
        return getUserScans(0, 20, false).getContent();
    }

    @Transactional(readOnly = true)
    public ScanResponseDto getScanById(Long id) {
        User currentUser = getRequiredCurrentUser();
        RepositoryScan scan = scanRepository.findDetailedById(id)
                .or(() -> scanRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("RepositoryScan not found: " + id));

        checkScanOwnership(scan, currentUser);
        return ScanResponseDto.fromEntity(scan, true);
    }

    @Transactional(readOnly = true)
    public ScanSummaryDto getScanSummary(Long id) {
        User currentUser = getRequiredCurrentUser();
        RepositoryScan scan = scanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RepositoryScan not found: " + id));

        checkScanOwnership(scan, currentUser);

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

    @Transactional(readOnly = true)
    public GlobalStatsDto getStats() {
        User currentUser = getRequiredCurrentUser();
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return getGlobalStats();
        }
        return getUserStats(currentUser.getId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public GlobalStatsDto getUserStats(Long userId) {
        long totalScans = scanRepository.countByUserId(userId);
        long totalBugs = scanRepository.sumBuggySnippetsByUserId(userId);
        double avgRisk = scanRepository.averageRiskScoreByUserId(userId);
        long activeScans = scanRepository.countByUserIdAndStatus(userId, ScanStatus.IN_PROGRESS);

        return new GlobalStatsDto(
                totalScans,
                totalBugs,
                Math.round(avgRisk * 10000.0) / 10000.0,
                activeScans
        );
    }
}
