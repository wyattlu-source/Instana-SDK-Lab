# Camping API - Instana 效能分析報告

## 📊 執行摘要

**分析日期**: 2026-05-27  
**專案**: Camping API (Java 17 / Jakarta EE 10)  
**部署環境**: JBoss EAP 8  
**Instana 端點**: https://it-palsys.instana-k3s.palsys.com.tw

---

## 🎯 分析目標

本報告針對 Camping API 專案進行全面的效能分析,識別系統瓶頸、程式碼問題和優化機會。

---

## 🔍 系統架構概覽

### 技術棧
- **後端框架**: Jakarta EE 10, JAX-RS
- **應用伺服器**: JBoss EAP 8
- **資料庫**: MongoDB Atlas
- **訊息佇列**: Kafka 3.7.1+ (Topic: raw_events)
- **監控工具**: Instana Agent + SDK
- **前端**: React (內建於 WAR)

### 核心服務流程
```
用戶請求 → JAX-RS Resource → Business Service → Repository → MongoDB
                ↓
         Kafka Producer → raw_events Topic → Consumer Services
                ↓
         Instana Tracing (自動 + 手動)
```

---

## ⚠️ 關鍵效能問題

### 🔴 嚴重問題 (Critical)

#### 1. ReportingService - 字串串接效能災難
**位置**: [`ReportingService.java:33-36`](src/main/java/com/example/camping/service/ReportingService.java:33-36)

**問題描述**:
```java
String report = "";
for (int i = 0; i < 50000; i++) {
    report += "LINE-" + i + "|orderId=" + orderId + "|ts=" + System.currentTimeMillis() + "\n";
}
```

**影響**:
- ⚠️ **時間複雜度**: O(n²) - 每次 `+=` 都會創建新的 String 物件
- ⚠️ **記憶體消耗**: 產生 50,000 個中間 String 物件
- ⚠️ **GC 壓力**: 大量臨時物件導致頻繁 GC
- ⚠️ **執行時間**: 預估 5-10 秒 (應該 < 100ms)

**Instana Span**: `camping-reporting-generate-summary`

**建議修復**:
```java
StringBuilder report = new StringBuilder(50000 * 100); // 預分配容量
for (int i = 0; i < 50000; i++) {
    report.append("LINE-").append(i)
          .append("|orderId=").append(orderId)
          .append("|ts=").append(System.currentTimeMillis())
          .append("\n");
}
return report.substring(0, Math.min(200, report.length()));
```

**預期改善**: 執行時間從 5-10 秒降至 50-100ms (50-100x 提升)

---

#### 2. ReportingService - O(n²) 巢狀迴圈
**位置**: [`ReportingService.java:57-61`](src/main/java/com/example/camping/service/ReportingService.java:57-61)

**問題描述**:
```java
long checksum = 0;
for (int i = 0; i < 1000; i++) {
    for (int j = 0; j < 1000; j++) {
        checksum += (long)(Math.sqrt(i * j + 1) * Math.log(j + 2) * Math.sin(i + 1));
    }
}
```

**影響**:
- ⚠️ **計算次數**: 1,000,000 次複雜數學運算
- ⚠️ **CPU 密集**: sqrt, log, sin 運算非常耗時
- ⚠️ **執行時間**: 預估 2-5 秒
- ⚠️ **阻塞主執行緒**: 影響其他請求處理

**Instana Span**: `camping-reporting-audit-matrix`

**建議修復**:
1. **快取結果**: 如果 userId 相同,結果應該快取
2. **非同步處理**: 移到背景執行緒
3. **演算法優化**: 重新評估是否真的需要這個計算
4. **批次處理**: 如果必須計算,考慮分批處理

```java
// 選項 1: 快取
private final Map<String, Long> auditCache = new ConcurrentHashMap<>();

public long runAuditMatrix(String userId) {
    return auditCache.computeIfAbsent(userId, this::computeAuditMatrix);
}

// 選項 2: 非同步
@Asynchronous
public CompletableFuture<Long> runAuditMatrixAsync(String userId) {
    return CompletableFuture.supplyAsync(() -> computeAuditMatrix(userId));
}
```

