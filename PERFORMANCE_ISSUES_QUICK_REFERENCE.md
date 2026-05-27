# 效能問題快速參考表

## 📊 問題總覽

| # | 嚴重度 | 問題 | 檔案:行數 | 耗時 | 修復時間 | 優先級 |
|---|--------|------|-----------|------|---------|--------|
| 1 | 🔴 Critical | 字串串接 O(n²) | `ReportingService.java:33-36` | 5-10s | 30min | P0 |
| 2 | 🔴 Critical | 巢狀迴圈 O(n²) | `ReportingService.java:57-61` | 2-5s | 1h | P0 |
| 3 | 🔴 Critical | 不必要物件創建 | `ReportingService.java:78-92` | 1-2s | 30min | P0 |
| 4 | 🟡 Medium | 同步報表阻塞 | `CheckoutResource.java:148-155` | 8-17s | 1h | P1 |
| 5 | 🟡 Medium | Dead Code 70% | `ReportingService.java:101-337` | - | 2h | P2 |
| 6 | 🟢 Low | Kafka 同步發送 | `KafkaCheckoutService.java:74` | 10-100ms | 1h | P3 |

---

## 🔴 Critical 問題

### #1 字串串接災難 (5-10 秒)

```java
// ❌ 問題: ReportingService.java:33-36
String report = "";
for (int i = 0; i < 50000; i++) {
    report += "LINE-" + i + "|orderId=" + orderId + "\n";  // O(n²)
}

// ✅ 修復
StringBuilder report = new StringBuilder(5000000);
for (int i = 0; i < 50000; i++) {
    report.append("LINE-").append(i).append("|orderId=").append(orderId).append("\n");
}
```

**改善**: 5-10s → 50-100ms (50-100x)

---

### #2 巢狀迴圈 (2-5 秒)

```java
// ❌ 問題: ReportingService.java:57-61
for (int i = 0; i < 1000; i++) {
    for (int j = 0; j < 1000; j++) {
        checksum += (long)(Math.sqrt(i*j+1) * Math.log(j+2) * Math.sin(i+1));
    }
}

// ✅ 修復: 快取
private final Map<String, Long> cache = new ConcurrentHashMap<>();
public long runAuditMatrix(String userId) {
    return cache.computeIfAbsent(userId, this::compute);
}
```

**改善**: 2-5s → < 1ms (2000-5000x)

---

### #3 不必要物件 (1-2 秒)

```java
// ❌ 問題: ReportingService.java:78-92
List<String> results = new ArrayList<>();
for (int i = 0; i < 30000; i++) {
    results.add(new String("SCAN-" + orderId + "-ITEM-" + i));
}
return results.stream().filter(...).map(...).collect(...).subList(0, 10);

// ✅ 修復: 只產生需要的
List<String> results = new ArrayList<>(10);
for (int i = 0; i < 10; i++) {
    results.add("SCAN-" + orderId + "-ITEM-" + i);
}
return results;
```

**改善**: 1-2s → < 1ms (1000-2000x)

---

## 🟡 Medium 問題

### #4 同步報表阻塞 (8-17 秒)

```java
// ❌ 問題: CheckoutResource.java:148-155
reportingService.generateOrderSummary(order.getOrderId());      // 5-10s
reportingService.runAuditMatrix(authenticatedUser.getUserId()); // 2-5s
reportingService.redundantOrderScan(order.getOrderId());        // 1-2s

// ✅ 修復: 非同步
Object snapshot = ContextSupport.takeSnapshot();
executorService.submit(() -> {
    ContextSupport.restoreSnapshot(snapshot);
    reportingService.generateOrderSummary(order.getOrderId());
    reportingService.runAuditMatrix(authenticatedUser.getUserId());
    reportingService.redundantOrderScan(order.getOrderId());
});
```

**改善**: Checkout 8-17s → < 500ms (17-35x)

---

### #5 Dead Code (237 行)

```java
// ❌ 問題: ReportingService.java:101-337
// 50+ 個未使用方法:
formatReportHeader(), formatReportFooter(), archiveOldReports(),
deleteExpiredReports(), encryptReportData(), exportToCsv(), ...

// ✅ 修復: 刪除所有未使用方法
```

**改善**: 程式碼減少 70%

---

## 📈 效能改善預估

| 階段 | Checkout 回應時間 | 改善 |
|------|------------------|------|
| 修復前 | 8,500-17,500ms | - |
| 修復 #1 | 3,500-7,500ms | 2.4x |
| 修復 #2 | 1,500-2,500ms | 2.3-3.0x |
| 修復 #3 | 500-1,500ms | 3.0x |
| **修復 #4** | **< 500ms** | **17-35x** |

---

## 🎯 修復計劃

### 本週 (P0)
- [ ] #1 StringBuilder (30min)
- [ ] #4 非同步報表 (1h)
- [ ] 測試部署 (2h)

**效果**: 8-17s → < 500ms

### 下週 (P0-P1)
- [ ] #2 快取 (1h)
- [ ] #3 優化物件 (30min)
- [ ] 測試部署 (1.5h)

**效果**: CPU/記憶體減少 90%

### 下下週 (P2)
- [ ] #5 刪除 Dead Code (2h)

**效果**: 程式碼減少 70%

---

## 📊 Instana Span 對照

| Span | 當前 | 目標 | 改善 |
|------|------|------|------|
| `camping-api-checkout` | 8.5-17.5s | < 500ms | 17-35x |
| `camping-reporting-generate-summary` | 5-10s | 50-100ms | 50-100x |
| `camping-reporting-audit-matrix` | 2-5s | < 1ms | 2000-5000x |
| `camping-reporting-redundant-scan` | 1-2s | < 1ms | 1000-2000x |

---

**更新**: 2026-05-27  
**完整報告**: [`INSTANA_PERFORMANCE_ANALYSIS_REPORT.md`](INSTANA_PERFORMANCE_ANALYSIS_REPORT.md)