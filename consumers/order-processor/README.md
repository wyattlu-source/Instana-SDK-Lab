# Order Processor Service

訂單處理服務 - 消費 Kafka `raw_events` topic 中的 checkout 事件並執行後處理邏輯。

## 功能

- ✅ 消費 `raw_events` topic
- ✅ 過濾並處理 `event_type = "checkout"` 的事件
- ✅ 執行訂單後處理邏輯
- ✅ 發送到下游 topic: `orders_processed`
- ✅ 完整的 Instana 分散式追蹤
- ✅ 優雅的關閉機制
- ✅ 錯誤處理和重試

## 技術棧

- Java 17
- Apache Kafka 3.7.1
- Confluent Avro 7.7.1
- Instana SDK 1.1.28
- SLF4J Logging

## 建置

```bash
# 使用 Maven 建置
mvn clean package

# 產生的檔案:
# target/order-processor-1.0.0.jar (主程式)
# target/order-processor-1.0.0-jar-with-dependencies.jar (包含所有依賴)
# target/lib/ (所有依賴 JAR)
```

## 執行

### 方式 1: 使用 fat JAR (推薦)

```bash
java -jar target/order-processor-1.0.0-jar-with-dependencies.jar
```

### 方式 2: 使用 classpath

```bash
java -cp "target/order-processor-1.0.0.jar:target/lib/*" \
     com.example.consumer.order.OrderProcessorApp
```

### 方式 3: 使用 PowerShell 腳本

```powershell
.\run.ps1
```

## 配置

透過環境變數配置:

| 環境變數 | 預設值 | 說明 |
|---------|--------|------|
| `KAFKA_BOOTSTRAP_SERVER` | `10.107.85.239:9092` | Kafka 伺服器位址 |
| `SCHEMA_REGISTRY_URL` | `http://10.107.85.239:8081` | Schema Registry URL |
| `CONSUMER_GROUP_ID` | `order-processor-group` | Consumer Group ID |
| `SOURCE_TOPIC` | `raw_events` | 來源 Topic |
| `TARGET_TOPIC` | `orders_processed` | 目標 Topic |
| `POLL_TIMEOUT_MS` | `1000` | Poll 超時時間 (毫秒) |
| `MAX_POLL_RECORDS` | `100` | 每次 Poll 最大記錄數 |
| `INSTANA_AGENT_HOST` | `localhost` | Instana Agent 主機 |
| `INSTANA_AGENT_PORT` | `42699` | Instana Agent 埠號 |

### 設定環境變數範例

**Windows PowerShell:**
```powershell
$env:KAFKA_BOOTSTRAP_SERVER="10.107.85.239:9092"
$env:CONSUMER_GROUP_ID="order-processor-group-1"
java -jar target/order-processor-1.0.0-jar-with-dependencies.jar
```

**Linux/Mac:**
```bash
export KAFKA_BOOTSTRAP_SERVER="10.107.85.239:9092"
export CONSUMER_GROUP_ID="order-processor-group-1"
java -jar target/order-processor-1.0.0-jar-with-dependencies.jar
```

## 整合 Instana

### 使用 Instana Java Agent

```bash
java -javaagent:/path/to/instana-agent.jar \
     -jar target/order-processor-1.0.0-jar-with-dependencies.jar
```

### Trace 繼承

服務會自動從 Kafka message headers 中讀取 Instana trace context:
- `X-Instana-T`: Trace ID
- `X-Instana-S`: Span ID
- `X-Instana-L`: Level

這樣可以實現從 camping-api → Kafka → order-processor 的完整追蹤鏈。

## 監控指標

服務會記錄以下指標:

- **處理數量**: 已處理的訂單總數
- **總金額**: 已處理訂單的總金額
- **錯誤數**: 處理失敗的訂單數
- **Consumer Lag**: Kafka consumer 延遲

## 日誌

日誌輸出到 stdout,包含:

- 啟動和關閉訊息
- 每筆訂單的處理詳情
- 錯誤和異常
- 統計資訊 (每 10 筆訂單)

### 日誌範例