**預期改善**: 
- 快取命中: 0ms (100% 提升)
- 非同步: 不阻塞主請求 (用戶體驗大幅改善)

---

#### 3. ReportingService - 不必要的物件創建
**位置**: [`ReportingService.java:78-92`](src/main/java/com/example/camping/service/ReportingService.java:78-92)

**問題描述**:
```java
List<String> results = new ArrayList<>();
for (int i = 0; i < 30000; i++) {
    String item = new String("SCAN-" + orderId + "-ITEM-" + i);  // 刻意避開 String pool
    results.add(item);
}

List<String> filtered = results.stream()
        .filter(s -> s.contains(orderId))  // 所有項目都包含 orderId
        .map(s -> s.toUpperCase())         // 不必要的轉換
        .collect(Collectors.toList());
```

**影響**:
- ⚠️ **記憶體浪費**: 30,000 個 String 物件 + 30,000 個大寫版本
- ⚠️ **CPU 浪費**: 不必要的 filter (所有項目都符合) 和 map
- ⚠️ **執行時間**: 預估 1-2 秒
- ⚠️ **最終只用 10 筆**: 99.97% 的工作都是浪費

**Instana Span**: `camping-reporting-redundant-scan`

**建議修復**:
```java
// 如果只需要 10 筆,為什麼要產生 30,000 筆?
List<String> results = new ArrayList<>(10);
for (int i = 0; i < 10; i++) {
    results.add("SCAN-" + orderId + "-ITEM-" + i);
}
return results;
```

**預期改善**: 執行時間從 1-2 秒降至 < 1ms (1000x 提升)

---

### 🟡 中等問題 (Medium)

#### 4. CheckoutResource - 同步報表處理阻塞請求
**位置**: [`CheckoutResource.java:148-155`](src/main/java/com/example/camping/resource/CheckoutResource.java:148-155)

**問題描述**:
```java
// ⑥ 報表與稽核處理（效能瓶頸）
try {
    reportingService.generateOrderSummary(order.getOrderId());      // 5-10 秒
    reportingService.runAuditMatrix(authenticatedUser.getUserId()); // 2-5 秒
    reportingService.redundantOrderScan(order.getOrderId());        // 1-2 秒
} catch (Exception e) {
    LOGGER.warn("[CHECKOUT] reporting step failed: {}", e.getMessage());
}
```

**影響**:
- ⚠️ **總延遲**: 8-17 秒額外延遲
- ⚠️ **用戶體驗**: Checkout 請求變得非常慢
- ⚠️ **資源浪費**: 即使失敗也只是 log,不影響業務邏輯
- ⚠️ **Instana 追蹤**: 會顯示 checkout 端點異常緩慢

**Instana Span**: `camping-api-checkout` (ENTRY)

**建議修復**:
```java
// 移到非同步處理
executorService.submit(() -> {
    try {
        reportingService.generateOrderSummary(order.getOrderId());
        reportingService.runAuditMatrix(authenticatedUser.getUserId());
        reportingService.redundantOrderScan(order.getOrderId());
    } catch (Exception e) {
        LOGGER.warn("[CHECKOUT] async reporting failed: {}", e.getMessage());
    }
});
```

**預期改善**: Checkout 回應時間從 8-17 秒降至 < 500ms (16-34x 提升)

---

#### 5. 大量 Dead Code (未使用方法)
**位置**: [`ReportingService.java:101-337`](src/main/java/com/example/camping/service/ReportingService.java:101-337)

**問題描述**:
- 237 行程式碼 (70% 的檔案) 從未被呼叫
- 包含 50+ 個未使用的方法
- 增加維護成本和程式碼複雜度

**影響**:
- ⚠️ **程式碼膨脹**: 增加 WAR 檔案大小
- ⚠️ **維護成本**: 開發者需要理解這些無用程式碼
- ⚠️ **潛在 Bug**: 未測試的程式碼可能包含錯誤

