package com.sambath.admincafe.report;

import com.sambath.admincafe.report.dto.ReportKpisResponse;
import com.sambath.admincafe.report.dto.ReportSummaryResponse;
import com.sambath.admincafe.report.dto.RevenueSeriesResponse;
import com.sambath.admincafe.report.dto.TopProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ReportSummaryResponse summary(@RequestParam(required = false) String range) {
        return reportService.summary(ReportRange.parse(range, ReportRange.DAILY));
    }

    @GetMapping("/revenue-series")
    public RevenueSeriesResponse revenueSeries(@RequestParam(required = false) String range) {
        return reportService.revenueSeries(ReportRange.parse(range, ReportRange.WEEKLY));
    }

    @GetMapping("/top-products")
    public List<TopProductResponse> topProducts(
            @RequestParam(required = false) String range,
            @RequestParam(required = false, defaultValue = "5") int limit
    ) {
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        return reportService.topProducts(ReportRange.parse(range, ReportRange.MONTHLY), limit);
    }

    @GetMapping("/kpis")
    public ReportKpisResponse kpis(@RequestParam(required = false) String range) {
        return reportService.kpis(ReportRange.parse(range, ReportRange.MONTHLY));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String range,
            @RequestParam(required = false, defaultValue = "pdf") String format
    ) {
        ReportRange r = ReportRange.parse(range, ReportRange.MONTHLY);

        if ("csv".equalsIgnoreCase(format)) {
            byte[] body = reportService.exportCsv(r);
            String filename = "report-" + r.displayName() + "-" + LocalDate.now() + ".csv";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(body);
        }

        if ("pdf".equalsIgnoreCase(format)) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                    "PDF export not yet implemented; use format=csv");
        }

        throw new IllegalArgumentException("Invalid format: " + format + " (use pdf or csv)");
    }
}
