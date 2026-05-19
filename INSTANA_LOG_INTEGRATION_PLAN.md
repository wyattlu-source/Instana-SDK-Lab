# Instana 日誌整合計畫
## 在 Instana 上查看 INFO 等級日誌

---

## 📋 目標

在 Instana 監控平台上查看應用程式的 INFO、WARN、ERROR 等各等級日誌,實現完整的可觀測性。

---

## ✅ 可行性評估

**結論: 完全可行!**

Instana 支援多種方式收集和顯示應用程式日誌:
- ✅ Instana Agent 自動日誌收集
- ✅ Instana SDK Event API
- ✅ 標準日誌框架整合 (Log4j/Logback/JUL)
- ✅ 自訂日誌轉發器

---

## 🎯 實作方案比較

### 方案 1: Instana Agent 日誌檔案收集 ⭐ 推薦

**優點:**
- ✅ 無需修改應用程式程式碼
- ✅ 自動關聯追蹤 (Trace) 和日誌
- ✅ 支援多種日誌格式
- ✅ 效能影響最小

**缺點:**
- ⚠️ 需要配置 Agent
- ⚠️ 需要日誌檔案路徑

**適用場景:**
- 已有日誌檔案輸出
- 不想修改程式碼
- 需要收集多個應用程式的日誌

---

### 方案 2: Instana SDK Event API

**優點:**
- ✅ 即時發送日誌到 Instana
- ✅ 可自訂日誌格式和欄位
- ✅ 與現有 SDK 整合良好
- ✅ 精確控制要發送的日誌

**缺點:**
- ⚠️ 需要修改程式碼
- ⚠️ 輕微效能開銷

**適用場景:**
- 需要即時日誌
- 想要自訂日誌欄位
- 已使用 Instana SDK

---

### 方案 3: 標準日誌框架整合

**優點:**
- ✅ 使用標準 Java 日誌 API
- ✅ 易於維護和測試
- ✅ 可同時輸出到多個目標

**缺點:**
- ⚠️ 需要配置日誌框架
- ⚠️ 需要 Instana Agent 配合

**適用場景:**
- 使用 Log4j/Logback
- 需要標準化日誌管理
- 團隊熟悉日誌框架

---

## 📝 建議實作方案

### 階段 1: 快速啟用 (方案 2 - SDK Event API)

**為什麼選擇這個方案:**
- 專案已整合 Instana SDK
- 可立即使用,無需額外配置
- 與現有追蹤功能完美整合

**實作步驟:**

#### 步驟 1: 擴展 InstanaTracingUtil.java

在 `src/main/java/com/example/camping/util/InstanaTracingUtil.java` 加入日誌方法:

```java
/**
 * 發送日誌到 Instana
 * @param level 日誌等級 (INFO, WARN, ERROR)
 * @param message 日誌訊息
 */
public static void logToInstana(String level, String message) {
    if (!SDK_ENABLED) return;
    
    SpanSupport.annotate("log.level", level);
    SpanSupport.annotate("log.message", message);
    SpanSupport.annotate("log.timestamp", String.valueOf(System.currentTimeMillis()));
    SpanSupport.annotate("log.source", "camping-api");
    
    // 同時記錄到標準日誌
    switch (level.toUpperCase()) {
        case "INFO":
            LOGGER.info(String.format("[INSTANA-LOG] %s", message));
            break;
        case "WARN":
            LOGGER.warning(String.format("[INSTANA-LOG] %s", message));
            break;
        case "ERROR":
            LOGGER.severe(String.format("[INSTANA-LOG] %s", message));
            break;
        default:
            LOGGER.info(String.format("[INSTANA-LOG] %s: %s", level, message));
    }
}

/**
 * 發送帶有額外資訊的日誌
 */
public static void logToInstana(String level, String message, Map<String, String> context) {
    if (!SDK_ENABLED) return;
    
    logToInstana(level, message);
    
    // 加入額外的上下文資訊
    if (context != null) {
        context.forEach((key, value) -> 
            SpanSupport.annotate("log.context." + key, value)
        );
    }
}

/**
 * 快捷方法
 */
public static void logInfo(String message) {
    logToInstana("INFO", message);
}

public static void logWarn(String message) {
    logToInstana("WARN", message);
}

public static void logError(String message) {
    logToInstana("ERROR", message);
}

public static void logError(String message, Throwable throwable) {
    if (!SDK_ENABLED) return;
    
    logToInstana("ERROR", message);
    SpanSupport.annotate("log.error.type", throwable.getClass().getName());
    SpanSupport.annotate("log.error.message", throwable.getMessage());
    SpanSupport.annotate("log.error.stacktrace", getStackTrace(throwable));
}

private static String getStackTrace(Throwable throwable) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);
    return sw.toString();
}
```