**建議修復**:
1. 使用 IDE 的 "Find Usages" 功能確認未使用
2. 刪除所有未使用的方法
3. 如果未來可能需要,移到單獨的 utility 類別

**預期改善**: 
- 程式碼行數減少 70%
- WAR 檔案縮小
- 提升程式碼可讀性

---

### 🟢 輕微問題 (Low)

#### 6. Kafka Producer 同步發送
**位置**: [`KafkaCheckoutService.java:74`](src/main/java/com/example/camping/service/KafkaCheckoutService.java:74)

**問題描述**:
```java
currentProducer.send(record).get();  // 同步等待
```

**影響**:
- ⚠️ **網路延遲**: 等待 Kafka broker 確認 (10-100ms)
- ⚠️ **吞吐量限制**: 無法批次發送

**建議**:
- 目前已經在非同步執行緒中執行,影響較小
- 如果需要更高吞吐量,考慮非同步發送 + callback

---

## 📈 Instana 監控整合分析

### ✅ 優點

#### 1. 完整的追蹤覆蓋
- **自動追蹤**: JAX-RS, Servlet, Kafka, CDI, MongoDB
- **手動追蹤**: 26 個自訂 Span 名稱
- **追蹤工具類**: [`InstanaTracing.java`](src/main/java/com/example/camping/observability/InstanaTracing.java)

#### 2. 豐富的標籤和日誌
```java
SpanSupport.annotate("tags.checkout.order_id", order.getOrderId());
SpanSupport.annotate("tags.checkout.total", String.valueOf(total));
InstanaTracing.logWarn(LOGGER, "[CHECKOUT] accepted - event_id: " + order.getEventId());
```

#### 3. Trace Context 傳遞
- Kafka Headers: ✅ 實作完整
- HTTP Headers: ✅ 實作完整
- 非同步執行: ✅ 使用 ContextSupport

#### 4. 錯誤追蹤
```java
InstanaTracing.error(Span.Type.EXIT, InstanaTracing.KAFKA_SEND_SPAN, e);
```

### ⚠️ 改善建議

#### 1. 效能瓶頸可視化
目前的 Instana 追蹤會清楚顯示:
- `camping-reporting-generate-summary`: 5-10 秒
- `camping-reporting-audit-matrix`: 2-5 秒
- `camping-reporting-redundant-scan`: 1-2 秒

這些 Span 會在 Instana UI 中顯示為紅色/黃色警告。

#### 2. 建議新增的監控指標
```java
// 業務指標
SpanSupport.annotate("business.order.value", String.valueOf(finalTotal));
SpanSupport.annotate("business.coupon.applied", couponCode != null ? "true" : "false");
SpanSupport.annotate("business.nights", String.valueOf(nights));

// 效能指標
long startTime = System.currentTimeMillis();
// ... 執行業務邏輯 ...
SpanSupport.annotate("performance.duration_ms", String.valueOf(System.currentTimeMillis() - startTime));
```

---

## 🎯 優先修復建議

### 第一優先 (立即修復)
1. **ReportingService 字串串接** - 改用 StringBuilder
2. **CheckoutResource 同步報表** - 移到非同步處理

**預期效果**: Checkout 回應時間從 8-17 秒降至 < 500ms

### 第二優先 (本週修復)
3. **ReportingService O(n²) 迴圈** - 實作快取或非同步
4. **ReportingService 不必要物件** - 只產生需要的資料

**預期效果**: 減少 CPU 和記憶體使用 90%

### 第三優先 (下週修復)
5. **刪除 Dead Code** - 清理未使用方法

**預期效果**: 提升程式碼可維護性

---

## 📊 效能改善預估

### 修復前
```
Checkout 請求流程:
├─ HTTP 請求處理: 50ms
├─ 訂單驗證: 100ms
├─ 價格計算 (呼叫 spot-service): 200ms
├─ 優惠券驗證: 50ms
├─ MongoDB 儲存: 100ms
├─ 報表處理 (同步): 8,000-17,000ms ⚠️
└─ Kafka 發送 (非同步): 0ms
總計: 8,500-17,500ms
```

