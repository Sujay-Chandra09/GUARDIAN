package com.guardian.backend.controller;

import com.guardian.backend.dto.*;
import com.guardian.backend.service.ScanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scans")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    /**
     * Real-time single snippet bug analysis (used by IDE plugins, PR bots, and dashboard playground).
     */
    @PostMapping("/analyze")
    public ResponseEntity<ScanFindingDto> analyzeSnippet(@Valid @RequestBody AnalyzeSnippetRequest request) {
        ScanFindingDto finding = scanService.analyzeSnippet(request);
        return ResponseEntity.ok(finding);
    }

    /**
     * Create a full repository scan or batch snippet scan.
     */
    @PostMapping
    public ResponseEntity<ScanResponseDto> createScan(@Valid @RequestBody CreateScanRequest request) {
        ScanResponseDto scan = scanService.createRepositoryScan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(scan);
    }

    /**
     * Get scan history for the current authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ScanResponseDto>> getUserScans() {
        return ResponseEntity.ok(scanService.getUserScans());
    }

    /**
     * Get scan details by ID, including all findings.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScanResponseDto> getScanById(@PathVariable Long id) {
        return ResponseEntity.ok(scanService.getScanById(id));
    }

    /**
     * Get aggregated risk metrics for the scan (High/Med/Low counts, health status).
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<ScanSummaryDto> getScanSummary(@PathVariable Long id) {
        return ResponseEntity.ok(scanService.getScanSummary(id));
    }

    /**
     * Get global platform-wide statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<GlobalStatsDto> getGlobalStats() {
        return ResponseEntity.ok(scanService.getGlobalStats());
    }
}
