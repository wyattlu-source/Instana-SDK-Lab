# Camping API 技術文件

## 📋 目錄

1. [專案概述](#專案概述)
2. [系統架構](#系統架構)
3. [技術棧](#技術棧)
4. [核心模組](#核心模組)
5. [API 端點](#api-端點)
6. [資料模型](#資料模型)
7. [Kafka 整合](#kafka-整合)
8. [Instana 監控](#instana-監控)
9. [部署指南](#部署指南)
10. [配置說明](#配置說明)

---

## 專案概述

**Camping API** 是一個基於 Java 17 和 Jakarta EE 10 的露營地預訂系統後端服務,提供完整的 RESTful API、事件驅動架構和全面的可觀測性支援。

### 主要特性

- ✅ **RESTful API**: 完整的露營地管理和訂單處理端點
- ✅ **事件驅動架構**: 基於 Kafka 的非同步訊息處理
- ✅ **微服務整合**: 與外部 Spot Service 整合
- ✅ **全面監控**: Instana SDK 深度整合
- ✅ **React 前端**: 內建於 WAR 的單頁應用
- ✅ **MongoDB 持久化**: 訂單、使用者、優惠券資料儲存
- ✅ **JWT 認證**: 安全的使用者身份驗證

### 專案資訊

- **版本**: 1.0.0-SNAPSHOT
- **Java 版本**: 17
- **Jakarta EE 版本**: 10.0.0
- **部署目標**: JBoss EAP 8
- **打包格式**: WAR

---

## 系統架構

### 整體架構圖

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                        │
│                    Built into WAR at /camping-api               │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP/REST
┌────────────────────────────▼────────────────────────────────────┐
│                      Camping API (JBoss EAP 8)                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              JAX-RS Resources (REST Endpoints)           │   │
│  │  - CheckoutResource  - SpotResource  - AuthResource      │   │
│  │  - OrderResource     - FavoriteResource  - CouponResource│   │
│  └────────────┬─────────────────────────────────┬────────────┘  │
│               │                                 │               │
│  ┌────────────▼─────────────────┐  ┌───────────▼──────────────┐ │
│  │      Business Services       │  │    Repository Layer      │ │
│  │  - AuthService               │  │  - UserRepository        │ │
│  │  - SpotService               │  │  - OrderRepository       │ │
│  │  - PricingService            │  │  - CouponRepository      │ │
│  │  - OrderValidateService      │  │  - FavoriteRepository    │ │
│  │  - KafkaCheckoutService      │  └──────────┬───────────────┘ │
│  │  - AuditService              │             │                 │
│  │  - ReportingService          │             │                 │
│  └────────────┬─────────────────┘             │                 │
│               │                               │                 │
└───────────────┼───────────────────────────────┼─────────────────┘
                │                               │
    ┌───────────▼──────────┐        ┌──────────▼──────────┐
    │   Kafka Cluster      │        │   MongoDB Atlas     │
    │   Topic: raw_events  │        │   Database: camping │
    └───────────┬──────────┘        └─────────────────────┘
                │
    ┌───────────▼──────────────────────────────────────────┐
    │              Kafka Consumer Services                 │
    │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │
    │  │order-        │  │notification- │  │analytics-   │ │
    │  │processor     │  │service       │  │service      │ │
    │  └──────────────┘  └──────────────┘  └─────────────┘ │
    └──────────────────────────────────────────────────────┘
                │
    ┌───────────▼──────────┐
    │  External Services   │
    │  - Spot Service API  │
    │  - Email Service     │
    └──────────────────────┘
```

### 服務層次結構

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                   │
│              JAX-RS Resources (REST API)                │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                     Service Layer                       │
│  - Business Logic                                       │
│  - Transaction Management                               │
│  - External Service Integration                         │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                   Repository Layer                      │
│  - Data Access                                          │
│  - MongoDB Operations                                   │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                   Infrastructure Layer                  │
│  - Kafka Producer/Consumer                              │
│  - MongoDB Driver                                       │
│  - Instana SDK                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 技術棧

### 後端技術

| 技術 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 程式語言 |
| Jakarta EE | 10.0.0 | 企業級框架 |
| JBoss EAP | 8 | 應用伺服器 |
| JAX-RS | 3.1 | REST API |
| CDI | 4.0 | 依賴注入 |
| Maven | 3.x | 建置工具 |

### 資料與訊息

| 技術 | 版本 | 用途 |
|------|------|------|
| MongoDB | 4.11.1 | NoSQL 資料庫 |
| Apache Kafka | 3.7.1 | 訊息佇列 |
| Confluent Avro | 7.7.1 | 訊息序列化 |
| Schema Registry | 7.7.1 | Schema 管理 |

### 監控與可觀測性

| 技術 | 版本 | 用途 |
|------|------|------|
| Instana SDK | 1.2.0 | APM 監控 |
| SLF4J | 2.0.13 | 日誌框架 |

### 前端技術

| 技術 | 用途 |
|------|------|
| React | UI 框架 |
| Vite | 建置工具 |

---

## 核心模組

### 1. Resource Layer (REST 端點)

#### CheckoutResource
**路徑**: [`src/main/java/com/example/camping/resource/CheckoutResource.java`](src/main/java/com/example/camping/resource/CheckoutResource.java)

**職責**:
- 處理訂單結帳請求
- 驗證訂單資料
- 計算價格和優惠
- 儲存訂單到 MongoDB
- 非同步發送事件到 Kafka

**主要方法**:
```java
@POST
@Span(type = Span.Type.ENTRY, value = "camping-api-checkout")
public Map<String, Object> receiveCheckout(@Valid OrderPayload order)
```

**處理流程**:
1. 計算住宿天數
2. 驗證訂單 (OrderValidateService)
3. 取得單價 (PricingService)
4. 驗證並使用優惠券
5. 儲存訂單到 MongoDB
6. 記錄審計日誌
7. 執行報表處理
8. 非同步發送到 Kafka

#### AuthResource
**路徑**: [`src/main/java/com/example/camping/resource/AuthResource.java`](src/main/java/com/example/camping/resource/AuthResource.java)

**職責**:
- 使用者註冊
- 使用者登入
- JWT Token 驗證

**端點**:
- `POST /api/auth/register` - 註冊新使用者
- `POST /api/auth/login` - 使用者登入
- `POST /api/auth/logout` - 使用者登出

#### SpotResource
**路徑**: [`src/main/java/com/example/camping/resource/SpotResource.java`](src/main/java/com/example/camping/resource/SpotResource.java)

**職責**:
- 列出所有露營地
- 取得特定露營地詳情

**端點**:
- `GET /api/spot` - 列出所有露營地
- `GET /api/spot/{spot_id}` - 取得露營地詳情

### 2. Service Layer (業務邏輯)

#### KafkaCheckoutService
**路徑**: [`src/main/java/com/example/camping/service/KafkaCheckoutService.java`](src/main/java/com/example/camping/service/KafkaCheckoutService.java)

**職責**:
- 初始化 Kafka Producer
- 將訂單轉換為 Avro 格式
- 發送訊息到 Kafka Topic
- 傳遞 Instana Trace Context

**關鍵特性**:
- 使用 Confluent Avro Serializer
- 自動註冊 Schema
- 同步發送確保可靠性
- 完整的 Instana 追蹤

#### SpotService
**路徑**: [`src/main/java/com/example/camping/service/SpotService.java`](src/main/java/com/example/camping/service/SpotService.java)

**職責**:
- 呼叫外部 Spot Service API
- 提供 Fallback 資料
- 錯誤分類和處理

**錯誤處理**:
- `connection_refused`: 服務未啟動
- `timeout`: 連線逾時
- `unknown_host`: DNS 解析失敗
- `unauthorized`: 認證失敗

#### AuthService
**路徑**: [`src/main/java/com/example/camping/service/AuthService.java`](src/main/java/com/example/camping/service/AuthService.java)

**職責**:
- 使用者註冊邏輯
- 使用者登入驗證
- JWT Token 生成與驗證
- 密碼雜湊處理

#### PricingService
**路徑**: [`src/main/java/com/example/camping/service/PricingService.java`](src/main/java/com/example/camping/service/PricingService.java)

**職責**:
- 從 Spot Service 取得價格
- 提供預設價格 Fallback

#### OrderValidateService
**路徑**: [`src/main/java/com/example/camping/service/OrderValidateService.java`](src/main/java/com/example/camping/service/OrderValidateService.java)

**職責**:
- 驗證訂單必填欄位
- 驗證日期格式
- 驗證 Email 格式

#### AuditService
**路徑**: [`src/main/java/com/example/camping/service/AuditService.java`](src/main/java/com/example/camping/service/AuditService.java)

**職責**:
- 記錄使用者操作
- 提供審計追蹤

### 3. Repository Layer (資料存取)

#### UserRepository
**職責**:
- 使用者 CRUD 操作
- Email 查詢
- 重複檢查

#### OrderRepository
**職責**:
- 訂單儲存
- 訂單查詢
- 使用者訂單列表

#### CouponRepository
**職責**:
- 優惠券管理
- 優惠券驗證
- 優惠券使用

#### FavoriteRepository
**職責**:
- 收藏管理
- 收藏查詢
- 收藏狀態更新

### 4. Configuration Layer

#### AppConfig
**路徑**: [`src/main/java/com/example/camping/config/AppConfig.java`](src/main/java/com/example/camping/config/AppConfig.java)

**配置項目**:
- Kafka Bootstrap Server
- Schema Registry Endpoint
- KsqlDB Endpoint
- Kafka Topic Name
- Spot Service URL
- MongoDB URI
- JWT Secret

### 5. Observability Layer

#### InstanaTracing
**路徑**: [`src/main/java/com/example/camping/observability/InstanaTracing.java`](src/main/java/com/example/camping/observability/InstanaTracing.java)

**功能**:
- 統一的 Span 命名
- HTTP 請求追蹤
- Kafka 訊息追蹤
- 方法級追蹤
- 錯誤追蹤
- 日誌與 Span 關聯

**Span 類型**:
- `ENTRY`: HTTP 端點、批次作業
- `EXIT`: Kafka 發送、外部 API 呼叫
- `INTERMEDIATE`: 內部方法、業務邏輯

---

## API 端點

### 健康檢查

#### GET /api/health
檢查服務健康狀態

**回應**:
```json
{
  "status": "UP",
  "timestamp": 1234567890
}
```

### 露營地管理

#### GET /api/spot
列出所有露營地

**回應**:
```json
[
  {
    "spotId": "11111111-1111-1111-1111-111111111111",
    "name": "陽明山國家公園",
    "title": "陽明山國家公園",
    "description": "台北近郊的山林營地",
    "price": 1200,
    "imageUrl": "/images/spot/Yangmingshan_National_Park.jpg",
    "thumbnailUrl": "/images/spot/Yangmingshan_National_Park.jpg"
  }
]
```

#### GET /api/spot/{spot_id}
取得特定露營地詳情

**參數**:
- `spot_id`: 露營地 UUID

**回應**: 同上單一物件

### 訂單處理

#### POST /api/checkout
處理訂單結帳

**請求 Body**:
```json
{
  "eventType": "checkout",
  "eventId": "evt_123",
  "sessionId": "sess_456",
  "orderId": "ord_789",
  "userEmail": "user@example.com",
  "userName": "張三",
  "productId": "11111111-1111-1111-1111-111111111111",
  "productName": "陽明山國家公園",
  "checkInDate": "2026-06-01",
  "checkOutDate": "2026-06-03",
  "couponCode": "SUMMER2026",
  "amount": 2400,
  "ts": 1234567890000
}
```

**回應**:
```json
{
  "status": "success",
  "message": "Checkout event accepted",
  "order_id": "ord_789",
  "check_in_date": "2026-06-01",
  "check_out_date": "2026-06-03",
  "nights": 2,
  "unit_price": 1200,
  "total": 2400,
  "discount_amount": 200,
  "final_total": 2200
}
```

### 認證

#### POST /api/auth/register
註冊新使用者

**請求 Body**:
```json
{
  "name": "張三",
  "email": "user@example.com",
  "password": "password123"
}
```

**回應**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "userId": "user_123",
  "name": "張三",
  "email": "user@example.com"
}
```

#### POST /api/auth/login
使用者登入

**請求 Body**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**回應**: 同註冊回應

### 收藏管理

#### POST /api/favorite
新增收藏

**Headers**:
- `Authorization: Bearer {token}`

**請求 Body**:
```json
{
  "spotId": "11111111-1111-1111-1111-111111111111"
}
```

#### GET /api/favorite
取得使用者收藏列表

**Headers**:
- `Authorization: Bearer {token}`

#### DELETE /api/favorite/{spot_id}
取消收藏

**Headers**:
- `Authorization: Bearer {token}`

### 訂單查詢

#### GET /api/order
取得使用者訂單列表

**Headers**:
- `Authorization: Bearer {token}`

---

## 資料模型

### Order (訂單)
```java
{
  "orderId": String,        // 訂單 ID
  "userId": String,         // 使用者 ID
  "userEmail": String,      // 使用者 Email
  "spotId": String,         // 露營地 ID
  "spotName": String,       // 露營地名稱
  "checkInDate": String,    // 入住日期 (YYYY-MM-DD)
  "checkOutDate": String,   // 退房日期 (YYYY-MM-DD)
  "nights": int,            // 住宿天數
  "unitPrice": int,         // 單價
  "total": int,             // 總價
  "discountAmount": int,    // 折扣金額
  "finalTotal": int,        // 最終金額
  "couponCode": String,     // 優惠券代碼
  "status": String,         // 訂單狀態
  "createdAt": long         // 建立時間戳
}
```

### User (使用者)
```java
{
  "userId": String,         // 使用者 ID
  "name": String,           // 姓名
  "email": String,          // Email
  "passwordHash": String,   // 密碼雜湊
  "createdAt": long         // 註冊時間戳
}
```

### Coupon (優惠券)
```java
{
  "couponId": String,       // 優惠券 ID
  "couponCode": String,     // 優惠券代碼
  "userId": String,         // 所屬使用者
  "discountAmount": int,    // 折扣金額
  "status": CouponStatus,   // 狀態 (UNUSED/USED/EXPIRED)
  "usedAt": Long,           // 使用時間
  "usedOrderId": String,    // 使用的訂單 ID
  "expiresAt": long,        // 過期時間
  "createdAt": long         // 建立時間
}
```

### Favorite (收藏)
```java
{
  "favoriteId": String,     // 收藏 ID
  "userId": String,         // 使用者 ID
  "spotId": String,         // 露營地 ID
  "active": boolean,        // 是否啟用
  "createdAt": long,        // 建立時間
  "updatedAt": long         // 更新時間
}
```

### SpotDto (露營地)
```java
{
  "spotId": String,         // 露營地 ID
  "name": String,           // 名稱
  "title": String,          // 標題
  "description": String,    // 描述
  "price": int,             // 價格
  "imageUrl": String,       // 圖片 URL
  "thumbnailUrl": String    // 縮圖 URL
}
```

---

## Kafka 整合

### Topic 架構

#### raw_events Topic
**用途**: 主要事件流
**Schema**: RawEvent (Avro)
**Partition**: 預設
**Replication**: 3

**Schema 定義**:
```json
{
  "type": "record",
  "name": "RawEvent",
  "namespace": "com.travel.events",
  "fields": [
    {"name": "event_type", "type": "string"},
    {"name": "event_id", "type": "string"},
    {"name": "session_id", "type": "string"},
    {"name": "user_email", "type": ["null", "string"]},
    {"name": "user_name", "type": ["null", "string"]},
    {"name": "order_id", "type": ["null", "string"]},
    {"name": "product_id", "type": ["null", "string"]},
    {"name": "product_name", "type": ["null", "string"]},
    {"name": "product_url", "type": ["null", "string"]},
    {"name": "address", "type": ["null", "string"]},
    {"name": "amount", "type": ["null", "int"]},
    {"name": "order_status", "type": ["null", "string"]},
    {"name": "funnel_step", "type": ["null", "string"]},
    {"name": "ts", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "is_real", "type": ["null", "boolean"]}
  ]
}
```

### Producer 配置

**類別**: [`KafkaCheckoutService`](src/main/java/com/example/camping/service/KafkaCheckoutService.java)

**配置**:
```properties
bootstrap.servers=${KAFKA_BOOTSTRAP_SERVER}
key.serializer=StringSerializer
value.serializer=KafkaAvroSerializer
schema.registry.url=${SCHEMA_REGISTRY_ENDPOINT}
auto.register.schemas=false
use.latest.version=true
```

**特性**:
- 同步發送 (`.get()`)
- Instana Trace Header 傳遞
- 錯誤處理與重試

### Consumer Services

#### 1. order-processor
**路徑**: [`consumers/order-processor/`](consumers/order-processor/)

**功能**:
- 消費 checkout 事件
- 執行訂單後處理
- 更新訂單狀態
- 發送到 `orders_processed` topic

**Consumer Group**: `order-processor-group`

#### 2. notification-service
**路徑**: [`consumers/notification-service/`](consumers/notification-service/)

**功能**:
- 消費 checkout 事件
- 發送訂單確認 Email
- 記錄通知歷史

**Consumer Group**: `notification-service-group`

#### 3. analytics-service
**路徑**: `consumers/analytics-service/` (未包含在此專案)

**功能**:
- 消費所有事件
- 即時統計分析
- 漏斗分析

---

## Instana 監控

### 自動追蹤 (Instana Agent)

**配置檔**: [`instana-agent-config.json`](instana-agent-config.json)

**啟用的技術**:
- `jaxrs`: JAX-RS 端點
- `servlet`: Servlet 請求
- `kafka-producer`: Kafka 生產者
- `kafka-consumer`: Kafka 消費者
- `cdi`: CDI Bean 方法
- `ejb`: EJB 方法
- `jms`: JMS 訊息

**追蹤配置**:
```json
{
  "tracing": {
    "enabled": true,
    "stack-trace-length": 50,
    "max-exit-calls": 500,
    "max-entry-calls": 500,
    "detailed-errors": true
  }
}
```

### 手動追蹤 (Instana SDK)

**SDK 配置**: [`instana-sdk.properties`](src/main/resources/instana-sdk.properties)
```properties
instana.sdk.enabled=true
```

**追蹤工具類**: [`InstanaTracing`](src/main/java/com/example/camping/observability/InstanaTracing.java)

**Span 類型**:

1. **ENTRY Spans** (入口點)
   - HTTP 端點: `camping-api-checkout`, `camping-api-list-spots`
   - 批次作業: `camping-checkout-async-job`

2. **EXIT Spans** (外部呼叫)
   - Kafka 發送: `camping-kafka-checkout-send`
   - HTTP 呼叫: `camping-http-spot-service-exit`

3. **INTERMEDIATE Spans** (內部邏輯)
   - 業務邏輯: `camping-order-validate`, `camping-pricing-calculate`
   - 資料存取: `camping-user-repo-save`, `camping-order-repo-save`
   - 工具方法: `camping-kafka-record-build`

**標籤 (Tags)**:
- `tags.http.method`: HTTP 方法
- `tags.http.url`: 請求 URL
- `tags.http.status_code`: 狀態碼
- `tags.kafka.topic`: Kafka Topic
- `tags.kafka.key`: 訊息 Key
- `tags.event.type`: 事件類型
- `tags.checkout.order_id`: 訂單 ID
- `tags.checkout.total`: 訂單金額
- `tags.error`: 錯誤標記
- `tags.error.message`: 錯誤訊息

**日誌整合**:
```java
InstanaTracing.logWarn(LOGGER, "[CHECKOUT] received - order_id: " + orderId);
```
- 自動關聯日誌到 Span
- 支援 INFO, WARN, ERROR 級別
- 包含錯誤堆疊追蹤

### Trace Context 傳遞

**Kafka Producer**:
```java
Map<String, String> traceHeaders = new HashMap<>();
SpanSupport.addTraceHeadersIfTracing(Span.Type.EXIT, traceHeaders);
traceHeaders.forEach((key, value) -> 
    record.headers().add(key, value.getBytes(StandardCharsets.UTF_8))
);
```

**HTTP Client**:
```java
Map<String, String> instanaHeaders = new HashMap<>();
SpanSupport.addTraceHeadersIfTracing(instanaHeaders);
MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
instanaHeaders.forEach((k, v) -> headers.add(k, v));
```

**非同步執行**:
```java
Object snapshotKey = ContextSupport.takeSnapshot();
executorService.submit(() -> {
    ContextSupport.restoreSnapshot(snapshotKey);
    // 執行非同步任務
});
```

---

## 部署指南

### 前置需求

1. **JDK 17**
   ```bash
   java -version
   # 應顯示 Java 17
   ```

2. **Maven 3.x**
   ```bash
   mvn -version
   ```

3. **JBoss EAP 8**
   - 下載並安裝 JBoss EAP 8
   - 設定 `JBOSS_HOME` 環境變數

4. **Kafka Cluster**
   - Kafka 3.7.1+
   - Schema Registry 7.7.1+
   - Topic: `raw_events` (已建立)

5. **MongoDB**
   - MongoDB 4.x+
   - Database: `camping`
   - Collections: `users`, `orders`, `coupons`, `favorites`

6. **Instana Agent**
   - 已安裝並運行
   - 配置檔: `instana-agent-config.json`

### 建置步驟

#### 1. 建置後端 (不含前端)
```bash
mvn clean package -DskipFrontend=true
```

#### 2. 建置完整應用 (含前端)
```bash
# 需要先建置 React 前端
cd ../camping-react
npm ci
npm run build

# 回到後端專案
cd ../Instana-SDK-Lab-clone
mvn clean package -Pwith-frontend
```

#### 3. 使用建置腳本
**Windows**:
```powershell
.\build.bat
```

**Linux/Mac**:
```bash
./build.sh
```

### 部署到 JBoss EAP 8

#### 方法 1: 複製 WAR 檔
```bash
cp target/camping-api.war $JBOSS_HOME/standalone/deployments/
```

#### 方法 2: 使用 JBoss CLI
```bash
$JBOSS_HOME/bin/jboss-cli.sh --connect
deploy target/camping-api.war
```

#### 方法 3: 管理控制台
1. 開啟 http://localhost:9990
2. 進入 Deployments
3. 上傳 `camping-api.war`

### 驗證部署

1. **檢查部署狀態**
   ```bash
   curl http://localhost:8080/camping-api/api/health
   ```
   
   預期回應:
   ```json
   {
     "status": "UP",
     "timestamp": 1234567890
   }
   ```

2. **檢查前端**
   - 開啟瀏覽器: http://localhost:8080/camping-api/

3. **檢查 Instana**
   - 登入 Instana UI
   - 查看 Applications > camping-api
   - 確認有追蹤資料

### Consumer Services 部署

#### order-processor
```bash
cd consumers/order-processor
mvn clean package
java -jar target/order-processor-1.0.0.jar
```

#### notification-service
```bash
cd consumers/notification-service
mvn clean package
java -jar target/notification-service-1.0.0.jar
```

---

## 配置說明

### 環境變數

| 變數名稱 | 預設值 | 說明 |
|---------|--------|------|
| `KAFKA_BOOTSTRAP_SERVER` | `10.107.85.239:9092` | Kafka 伺服器位址 |
| `SCHEMA_REGISTRY_ENDPOINT` | `http://10.107.85.239:8081` | Schema Registry URL |
| `KSQLDB_ENDPOINT` | `http://10.107.85.239:8088` | KsqlDB URL |
| `KAFKA_TOPIC_NAME` | `raw_events` | Kafka Topic 名稱 |
| `SPOT_SERVICE_URL` | `http://10.107.85.67:8080/spot-service/api` | Spot Service API URL |
| `MONGODB_URI` | (見 AppConfig) | MongoDB 連線字串 |
| `MONGODB_DATABASE` | `camping` | MongoDB 資料庫名稱 |
| `JWT_SECRET` | (預設值) | JWT 簽章密鑰 |

### JBoss EAP 配置

#### 設定環境變數
**standalone.conf** (Linux/Mac):
```bash
JAVA_OPTS="$JAVA_OPTS -DKAFKA_BOOTSTRAP_SERVER=10.107.85.239:9092"
JAVA_OPTS="$JAVA_OPTS -DSCHEMA_REGISTRY_ENDPOINT=http://10.107.85.239:8081"
JAVA_OPTS="$JAVA_OPTS -DMONGODB_URI=mongodb://..."
```

**standalone.conf.bat** (Windows):
```batch
set "JAVA_OPTS=%JAVA_OPTS% -DKAFKA_BOOTSTRAP_SERVER=10.107.85.239:9092"
set "JAVA_OPTS=%JAVA_OPTS% -DSCHEMA_REGISTRY_ENDPOINT=http://10.107.85.239:8081"
```

#### 設定 Context Root
**jboss-web.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jboss-web>
    <context-root>/camping-api</context-root>
</jboss-web>
```

### MongoDB 配置

#### 建立資料庫和集合
```javascript
use camping

db.createCollection("users")
db.createCollection("orders")
db.createCollection("coupons")
db.createCollection("favorites")

// 建立索引
db.users.createIndex({ "email": 1 }, { unique: true })
db.orders.createIndex({ "userId": 1 })
db.orders.createIndex({ "orderId": 1 }, { unique: true })
db.coupons.createIndex({ "couponCode": 1 }, { unique: true })
db.coupons.createIndex({ "userId": 1 })
db.favorites.createIndex({ "userId": 1, "spotId": 1 })
```

### Kafka 配置

#### 建立 Topic
```bash
kafka-topics --create \
  --bootstrap-server 10.107.85.239:9092 \
  --topic raw_events \
  --partitions 3 \
  --replication-factor 3
```

#### 註冊 Schema
```bash
curl -X POST http://10.107.85.239:8081/subjects/raw_events-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d @raw_event_schema.json
```

### Instana 配置

#### Agent 配置
**instana-agent-config.json**:
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
      },
      "enabled-technologies": [
        "jaxrs",
        "servlet",
        "kafka-producer",
        "kafka-consumer",
        "cdi",
        "ejb",
        "jms"
      ]
    },
    "tracing": {
      "enabled": true,
      "stack-trace-length": 50,
      "max-exit-calls": 500,
      "max-entry-calls": 500,
      "detailed-errors": true
    }
  }
}
```

#### SDK 配置
**instana-sdk.properties**:
```properties
instana.sdk.enabled=true
```

---

## 開發指南

### 本地開發環境設定

1. **安裝依賴**
   ```bash
   mvn clean install
   ```

2. **啟動 JBoss EAP**
   ```bash
   $JBOSS_HOME/bin/standalone.sh
   ```

3. **部署應用**
   ```bash
   mvn clean package -DskipFrontend=true
   cp target/camping-api.war $JBOSS_HOME/standalone/deployments/
   ```

4. **啟動 Consumer Services**
   ```bash
   # Terminal 1
   cd consumers/order-processor
   mvn exec:java

   # Terminal 2
   cd consumers/notification-service
   mvn exec:java
   ```

### 測試

#### 單元測試
```bash
mvn test
```

#### 整合測試
```bash
mvn verify
```

#### API 測試
使用提供的 PowerShell 腳本:
```powershell
.\test-checkout.ps1
.\test-auth.ps1
.\test-spot-service-comparison.ps1
```

### 除錯

#### 啟用 JBoss 除錯模式
```bash
$JBOSS_HOME/bin/standalone.sh --debug
```

#### 連接除錯器
- Host: localhost
- Port: 8787

#### 查看日誌
```bash
tail -f $JBOSS_HOME/standalone/log/server.log
```

---

## 故障排除

### 常見問題

#### 1. Kafka 連線失敗
**錯誤**: `Connection refused`

**解決方案**:
- 檢查 Kafka 是否運行
- 驗證 `KAFKA_BOOTSTRAP_SERVER` 設定
- 檢查網路連線和防火牆

#### 2. MongoDB 連線失敗
**錯誤**: `MongoTimeoutException`

**解決方案**:
- 檢查 MongoDB 是否運行
- 驗證 `MONGODB_URI` 設定
- 檢查網路連線
- 確認認證資訊正確

#### 3. Spot Service 呼叫失敗
**錯誤**: `Connection refused` 或 `Timeout`

**解決方案**:
- 檢查 Spot Service 是否運行
- 驗證 `SPOT_SERVICE_URL` 設定
- 系統會自動使用 Fallback 資料

#### 4. Instana 追蹤缺失
**問題**: 看不到追蹤資料

**解決方案**:
- 檢查 Instana Agent 是否運行
- 驗證 `instana-agent-config.json` 配置
- 確認 `instana-sdk.properties` 啟用
- 檢查應用程式日誌

#### 5. JWT Token 驗證失敗
**錯誤**: `Unauthorized`

**解決方案**:
- 確認 Token 格式正確
- 檢查 Token 是否過期
- 驗證 `JWT_SECRET` 設定一致

---

## 效能優化

### 建議

1. **Kafka Producer 池化**
   - 使用單例 Producer
   - 避免頻繁建立/關閉

2. **MongoDB 連線池**
   - 設定適當的連線池大小
   - 使用連線池監控

3. **HTTP Client 重用**
   - 使用連線池
   - 設定適當的 timeout

4. **快取策略**
   - 快取 Spot Service 回應
   - 使用 CDI `@ApplicationScoped`

5. **非同步處理**
   - Kafka 發送使用非同步
   - 報表處理使用背景執行緒

---

## 安全性

### 實作的安全措施

1. **JWT 認證**
   - 所有需要認證的端點使用 JWT
   - Token 包含使用者資訊

2. **密碼雜湊**
   - 使用 BCrypt 雜湊密碼
   - 不儲存明文密碼

3. **CORS 設定**
   - 配置允許的來源
   - 限制 HTTP 方法

4. **輸入驗證**
   - 使用 Bean Validation
   - 驗證所有使用者輸入

5. **錯誤處理**
   - 不洩漏敏感資訊
   - 統一的錯誤回應格式

### 安全建議

1. **生產環境**
   - 更改預設 JWT Secret
   - 使用 HTTPS
   - 啟用 MongoDB 認證
   - 限制 Kafka 存取

2. **監控**
   - 監控異常登入嘗試
   - 追蹤 API 使用率
   - 設定告警規則

---

## 附錄

### A. 專案結構

```
Instana-SDK-Lab-clone/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/camping/
│   │   │       ├── CampingApplication.java
│   │   │       ├── config/
│   │   │       ├── dto/
│   │   │       ├── filter/
│   │   │       ├── model/
│   │   │       ├── observability/
│   │   │       ├── repository/
│   │   │       ├── resource/
│   │   │       ├── service/
│   │   │       └── util/
│   │   ├── resources/
│   │   │   └── instana-sdk.properties
│   │   └── webapp/
│   │       ├── index.html
│   │       ├── assets/
│   │       └── images/
│   └── test/
├── consumers/
│   ├── order-processor/
│   ├── notification-service/
│   └── README.md
├── pom.xml
├── build.bat
├── build.sh
├── instana-agent-config.json
└── README.md
```

### B. 相關文件

- [README.md](README.md) - 專案說明
- [INSTANA_SDK_DEPLOYMENT_GUIDE.md](INSTANA_SDK_DEPLOYMENT_GUIDE.md) - Instana 部署指南
- [SDK_TOGGLE_DESIGN.md](SDK_TOGGLE_DESIGN.md) - SDK 切換設計
- [TESTING.md](TESTING.md) - 測試指南
- [consumers/README.md](consumers/README.md) - Consumer 服務說明

### C. 版本歷史

- **1.0.0-SNAPSHOT** (2026-05)
  - 初始版本
  - 完整的 REST API
  - Kafka 整合
  - Instana 監控
  - MongoDB 持久化
  - JWT 認證

---

## 聯絡資訊

如有問題或建議,請聯絡開發團隊。

---

**文件版本**: 1.0.0  
**最後更新**: 2026-05-26  
**維護者**: Camping API 開發團隊