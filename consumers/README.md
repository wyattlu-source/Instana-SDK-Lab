# Kafka Consumer Services

這個目錄包含三個獨立的 Kafka Consumer 服務,用於處理 camping-api 發送到 `raw_events` topic 的事件。

## 服務列表

### 1. order-processor
**訂單處理服務**
- 消費 raw_events 中的 checkout 事件
- 執行訂單後處理邏輯
- 更新訂單狀態
- 發送到下游 topic: `orders_processed`

### 2. analytics-service
**分析服務**
- 消費 raw_events 中的所有事件
- 計算即時統計數據
- 漏斗分析
- 發送到下游 topics: `funnel_processed`, `revenue_hourly`, `hot_products_5m`

### 3. notification-service
**通知服務**
- 消費 raw_events 中的 checkout 事件
- 發送訂單確認通知
- Email/SMS 通知
- 記錄通知歷史

## 技術棧

- **語言**: Java 17
- **框架**: Jakarta EE 10 (獨立應用程式)
- **Kafka Client**: Apache Kafka 3.7.1
- **序列化**: Confluent Avro
- **監控**: Instana SDK
- **建置工具**: Maven

## 架構

```
raw_events Topic (Kafka)
    ↓
    ├─→ order-processor → orders_processed
    ├─→ analytics-service → funnel_processed, revenue_hourly
    └─→ notification-service → (Email/SMS)
```

## 部署方式

每個服務都是獨立的 Java 應用程式:

```bash
# 建置
cd order-processor
mvn clean package

# 運行
java -jar target/order-processor-1.0.0.jar

# 或使用 Windows Service / systemd
```

## Instana 整合

所有服務都整合了 Instana SDK:
- 自動追蹤 Kafka 消費
- 繼承 Producer 的 Trace Context
- 完整的端到端可觀測性

## 配置

每個服務都支援環境變數配置:
- `KAFKA_BOOTSTRAP_SERVER`: Kafka 伺服器位址
- `SCHEMA_REGISTRY_URL`: Schema Registry URL
- `CONSUMER_GROUP_ID`: Consumer Group ID
- `INSTANA_AGENT_HOST`: Instana Agent 位址

## 開發指南

請參考各服務目錄下的 README.md