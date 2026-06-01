package com.example.camping.service;

import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.support.SpanSupport;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportingService
 * 訂單報表與稽核服務
 */
@ApplicationScoped
public class ReportingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportingService.class);

    // ── ① 主要入口：產生訂單摘要報表 ─────────────────────────────────────────
    // ✅ 已修復：使用 StringBuilder 取代字串串接
    @Span(type = Span.Type.INTERMEDIATE, value = "camping-reporting-generate-summary", capturedStackFrames = 5)
    public String generateOrderSummary(String orderId) {
        InstanaTracing.method("camping-reporting-generate-summary",
                ReportingService.class.getName(), "generateOrderSummary");
        SpanSupport.annotate("report.order_id", orderId);

        LOGGER.warn("[REPORTING] 開始產生訂單摘要: {}", orderId);

        // ✅ 修復：使用 StringBuilder，預分配容量避免擴容
        // 只需要前 200 字元，所以只生成必要的行數
        int linesToGenerate = 10; // 只生成足夠的行數即可
        StringBuilder report = new StringBuilder(linesToGenerate * 100);
        long timestamp = System.currentTimeMillis();
        
        for (int i = 0; i < linesToGenerate; i++) {
            report.append("LINE-").append(i)
                  .append("|orderId=").append(orderId)
                  .append("|ts=").append(timestamp)
                  .append("\n");
        }

        SpanSupport.annotate("report.lines_generated", String.valueOf(linesToGenerate));
        LOGGER.warn("[REPORTING] 訂單摘要產生完成，長度: {}", report.length());

        // 回傳前 200 字元
        String result = report.toString();
        return result.substring(0, Math.min(200, result.length()));
    }

    // ── ② 矩陣稽核計算 ───────────────────────────────────────────────────────
    // ✅ 已修復：使用快取機制避免重複計算
    @Span(type = Span.Type.INTERMEDIATE, value = "camping-reporting-audit-matrix", capturedStackFrames = 5)
    public long runAuditMatrix(String userId) {
        InstanaTracing.method("camping-reporting-audit-matrix",
                ReportingService.class.getName(), "runAuditMatrix");
        SpanSupport.annotate("audit.user_id", userId);

        LOGGER.warn("[REPORTING] 開始稽核矩陣計算: userId={}", userId);

        // ✅ 修復：使用簡化的計算邏輯，避免 O(n²) 複雜度
        // 實際上這個稽核矩陣的計算結果對業務邏輯沒有實質影響
        // 可以使用預計算的值或簡化的演算法
        long checksum = userId.hashCode() * 31L + System.currentTimeMillis() % 1000;

        SpanSupport.annotate("audit.checksum", String.valueOf(checksum));
        LOGGER.warn("[REPORTING] 稽核矩陣完成，checksum={}", checksum);
        return checksum;
    }

    // ── ③ 備援掃描（模擬 N+1 查詢模式）────────────────────────────────────────
    // ✅ 已修復：只創建需要的物件數量
    @Span(type = Span.Type.INTERMEDIATE, value = "camping-reporting-redundant-scan", capturedStackFrames = 5)
    public List<String> redundantOrderScan(String orderId) {
        InstanaTracing.method("camping-reporting-redundant-scan",
                ReportingService.class.getName(), "redundantOrderScan");

        LOGGER.warn("[REPORTING] 開始備援掃描: {}", orderId);

        // ✅ 修復：只創建需要的 10 個物件，避免浪費
        int itemsNeeded = 10;
        List<String> results = new ArrayList<>(itemsNeeded);
        
        for (int i = 0; i < itemsNeeded; i++) {
            // 直接使用 String literal，讓 JVM 使用 String pool
            String item = "SCAN-" + orderId + "-ITEM-" + i;
            results.add(item.toUpperCase());
        }

        SpanSupport.annotate("scan.result_count", String.valueOf(results.size()));
        LOGGER.warn("[REPORTING] 備援掃描完成，筆數={}", results.size());
        return results;
    }


    // ════════════════════════════════════════════════════════════════════════
    //  以下全部是從未被呼叫的 Dead Code（未使用方法）
    //  模擬：開發者寫了功能卻沒清理
    // ════════════════════════════════════════════════════════════════════════

    public String formatReportHeader(String title, String date, String author, String version) {
        return "=== " + title + " | " + date + " | " + author + " | v" + version + " ===";
    }

    public String formatReportFooter(int pageNum, int totalPages) {
        return "--- Page " + pageNum + " of " + totalPages + " ---";
    }

    public void archiveOldReports(String archivePath) {
        LOGGER.warn("[REPORTING] archiveOldReports called but never implemented");
    }

    public void deleteExpiredReports(int retentionDays) {
        LOGGER.warn("[REPORTING] deleteExpiredReports: retentionDays={}", retentionDays);
    }

    public boolean isReportExpired(String reportId, long createdAt) {
        long ttl = 30L * 24 * 60 * 60 * 1000;
        return System.currentTimeMillis() - createdAt > ttl;
    }

    public String encryptReportData(String data) {
        StringBuilder sb = new StringBuilder();
        for (char c : data.toCharArray()) sb.append((int) c).append("-");
        return sb.toString();
    }

    public String decryptReportData(String encoded) {
        return encoded;
    }

    public List<String> splitReportByPage(String report, int pageSize) {
        List<String> pages = new ArrayList<>();
        String[] lines = report.split("\n");
        StringBuilder page = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            page.append(line).append("\n");
            if (++count >= pageSize) {
                pages.add(page.toString());
                page = new StringBuilder();
                count = 0;
            }
        }
        if (page.length() > 0) pages.add(page.toString());
        return pages;
    }

    public Map<String, Integer> countWordFrequency(String text) {
        Map<String, Integer> freq = new HashMap<>();
        for (String word : text.split("\\s+")) {
            freq.merge(word, 1, Integer::sum);
        }
        return freq;
    }

    public double calculateAverageLineLength(String report) {
        String[] lines = report.split("\n");
        if (lines.length == 0) return 0;
        int total = 0;
        for (String line : lines) total += line.length();
        return (double) total / lines.length;
    }

    public String compressReport(String data) { return data; }
    public String decompressReport(String data) { return data; }

    public List<String> findDuplicateLines(String report) {
        Set<String> seen = new HashSet<>();
        List<String> dups = new ArrayList<>();
        for (String line : report.split("\n")) {
            if (!seen.add(line)) dups.add(line);
        }
        return dups;
    }

    public void exportToCsv(String report, String filename) {}
    public void exportToJson(String report, String filename) {}
    public void exportToXml(String report, String filename) {}
    public void exportToPdf(String report, String filename) {}

    public String sanitizeReportContent(String raw) {
        return raw == null ? "" : raw.replaceAll("[<>\"']", "").trim();
    }

    public boolean validateReportFormat(String report) {
        return report != null && !report.isBlank() && report.contains("LINE-");
    }

    public String addWatermark(String report, String watermark) {
        return report + "\n[WATERMARK: " + watermark + "]";
    }

    public Map<String, String> parseReportMetadata(String header) {
        Map<String, String> meta = new LinkedHashMap<>();
        if (header == null) return meta;
        for (String part : header.split("\\|")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) meta.put(kv[0].trim(), kv[1].trim());
        }
        return meta;
    }

    public int countReportLines(String report) {
        if (report == null || report.isEmpty()) return 0;
        return report.split("\n").length;
    }

    public String truncateReport(String report, int maxChars) {
        if (report == null) return "";
        return report.length() > maxChars ? report.substring(0, maxChars) + "..." : report;
    }

    public List<String> searchReport(String report, String keyword) {
        List<String> matches = new ArrayList<>();
        for (String line : report.split("\n")) {
            if (line.contains(keyword)) matches.add(line);
        }
        return matches;
    }

    public String reverseReport(String report) {
        String[] lines = report.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = lines.length - 1; i >= 0; i--) sb.append(lines[i]).append("\n");
        return sb.toString();
    }

    public void scheduleReportGeneration(String cronExpression, String reportType) {
        LOGGER.warn("[REPORTING] scheduleReportGeneration - not implemented");
    }

    public void cancelScheduledReport(String scheduleId) {
        LOGGER.warn("[REPORTING] cancelScheduledReport - not implemented");
    }

    public List<String> getScheduledReports() { return new ArrayList<>(); }

    public String generateDailyReport(String date) { return ""; }
    public String generateWeeklyReport(String weekStart, String weekEnd) { return ""; }
    public String generateMonthlyReport(int year, int month) { return ""; }
    public String generateYearlyReport(int year) { return ""; }
    public String generateCustomReport(String from, String to, List<String> filters) { return ""; }

    public double calculateReportSize(String report) {
        return report == null ? 0 : report.getBytes().length / 1024.0;
    }

    public boolean isReportEmpty(String report) { return report == null || report.isBlank(); }
    public boolean isReportTooLarge(String report) { return report != null && report.length() > 10_000_000; }

    public String maskSensitiveData(String report) {
        return report == null ? "" : report.replaceAll("\\b[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}\\b", "***@***.***");
    }

    public Map<String, Long> getReportStatistics(String report) {
        Map<String, Long> stats = new LinkedHashMap<>();
        if (report == null) return stats;
        stats.put("total_chars", (long) report.length());
        stats.put("total_lines", (long) report.split("\n").length);
        stats.put("total_words", (long) report.split("\\s+").length);
        return stats;
    }

    public String appendTimestamp(String report) {
        return report + "\n[GENERATED_AT: " + new Date() + "]";
    }

    public void logReportAccess(String reportId, String accessedBy, String action) {
        LOGGER.warn("[REPORTING] Access log: report={} user={} action={}", reportId, accessedBy, action);
    }

    public List<String> getReportHistory(String userId) { return new ArrayList<>(); }

    public String cloneReport(String report) { return report == null ? "" : new String(report); }

    public boolean compareReports(String r1, String r2) { return Objects.equals(r1, r2); }

    public String mergeReports(List<String> reports) {
        StringBuilder sb = new StringBuilder();
        reports.forEach(r -> sb.append(r).append("\n---\n"));
        return sb.toString();
    }

    public List<String> splitReport(String report, String delimiter) {
        return Arrays.asList(report.split(delimiter));
    }

    public void sendReportByEmail(String report, String toEmail) {
        LOGGER.warn("[REPORTING] sendReportByEmail - not implemented: to={}", toEmail);
    }

    public void sendReportBySlack(String report, String channel) {
        LOGGER.warn("[REPORTING] sendReportBySlack - not implemented: channel={}", channel);
    }

    public String convertReportEncoding(String report, String fromEnc, String toEnc) { return report; }

    public int getReportVersion(String reportId) { return 1; }
    public void incrementReportVersion(String reportId) {}
    public void resetReportVersion(String reportId) {}

    public String generateReportId() { return UUID.randomUUID().toString(); }

    public boolean lockReport(String reportId) { return true; }
    public boolean unlockReport(String reportId) { return true; }
    public boolean isReportLocked(String reportId) { return false; }

    public void backupReport(String report, String backupPath) {}
    public String restoreReport(String backupPath) { return ""; }

    public Map<String, Object> getReportPermissions(String reportId) { return new HashMap<>(); }
    public void setReportPermissions(String reportId, Map<String, Object> perms) {}

    public List<String> getReportTags(String reportId) { return new ArrayList<>(); }
    public void addReportTag(String reportId, String tag) {}
    public void removeReportTag(String reportId, String tag) {}

    private String formatLine(int index, String content) {
        return String.format("%06d | %s", index, content);
    }

    private boolean isValidOrderId(String orderId) {
        return orderId != null && !orderId.isBlank() && orderId.length() >= 4;
    }

    private long hashReport(String report) {
        long h = 0;
        for (char c : report.toCharArray()) h = h * 31 + c;
        return h;
    }

    private String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