### 修復後
```
Checkout 請求流程:
├─ HTTP 請求處理: 50ms
├─ 訂單驗證: 100ms
├─ 價格計算 (呼叫 spot-service): 200ms
├─ 優惠券驗證: 50ms
├─ MongoDB 儲存: 100ms
├─ 報表處理 (非同步): 0ms ✅
└─ Kafka 發送 (非同步): 0ms
總計: 500ms
```

**改善幅度**: 17-35x 提升 (從 8.5-17.5 秒降至 0.5 秒)

---

## 🔧 實作範例

### 修復 CheckoutResource

```java
@POST
@Span(type = Span.Type.ENTRY, value = InstanaTracing.CHECKOUT_HTTP_SPAN, capturedStackFrames = 5)
public Map<String, Object> receiveCheckout(@Valid @TagParam("order") OrderPayload order) {
    // ... 前面的程式碼保持不變 ...

    // ⑥ 報表與稽核處理 - 改為非同步
    Object reportingSnapshot = ContextSupport.takeSnapshot();
    executorService.submit(() -> {
        ContextSupport.restoreSnapshot(reportingSnapshot);
        try {
            reportingService.generateOrderSummary(order.getOrderId());
            reportingService.runAuditMatrix(authenticatedUser.getUserId());
            reportingService.redundantOrderScan(order.getOrderId());
        } catch (Exception e) {
            LOGGER.warn("[CHECKOUT] async reporting failed: {}", e.getMessage());
        }
    });

    // ⑧ 非同步送 Kafka (保持不變)
    Object kafkaSnapshot = ContextSupport.takeSnapshot();
    executorService.submit(() -> {
        ContextSupport.restoreSnapshot(kafkaSnapshot);
        runCheckoutJob(order);
    });

    // ... 回應保持不變 ...
}
```

### 修復 ReportingService

```java
@Span(type = Span.Type.INTERMEDIATE, value = "camping-reporting-generate-summary", capturedStackFrames = 5)
public String generateOrderSummary(String orderId) {
    InstanaTracing.method("camping-reporting-generate-summary",
            ReportingService.class.getName(), "generateOrderSummary");
    SpanSupport.annotate("report.order_id", orderId);

    LOGGER.warn("[REPORTING] 開始產生訂單摘要: {}", orderId);

    // ✅ 修復: 使用 StringBuilder
    StringBuilder report = new StringBuilder(50000 * 100);
    for (int i = 0; i < 50000; i++) {
        report.append("LINE-").append(i)
              .append("|orderId=").append(orderId)
              .append("|ts=").append(System.currentTimeMillis())
              .append("\n");
    }

    SpanSupport.annotate("report.lines_generated", "50000");
    LOGGER.warn("[REPORTING] 訂單摘要產生完成，長度: {}", report.length());

    return report.substring(0, Math.min(200, report.length()));
}

// 快取實作
private final Map<String, Long> auditCache = new ConcurrentHashMap<>();

@Span(type = Span.Type.INTERMEDIATE, value = "camping-reporting-audit-matrix", capturedStackFrames = 5)
public long runAuditMatrix(String userId) {
    InstanaTracing.method("camping-reporting-audit-matrix",
            ReportingService.class.getName(), "runAuditMatrix");
    SpanSupport.annotate("audit.user_id", userId);

    // ✅ 修復: 使用快取
    return auditCache.computeIfAbsent(userId, this::computeAuditMatrix);
}

private long computeAuditMatrix(String userId) {
    LOGGER.warn("[REPORTING] 開始稽核矩陣計算: userId={}", userId);
    long checksum = 0;
    for (int i = 0; i < 1000; i++) {
        for (int j = 0; j < 1000; j++) {
            checksum += (long)(Math.sqrt(i * j + 1) * Math.log(j + 2) * Math.sin(i + 1));
        }
    }
    LOGGER.warn("[REPORTING] 稽核矩陣完成，checksum={}", checksum);
    return checksum;
}

@Span(type = Span.Type.INTERMEDIATE, value = "camping-reporting-redundant-scan", capturedStackFrames = 5)
public List<String> redundantOrderScan(String orderId) {
    InstanaTracing.method("camping-reporting-redundant-scan",
            ReportingService.class.getName(), "redundantOrderScan");

    LOGGER.warn("[REPORTING] 開始備援掃描: {}", orderId);

    // ✅ 修復: 只產生需要的資料
    List<String> results = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
        results.add("SCAN-" + orderId + "-ITEM-" + i);
    }

    SpanSupport.annotate("scan.result_count", String.valueOf(results.size()));
    LOGGER.warn("[REPORTING] 備援掃描完成，筆數={}", results.size());
    return results;
}
```

