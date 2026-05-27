# Kafka Consumer Services - 快速開始指南

## 📋 概覽

本專案包含三個 Kafka Consumer 服務,用於處理 camping-api 發送到 `raw_events` topic 的事件。

### 已完成的服務

✅ **order-processor** - 訂單處理服務 (完整實作)

### 計劃中的服務

⏳ **analytics-service** - 分析服務 (可基於 order-processor 範本建立)
⏳ **notification-service** - 通知服務 (可基於 order-processor 範本建立)

---

## 🚀 快速開始 - order-processor

### 1. 前置需求

- ✅ Java 17 或更高版本
- ✅ Maven 3.6 或更高版本
- ✅ Kafka 運行在 `10.107.85.239:9092`
- ✅ Schema Registry 運行在 `http://10.107.85.239:8081`
- ✅ camping-api 已部署並發送事件到 `raw_events`

### 2. 建置服務

```powershell
cd consumers/order-processor
.\build.ps1
```

或使用 Maven:

```bash
cd consumers/order-processor
mvn clean package
```

### 3. 執行服務

```powershell
.\run.ps1
```

或直接執行 JAR:

```bash
java -jar target/order-processor-1.0.0-jar-with-dependencies.jar
```

### 4. 驗證服務運行

服務啟動後,你應該看到類似的日誌:

```
[INFO] === Order Processor Service Starting ===
[INFO] Version: 1.0.0
[INFO] Java Version: 17.0.x
[INFO] === Configuration ===
[INFO] Kafka Bootstrap Server: 10.107.85.239:9092
[INFO] Schema Registry URL: http://10.107.85.239:8081
[INFO] Consumer Group ID: order-processor-group
[INFO] Source Topic: raw_events
[INFO] Target Topic: orders_processed
[INFO] ====================
[INFO] Kafka Consumer initialized
[INFO] Subscribed to topic: raw_events
[INFO] Starting consume loop...
```

### 5. 測試服務

在另一個終端機執行 camping-api 的 checkout 測試:

```powershell
cd c:\Users\Administrator\Desktop\Instana-SDK-Lab-clone
.\test-checkout-with-auth.ps1
```

你應該在 order-processor 的日誌中看到:

```
[INFO] Polled 1 records
[INFO] Processing checkout event: order_id=ord_xxx, user=user@example.com, amount=2900
[INFO] → Sending to orders_processed topic: order_id=ord_xxx
[INFO] → Updating inventory: product_id=spot_taipei101, order_id=ord_xxx
[INFO] → Triggering notification: user=user@example.com, order_id=ord_xxx
[INFO] ✓ Order processed successfully:
[INFO]   Order ID: ord_xxx
[INFO]   User: user@example.com
[INFO]   Product: 台北101觀景台
[INFO]   Amount: $2900
[INFO]   Status: confirmed
```

---

## 🔧 配置

### 環境變數

```powershell
# Kafka 配置
$env:KAFKA_BOOTSTRAP_SERVER="10.107.85.239:9092"
$env:SCHEMA_REGISTRY_URL="http://10.107.85.239:8081"

# Consumer 配置
$env:CONSUMER_GROUP_ID="order-processor-group"
$env:SOURCE_TOPIC="raw_events"
$env:TARGET_TOPIC="orders_processed"

# 效能調整
$env:POLL_TIMEOUT_MS="1000"
$env:MAX_POLL_RECORDS="100"

# Instana 配置
$env:INSTANA_AGENT_HOST="localhost"
$env:INSTANA_AGENT_PORT="42699"
```

---

## 📊 監控

### 查看 Consumer Group 狀態

```bash
# 列出所有 Consumer Groups
kafka-consumer-groups --bootstrap-server 10.107.85.239:9092 --list

# 查看 order-processor-group 的詳細資訊
kafka-consumer-groups --bootstrap-server 10.107.85.239:9092 \
  --group order-processor-group --describe
```

### 查看 Consumer Lag

```bash
kafka-consumer-groups --bootstrap-server 10.107.85.239:9092 \
  --group order-processor-group --describe

# 輸出範例:
# GROUP                TOPIC       PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
# order-processor-group raw_events  0          150             150             0
# order-processor-group raw_events  1          148             148             0
# order-processor-group raw_events  2          152             152             0
```

### Instana 追蹤

在 Instana UI 中:

1. 搜尋 `service:order-processor`
2. 查看 Trace 列表
3. 點擊任一 Trace 查看完整的追蹤鏈:
   ```
   camping-api (POST /api/checkout)
     → MongoDB (save order)
     → Kafka Producer (send to raw_events)
     → order-processor (consume from raw_events)
       → Process Order
       → Send to orders_processed
   ```

---

## 🏗️ 建立其他服務

### 基於 order-processor 範本建立新服務

order-processor 已經是一個完整的範本,你可以複製它來建立其他服務:

#### 1. 複製 order-processor

```powershell
# 複製整個目錄
Copy-Item -Path "consumers/order-processor" -Destination "consumers/analytics-service" -Recurse

cd consumers/analytics-service
```

#### 2. 修改 pom.xml

```xml
<artifactId>analytics-service</artifactId>
<name>Analytics Service</name>

<!-- 修改 mainClass -->
<mainClass>com.example.consumer.analytics.AnalyticsApp</mainClass>
```