#### 步驟 2: 在應用程式中使用

**範例 1: 在 Resource 中記錄請求**

```java
// CheckoutResource.java
@POST
@Path("/checkout")
public Response checkout(OrderPayload payload) {
    InstanaTracingUtil.logInfo("收到結帳請求: userId=" + payload.getUserId());
    
    try {
        // 業務邏輯
        Order order = processCheckout(payload);
        
        InstanaTracingUtil.logInfo("結帳成功: orderId=" + order.getId());
        return Response.ok(order).build();
        
    } catch (Exception e) {
        InstanaTracingUtil.logError("結帳失敗", e);
        throw e;
    }
}
```

**範例 2: 在 Service 中記錄業務邏輯**

```java
// PricingService.java
public double calculatePrice(OrderPayload payload) {
    InstanaTracingUtil.logInfo("開始計算價格");
    
    double basePrice = getBasePrice(payload);
    InstanaTracingUtil.logInfo("基礎價格: " + basePrice);
    
    if (payload.getCouponCode() != null) {
        InstanaTracingUtil.logWarn("使用優惠券: " + payload.getCouponCode());
        double discount = applyDiscount(payload.getCouponCode());
        InstanaTracingUtil.logInfo("折扣金額: " + discount);
    }
    
    return finalPrice;
}
```

**範例 3: 記錄帶有上下文的日誌**

```java
Map<String, String> context = new HashMap<>();
context.put("userId", user.getId());
context.put("spotId", spot.getId());
context.put("action", "favorite");

InstanaTracingUtil.logToInstana("INFO", "使用者收藏景點", context);
```

#### 步驟 3: 驗證日誌

1. **啟動應用程式**
   ```bash
   # 確保 SDK 已啟用
   export INSTANA_SDK_ENABLED=true
   $JBOSS_HOME/bin/standalone.sh
   ```

2. **觸發日誌記錄**
   ```bash
   # 呼叫 API 產生日誌
   curl -X POST http://localhost:8080/camping-api/checkout \
     -H "Content-Type: application/json" \
     -d '{"userId":"user123","spotId":"spot456"}'
   ```

3. **在 Instana 查看**
   - 登入 Instana Dashboard
   - 選擇 "Applications" → "camping-api"
   - 點選 "Traces" 標籤
   - 選擇一個 Trace
   - 在 Span 詳細資訊中查看 "Tags"
   - 尋找 `log.level`, `log.message` 等標籤

---

### 階段 2: 完整整合 (方案 1 - Agent 日誌收集)

**實作步驟:**

#### 步驟 1: 配置 JBoss 日誌輸出

確保 JBoss 有輸出日誌檔案:

```xml
<!-- standalone.xml -->
<subsystem xmlns="urn:jboss:domain:logging:8.0">
    <periodic-rotating-file-handler name="FILE" autoflush="true">
        <level name="INFO"/>
        <formatter>
            <named-formatter name="PATTERN"/>
        </formatter>
        <file relative-to="jboss.server.log.dir" path="server.log"/>
        <suffix value=".yyyy-MM-dd"/>
        <append value="true"/>
    </periodic-rotating-file-handler>
    
    <root-logger>
        <level name="INFO"/>
        <handlers>
            <handler name="CONSOLE"/>
            <handler name="FILE"/>
        </handlers>
    </root-logger>
</subsystem>
```