---

## 📋 測試計劃

### 1. 效能測試
```bash
# 修復前
curl -X POST http://localhost:8080/camping-api/api/checkout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d @test-order.json \
  -w "\nTime: %{time_total}s\n"

# 預期: 8-17 秒
```

### 2. Instana 驗證
- 檢查 `camping-api-checkout` Span 持續時間
- 確認報表處理 Span 不再阻塞主請求
- 驗證非同步 Span 正確關聯

### 3. 負載測試
```bash
# 使用 Apache Bench
ab -n 100 -c 10 -T application/json -p test-order.json \
   -H "Authorization: Bearer <token>" \
   http://localhost:8080/camping-api/api/checkout
```

---

## 🎓 最佳實踐建議

### 1. 效能優化原則
- ✅ **測量優先**: 使用 Instana 識別真正的瓶頸
- ✅ **非同步處理**: 非關鍵路徑的操作應該非同步
- ✅ **快取策略**: 重複計算的結果應該快取
- ✅ **資源管理**: 避免不必要的物件創建

### 2. Instana 監控原則
- ✅ **有意義的 Span 名稱**: 使用業務術語
- ✅ **豐富的標籤**: 包含關鍵業務資訊
- ✅ **錯誤追蹤**: 捕獲並標記所有錯誤
- ✅ **Context 傳遞**: 確保分散式追蹤完整性

### 3. 程式碼品質原則
- ✅ **刪除 Dead Code**: 定期清理未使用程式碼
- ✅ **單一職責**: 每個方法只做一件事
- ✅ **效能意識**: 了解常見的效能陷阱

---

## 📞 後續行動

### 立即行動 (本週)
1. ✅ 修復 ReportingService 字串串接問題
2. ✅ 將報表處理移到非同步
3. ✅ 部署到測試環境
4. ✅ 使用 Instana 驗證改善

### 短期行動 (下週)
5. ✅ 實作快取機制
6. ✅ 優化不必要的物件創建
7. ✅ 刪除 Dead Code
8. ✅ 負載測試驗證

### 長期行動 (本月)
9. ✅ 建立效能監控儀表板
10. ✅ 設定 Instana 告警規則
11. ✅ 文件化效能最佳實踐
12. ✅ 團隊培訓

---

## 📚 相關文件

- [技術文件](TECHNICAL_DOCUMENTATION.md)
- [Instana SSL 驗證](INSTANA_SSL_VERIFICATION.md)
- [部署指南](INSTANA_SDK_DEPLOYMENT_GUIDE.md)
- [架構概覽](ARCHITECTURE_OVERVIEW.md)

---

## 📝 結論

Camping API 專案已經有良好的 Instana 監控整合,但存在明顯的效能瓶頸。主要問題集中在 [`ReportingService`](src/main/java/com/example/camping/service/ReportingService.java) 的三個方法,這些方法在 Checkout 流程中同步執行,導致請求回應時間達到 8-17 秒。

通過將報表處理移到非同步執行,並修復字串串接、O(n²) 迴圈和不必要物件創建等問題,預期可以將 Checkout 回應時間降至 500ms 以下,改善幅度達 **17-35 倍**。

所有修復都可以在 Instana 中清楚地看到效果,建議優先修復第一和第二優先級的問題,並持續使用 Instana 監控系統效能。

---

**報告產生時間**: 2026-05-27 10:39 (UTC+8)  
**分析工具**: Bob AI Assistant + Instana MCP Server  
**下次審查**: 修復完成後