```
[INFO] === Order Processor Service Starting ===
[INFO] Kafka Consumer initialized
[INFO]   Bootstrap Servers: 10.107.85.239:9092
[INFO]   Group ID: order-processor-group
[INFO]   Topic: raw_events
[INFO] Subscribed to topic: raw_events
[INFO] Starting consume loop...
[INFO] Polled 5 records
[INFO] Processing checkout event: order_id=ord_001, user=user@example.com, amount=2900
[INFO] → Sending to orders_processed topic: order_id=ord_001
[INFO] → Updating inventory: product_id=spot_taipei101, order_id=ord_001
[INFO] → Triggering notification: user=user@example.com, order_id=ord_001
[INFO] → Logging to data warehouse: order_id=ord_001, amount=2900
[INFO] ✓ Order processed successfully:
[INFO]   Order ID: ord_001
[INFO]   User: user@example.com
[INFO]   Product: 台北101觀景台
[INFO]   Amount: $2900
[INFO]   Status: confirmed
[INFO]   Timestamp: 2026-05-20 17:30:00
```

## 部署

### Windows Service

使用 NSSM (Non-Sucking Service Manager):

```powershell
# 安裝服務
nssm install OrderProcessor "C:\Program Files\Java\jdk-17\bin\java.exe"
nssm set OrderProcessor AppParameters "-jar C:\path\to\order-processor-1.0.0-jar-with-dependencies.jar"
nssm set OrderProcessor AppDirectory "C:\path\to\consumers\order-processor"
nssm set OrderProcessor DisplayName "Order Processor Service"
nssm set OrderProcessor Description "Kafka Consumer for processing checkout orders"

# 啟動服務
nssm start OrderProcessor

# 查看狀態
nssm status OrderProcessor

# 停止服務
nssm stop OrderProcessor
```

### Linux systemd

建立 `/etc/systemd/system/order-processor.service`:

```ini
[Unit]
Description=Order Processor Service
After=network.target

[Service]
Type=simple
User=kafka
WorkingDirectory=/opt/order-processor
ExecStart=/usr/bin/java -jar /opt/order-processor/order-processor-1.0.0-jar-with-dependencies.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

啟動服務:

```bash
sudo systemctl daemon-reload
sudo systemctl enable order-processor
sudo systemctl start order-processor
sudo systemctl status order-processor
```

### Docker

```dockerfile
FROM openjdk:17-slim

WORKDIR /app

COPY target/order-processor-1.0.0-jar-with-dependencies.jar app.jar

ENV KAFKA_BOOTSTRAP_SERVER=10.107.85.239:9092
ENV SCHEMA_REGISTRY_URL=http://10.107.85.239:8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

建置和執行:

```bash
docker build -t order-processor:1.0.0 .
docker run -d --name order-processor \
  -e KAFKA_BOOTSTRAP_SERVER=10.107.85.239:9092 \
  order-processor:1.0.0
```

## 故障排除

### Consumer 無法連接到 Kafka

檢查:
1. Kafka broker 是否運行: `telnet 10.107.85.239 9092`
2. 網路連線是否正常
3. 防火牆設定

### Schema Registry 錯誤

檢查:
1. Schema Registry 是否運行: `curl http://10.107.85.239:8081/subjects`
2. Schema 是否已註冊: `curl http://10.107.85.239:8081/subjects/raw_events-value/versions/latest`

### Consumer Lag 過高

解決方案:
1. 增加 Consumer 實例數 (最多 3 個,對應 3 個 partitions)
2. 增加 `MAX_POLL_RECORDS`
3. 優化處理邏輯

### Instana Trace 中斷

檢查:
1. Kafka message headers 是否包含 Instana trace headers
2. Instana Agent 是否運行
3. 日誌中是否有 trace 相關錯誤

## 開發

### 執行測試

```bash
mvn test
```

### 本地開發

```bash
# 編譯
mvn compile

# 執行
mvn exec:java -Dexec.mainClass="com.example.consumer.order.OrderProcessorApp"
```

## 授權

Copyright © 2026 Example Corp.