#### 步驟 2: 更新 Instana Agent 配置

修改 `instana-agent-config.json`:

```json
{
  "com.instana.plugin.javatrace": {
    "instrumentation": {
      "enabled": true,
      "opentracing": true,
      "sdk": {
        "packages": [
          "com.example.camping",
          "com.example.spot"
        ]
      }
    },
    "tracing": {
      "enabled": true,
      "stack-trace-length": 50,
      "max-exit-calls": 500,
      "max-entry-calls": 500,
      "detailed-errors": true
    }
  },
  "com.instana.plugin.log": {
    "enabled": true,
    "files": [
      {
        "path": "${JBOSS_HOME}/standalone/log/server.log",
        "level": "INFO",
        "pattern": "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}) (INFO|WARN|ERROR|DEBUG) \\[(.+?)\\] \\((.+?)\\) (.+)$",
        "fields": {
          "timestamp": 1,
          "level": 2,
          "logger": 3,
          "thread": 4,
          "message": 5
        }
      }
    ],
    "multiline": {
      "pattern": "^\\d{4}-\\d{2}-\\d{2}",
      "negate": true,
      "match": "after"
    }
  }
}
```

#### 步驟 3: 重啟 Instana Agent

```bash
# Linux
sudo systemctl restart instana-agent

# Windows
net stop instana-agent
net start instana-agent

# Docker
docker restart instana-agent
```

#### 步驟 4: 驗證日誌收集

1. **檢查 Agent 狀態**
   ```bash
   # Linux
   sudo systemctl status instana-agent
   
   # 查看 Agent 日誌
   tail -f /opt/instana/agent/data/log/agent.log
   ```

2. **在 Instana Dashboard 查看**
   - 進入 "Infrastructure" → "Hosts"
   - 選擇你的主機
   - 查看 "Logs" 標籤
   - 應該能看到 server.log 的內容

---

### 階段 3: 進階整合 (方案 3 - Logback 整合)

#### 步驟 1: 加入 Logback 依賴

在 `pom.xml` 加入:

```xml
<dependencies>
    <!-- Logback -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.14</version>
    </dependency>
    
    <!-- SLF4J API -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

#### 步驟 2: 建立 Logback 配置

建立 `src/main/resources/logback.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/camping-api.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/camping-api.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- JSON Appender for Instana -->
    <appender name="JSON" class="ch.qos.logback.core.FileAppender">
        <file>logs/camping-api-json.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
    </appender>
    
    <!-- Logger Configuration -->
    <logger name="com.example.camping" level="INFO" additivity="false">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
        <appender-ref ref="JSON" />
    </logger>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