#### 3. 重新命名 Java 套件

```
src/main/java/com/example/consumer/order/
  → src/main/java/com/example/consumer/analytics/
```

#### 4. 修改業務邏輯

在 `AnalyticsProcessor.java` 中實作分析邏輯:

```java
public class AnalyticsProcessor {
    public void process(RawEvent event) {
        // 漏斗分析
        analyzeFunnel(event);
        
        // 營收統計
        calculateRevenue(event);
        
        // 熱門產品追蹤
        trackHotProducts(event);
    }
}
```

#### 5. 建置和執行

```powershell
.\build.ps1
.\run.ps1
```

---

## 📁 專案結構

```
consumers/
├── README.md                    # 總覽文件
├── QUICK_START.md              # 本文件
│
├── order-processor/            # ✅ 訂單處理服務 (已完成)
│   ├── pom.xml
│   ├── README.md
│   ├── build.ps1
│   ├── run.ps1
│   └── src/main/java/com/example/consumer/order/
│       ├── OrderProcessorApp.java      # 主程式
│       ├── OrderConsumer.java          # Kafka Consumer
│       ├── OrderProcessor.java         # 業務邏輯
│       ├── config/AppConfig.java       # 配置
│       └── model/RawEvent.java         # 資料模型
│
├── analytics-service/          # ⏳ 分析服務 (待建立)
│   └── (可基於 order-processor 範本建立)
│
└── notification-service/       # ⏳ 通知服務 (待建立)
    └── (可基於 order-processor 範本建立)
```

---

## 🎯 完整的資料流程

```
使用者 → camping-api
           ↓
    [處理訂單邏輯]
           ↓
    MongoDB (儲存訂單)
           ↓
    Kafka Producer
           ↓
    raw_events Topic (3 partitions)
           ↓
    ┌──────┴──────┬──────────────┐
    ↓             ↓              ↓
order-processor  analytics-    notification-
                 service        service
    ↓             ↓              ↓
orders_processed funnel_        Email/SMS
                 processed,
                 revenue_hourly
```

---

## 🔍 故障排除

### 問題 1: 無法連接到 Kafka

**症狀**: `Connection refused` 或 `Timeout`

**解決方案**:
```bash
# 測試連線
telnet 10.107.85.239 9092

# 檢查 Kafka 是否運行
# (在 Kafka 伺服器上)
systemctl status kafka
```

### 問題 2: Schema Registry 錯誤

**症狀**: `Schema not found` 或 `Failed to deserialize`

**解決方案**:
```bash
# 檢查 Schema Registry
curl http://10.107.85.239:8081/subjects

# 檢查 raw_events schema
curl http://10.107.85.239:8081/subjects/raw_events-value/versions/latest
```

### 問題 3: Consumer Lag 持續增加

**症狀**: LAG 數字不斷增加

**解決方案**:
1. 增加 Consumer 實例數 (最多 3 個)
2. 增加 `MAX_POLL_RECORDS`
3. 優化處理邏輯

### 問題 4: 沒有收到訊息

**症狀**: `Polled 0 records`

**檢查清單**:
- [ ] camping-api 是否正常運行?
- [ ] 是否有執行 checkout 測試?
- [ ] raw_events topic 是否有資料?
- [ ] Consumer Group ID 是否正確?

```bash
# 檢查 topic 是否有資料
kafka-console-consumer --bootstrap-server 10.107.85.239:9092 \
  --topic raw_events --from-beginning --max-messages 1
```

---

## 📚 相關文件

- [consumers/README.md](README.md) - 服務總覽
- [order-processor/README.md](order-processor/README.md) - order-processor 詳細文件
- [../INSTANA_MCP_TEST_REPORT.md](../INSTANA_MCP_TEST_REPORT.md) - Instana 測試報告
- [../HOW_TO_USE_INSTANA_MCP.md](../HOW_TO_USE_INSTANA_MCP.md) - Instana MCP 使用指南

---

## ✅ 檢查清單

### 部署前檢查

- [ ] Java 17 已安裝
- [ ] Maven 已安裝
- [ ] Kafka 可連接
- [ ] Schema Registry 可連接
- [ ] camping-api 正常運行
- [ ] raw_events topic 已建立

### 部署後檢查

- [ ] 服務成功啟動
- [ ] 成功訂閱 raw_events topic
- [ ] 可以接收並處理訊息
- [ ] Consumer Lag 為 0 或接近 0
- [ ] Instana 可以追蹤到服務
- [ ] 日誌正常輸出

---

## 🎉 下一步

1. ✅ **order-processor 已完成** - 可以開始使用
2. ⏳ **建立 analytics-service** - 基於 order-processor 範本
3. ⏳ **建立 notification-service** - 基於 order-processor 範本
4. 📊 **設定監控告警** - 在 Instana 中設定
5. 🚀 **生產環境部署** - 使用 Windows Service 或 Docker

---

## 💡 提示

- order-processor 是一個**完整可運行的範本**
- 可以直接複製並修改來建立其他服務
- 所有核心功能都已實作:
  - ✅ Kafka Consumer
  - ✅ Avro 反序列化
  - ✅ Instana 追蹤
  - ✅ 錯誤處理
  - ✅ 優雅關閉
  - ✅ 配置管理

**開始使用 order-processor,然後根據需求擴展其他服務!** 🚀