#### 步驟 3: 在程式碼中使用 SLF4J

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckoutResource {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutResource.class);
    
    @POST
    @Path("/checkout")
    public Response checkout(OrderPayload payload) {
        logger.info("收到結帳請求: userId={}", payload.getUserId());
        
        try {
            Order order = processCheckout(payload);
            logger.info("結帳成功: orderId={}, amount={}", order.getId(), order.getAmount());
            return Response.ok(order).build();
            
        } catch (Exception e) {
            logger.error("結帳失敗: userId={}", payload.getUserId(), e);
            throw e;
        }
    }
}
```

---

## 📊 在 Instana 查看日誌

### 方法 1: 在 Trace 中查看

1. 進入 Instana Dashboard
2. 選擇 "Applications" → "camping-api"
3. 點選 "Traces" 標籤
4. 選擇一個 Trace
5. 在 Span 詳細資訊中:
   - 查看 "Tags" 標籤頁
   - 尋找 `log.*` 開頭的標籤
   - 例如: `log.level=INFO`, `log.message=結帳成功`

### 方法 2: 在 Events 中查看

1. 進入 "Events" 頁面
2. 使用過濾器:
   - Type: "Log"
   - Level: "INFO" / "WARN" / "ERROR"
   - Service: "camping-api"
3. 查看日誌列表和詳細資訊

### 方法 3: 在 Infrastructure 中查看

1. 進入 "Infrastructure" → "Hosts"
2. 選擇你的主機
3. 點選 "Logs" 標籤
4. 使用搜尋和過濾功能

### 方法 4: 使用 Instana Query Language

```
entity.type:log AND log.level:INFO AND entity.service.name:camping-api
```

---

## 🔍 日誌過濾和搜尋

### 按等級過濾

```
log.level:INFO
log.level:WARN
log.level:ERROR
```

### 按時間範圍

```
@timestamp:[now-1h TO now]
```

### 按關鍵字搜尋

```
log.message:*結帳*
log.message:*錯誤*
```

### 組合查詢

```
log.level:ERROR AND entity.service.name:camping-api AND @timestamp:[now-1h TO now]
```

---

## 📈 效能考量

### SDK Event API 方式

- **記憶體開銷**: 每個日誌約 1-2 KB
- **CPU 開銷**: 可忽略 (< 0.1%)
- **網路開銷**: 與 Trace 一起發送,無額外連線

**建議:**
- 只記錄重要的業務日誌
- 避免在迴圈中大量記錄
- 使用 `SDK_ENABLED` 開關控制

### Agent 日誌收集方式

- **記憶體開銷**: Agent 端處理,應用程式無影響
- **CPU 開銷**: 可忽略
- **磁碟 I/O**: 取決於日誌量

**建議:**
- 設定日誌輪轉 (log rotation)
- 限制日誌檔案大小
- 定期清理舊日誌

---

## 🧪 測試計畫

### 測試 1: 驗證日誌發送

```bash
# 1. 啟動應用程式
export INSTANA_SDK_ENABLED=true
$JBOSS_HOME/bin/standalone.sh

# 2. 呼叫 API
curl -X GET http://localhost:8080/camping-api/spots

# 3. 檢查日誌
tail -f $JBOSS_HOME/standalone/log/server.log | grep "INSTANA-LOG"

# 4. 在 Instana 查看
# 應該能在 Trace 的 Tags 中看到 log.* 標籤
```

### 測試 2: 驗證不同等級

```java
// 在測試端點中
@GET
@Path("/test-logs")
public Response testLogs() {
    InstanaTracingUtil.logInfo("這是 INFO 日誌");
    InstanaTracingUtil.logWarn("這是 WARN 日誌");
    InstanaTracingUtil.logError("這是 ERROR 日誌");
    return Response.ok("日誌測試完成").build();
}
```

### 測試 3: 效能測試

```bash
# 使用 Apache Bench 測試
ab -n 1000 -c 10 http://localhost:8080/camping-api/spots

# 比較啟用/停用日誌的效能差異
```

---

## 📋 部署檢查清單

### 開發環境

- [ ] 更新 `InstanaTracingUtil.java` 加入日誌方法
- [ ] 在關鍵業務邏輯中加入日誌記錄
- [ ] 本地測試日誌功能
- [ ] 驗證 Instana 可以看到日誌

### 測試環境

- [ ] 部署更新後的應用程式
- [ ] 配置 Instana Agent (如使用方案 1)
- [ ] 執行整合測試
- [ ] 驗證日誌收集正常
- [ ] 效能測試

### 生產環境

- [ ] 審查日誌記錄點
- [ ] 確認效能影響可接受
- [ ] 準備回滾計畫
- [ ] 部署到生產環境
- [ ] 監控系統狀態
- [ ] 驗證日誌可見性

---

## 🎯 最佳實踐

### 1. 日誌等級使用

- **INFO**: 重要的業務流程
  ```java
  InstanaTracingUtil.logInfo("使用者登入成功: userId=" + userId);
  ```

- **WARN**: 潛在問題或異常情況
  ```java
  InstanaTracingUtil.logWarn("優惠券即將過期: couponId=" + couponId);
  ```

- **ERROR**: 錯誤和例外
  ```java
  InstanaTracingUtil.logError("資料庫連線失敗", exception);
  ```

### 2. 日誌訊息格式

```java
// ✅ 好的做法
InstanaTracingUtil.logInfo("結帳成功: orderId=" + orderId + ", amount=" + amount);

// ❌ 避免
InstanaTracingUtil.logInfo("成功");  // 太簡略
InstanaTracingUtil.logInfo(order.toString());  // 太詳細
```

### 3. 敏感資訊處理

```java
// ❌ 不要記錄敏感資訊
InstanaTracingUtil.logInfo("密碼: " + password);
InstanaTracingUtil.logInfo("信用卡號: " + cardNumber);

// ✅ 遮罩敏感資訊
InstanaTracingUtil.logInfo("信用卡號: ****" + cardNumber.substring(12));
```

### 4. 效能優化

```java
// ✅ 使用 SDK_ENABLED 檢查
if (SDK_ENABLED) {
    String expensiveData = generateExpensiveData();
    InstanaTracingUtil.logInfo("資料: " + expensiveData);
}

// ❌ 避免不必要的計算
InstanaTracingUtil.logInfo("資料: " + generateExpensiveData());
```

---

## 🔧 疑難排解

### 問題 1: 在 Instana 看不到日誌

**可能原因:**
1. SDK 未啟用
2. Instana Agent 未運行
3. 日誌未正確發送

**解決方案:**
```bash
# 檢查 SDK 狀態
echo $INSTANA_SDK_ENABLED

# 檢查應用程式日誌
tail -f $JBOSS_HOME/standalone/log/server.log | grep "INSTANA"

# 檢查 Instana Agent
sudo systemctl status instana-agent
```

### 問題 2: 日誌量太大

**解決方案:**
1. 調整日誌等級
2. 減少日誌記錄點
3. 使用取樣 (sampling)

```java
// 只記錄 10% 的請求
if (Math.random() < 0.1) {
    InstanaTracingUtil.logInfo("請求詳情...");
}
```

### 問題 3: 效能下降

**解決方案:**
1. 停用 SDK: `export INSTANA_SDK_ENABLED=false`
2. 使用非同步日誌
3. 減少日誌詳細程度

---

## 📚 相關文件

- [INSTANA_SDK_DEPLOYMENT_GUIDE.md](INSTANA_SDK_DEPLOYMENT_GUIDE.md) - SDK 部署指南
- [SDK_TOGGLE_DESIGN.md](SDK_TOGGLE_DESIGN.md) - SDK 開關設計
- [TESTING.md](TESTING.md) - 測試指南
- [Instana 官方文件](https://www.ibm.com/docs/en/instana-observability)

---

## 📞 支援資源

- **Instana 文件**: https://www.ibm.com/docs/en/instana-observability
- **Instana SDK API**: https://www.ibm.com/docs/en/instana-observability/current?topic=apis-instana-sdk
- **社群論壇**: https://community.ibm.com/community/user/aiops/communities/community-home?CommunityKey=7467d4b3-5f1e-4b6e-9e1e-9e1e9e1e9e1e

---

## 📝 總結

### 快速開始 (5 分鐘)

1. 更新 `InstanaTracingUtil.java` 加入日誌方法
2. 在程式碼中使用 `InstanaTracingUtil.logInfo("訊息")`
3. 重新部署應用程式
4. 在 Instana Trace 的 Tags 中查看日誌

### 完整整合 (30 分鐘)

1. 配置 Instana Agent 日誌收集
2. 設定 Logback 日誌框架
3. 在 Instana Dashboard 查看日誌
4. 設定告警和儀表板

### 建議實作順序

1. ✅ **階段 1**: 使用 SDK Event API (立即可用)
2. ✅ **階段 2**: 配置 Agent 日誌收集 (完整功能)
3. ✅ **階段 3**: 整合 Logback (標準化)

---

**最後更新**: 2026-05-18  
**版本**: 1.0.0  
**作者**: Bob (AI Software Engineer)