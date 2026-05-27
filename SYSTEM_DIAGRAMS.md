# Camping API 系統架構圖與流程圖

本文件使用 ASCII 藝術圖,可在任何文字編輯器中正確顯示。

---

## 1. 整體系統架構圖

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                         │
│                    Built into WAR at /camping-api                │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP/REST
┌────────────────────────────▼────────────────────────────────────┐
│                      Camping API (JBoss EAP 8)                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              JAX-RS Resources (REST Endpoints)            │   │
│  │  - CheckoutResource  - SpotResource  - AuthResource      │   │
│  │  - OrderResource     - FavoriteResource  - CouponResource│   │
│  └────────────┬─────────────────────────────────┬────────────┘   │
│               │                                 │                │
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
    │              Kafka Consumer Services                  │
    │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐│
    │  │order-        │  │notification- │  │analytics-   ││
    │  │processor     │  │service       │  │service      ││
    │  └──────────────┘  └──────────────┘  └─────────────┘│
    └───────────────────────────────────────────────────────┘
                │
    ┌───────────▼──────────┐
    │  External Services   │
    │  - Spot Service API  │
    │  - Email Service     │
    └──────────────────────┘
```

---

## 2. 服務層次結構

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│              JAX-RS Resources (REST API)                 │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                     Service Layer                        │
│  - Business Logic                                        │
│  - Transaction Management                                │
│  - External Service Integration                          │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                   Repository Layer                       │
│  - Data Access                                           │
│  - MongoDB Operations                                    │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                   Infrastructure Layer                   │
│  - Kafka Producer/Consumer                               │
│  - MongoDB Driver                                        │
│  - Instana SDK                                           │
└──────────────────────────────────────────────────────────┘
```

---

## 3. 訂單結帳完整流程

```
使用者                                                     系統處理流程
  │
  │ 1. 點擊結帳
  ├──────────────────────────────────────────────────────────────────┐
  │                                                                   │
  │                        POST /api/checkout                         │
  │                                                                   │
  │                    ┌──────────────────────┐                      │
  │                    │  CheckoutResource    │                      │
  │                    │  (Instana ENTRY)     │                      │
  │                    └──────────┬───────────┘                      │
  │                               │                                  │
  │                    ┌──────────▼───────────┐                      │
  │                    │  計算住宿天數         │                      │
  │                    │  nights = 退房-入住   │                      │
  │                    └──────────┬───────────┘                      │
  │                               │                                  │
  │                    ┌──────────▼───────────┐                      │
  │                    │ OrderValidateService │                      │
  │                    │ - 驗證必填欄位        │                      │
  │                    │ - 驗證日期格式        │                      │
  │                    │ - 驗證 Email         │                      │
  │                    └──────────┬───────────┘                      │
  │                               │                                  │
  │                    ┌──────────▼───────────┐                      │
  │                    │   PricingService     │                      │
  │                    │   ↓                  │                      │
  │                    │   SpotService        │                      │
  │                    │   ↓                  │                      │
  │                    │   HTTP GET           │                      │
  │                    │   Spot Service API   │                      │
  │                    └──────────┬───────────┘                      │
  │                               │                                  │
  │                    ┌──────────▼───────────┐                      │
  │                    │  計算總價             │                      │
  │                    │  total = price*nights│                      │
  │                    └──────────┬───────────┘                      │
  │                               │                                  │
  │                    ┌──────────▼───────────┐                      │
  │                    │  優惠券驗證 (如有)    │                      │
  │                    │  - 查詢優惠券         │                      │
  │                    │  - 檢查狀態          │                      │
  │                    │  - 檢查期限          │                      │
  │                    │  - 標記為已使用       │                      │
  │                    │  - 計算折扣後金額     │                      │
  │                    └──────────┬───────────┘                      │
  │                               │                                  │
  │                    ┌──────────▼───────────┐                      │
  │                    │  儲存訂單到 MongoDB  │                      │
  │                    │  OrderRepository     │                      │
  │                    └──────────┬───────────┘                      │
  │                               │                                  │
  │                    ┌──────────▼───────────┐                      │
  │                    │  記錄審計日誌         │                      │
  │                    │  AuditService        │                      │
  │                    └──────────┬───────────┘                      │
  │                               │                                  │
  │                    ┌──────────▼───────────┐                      │
  │                    │  報表處理             │                      │
  │                    │  ReportingService    │                      │
  │                    └──────────┬───────────┘                      │
  │                               │                                  │
  │                    ┌──────────▼───────────┐                      │
  │                    │  非同步 Kafka 發送    │                      │
  │                    │  (背景執行緒)         │                      │
  │                    │  ↓                   │                      │
  │                    │  KafkaCheckoutService│                      │
  │                    │  ↓                   │                      │
  │                    │  Kafka Cluster       │                      │
  │                    └──────────┬───────────┘                      │
  │                               │                                  │
  │ ◄─────────────────────────────┘                                  │
  │ 2. 返回訂單確認                                                   │
  │    - order_id                                                    │
  │    - nights                                                      │
  │    - total                                                       │
  │    - discount_amount                                             │
  │    - final_total                                                 │
  │                                                                   │
  └───────────────────────────────────────────────────────────────────┘
```

---

## 4. 訂單狀態機

```
                    ┌─────────────┐
                    │   Created   │ ◄─── 使用者提交訂單
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ Validating  │
                    └──────┬──────┘
                           │
                ┌──────────┼──────────┐
                │                     │
         ┌──────▼──────┐       ┌─────▼─────┐
         │   Failed    │       │   Price   │
         │             │       │Calculating│
         └─────────────┘       └─────┬─────┘
                                     │
                              ┌──────▼──────┐
                              │   Coupon    │
                              │ Validating  │
                              └──────┬──────┘
                                     │
                          ┌──────────┼──────────┐
                          │                     │
                   ┌──────▼──────┐       ┌─────▼─────┐
                   │   Failed    │       │   Saving  │
                   │             │       │           │
                   └─────────────┘       └─────┬─────┘
                                               │
                                        ┌──────▼──────┐
                                        │  Confirmed  │
                                        └──────┬──────┘
                                               │
                                        ┌──────▼──────┐
                                        │   Kafka     │
                                        │  Sending    │
                                        └──────┬──────┘
                                               │
                                    ┌──────────┼──────────┐
                                    │                     │
                             ┌──────▼──────┐      ┌──────▼──────┐
                             │  Completed  │      │  Partial    │
                             │             │      │  Success    │
                             └─────────────┘      └─────────────┘
```

---

## 5. 使用者認證流程

### 5.1 註冊流程

```
使用者          Frontend        AuthResource      AuthService      UserRepository    MongoDB
  │                │                 │                 │                 │              │
  │ 填寫註冊表單    │                 │                 │                 │              │
  ├───────────────►│                 │                 │                 │              │
  │                │ POST /register  │                 │                 │              │
  │                ├────────────────►│                 │                 │              │
  │                │                 │ register()      │                 │              │
  │                │                 ├────────────────►│                 │              │
  │                │                 │                 │ existsByEmail() │              │
  │                │                 │                 ├────────────────►│              │
  │                │                 │                 │                 │ findOne()    │
  │                │                 │                 │                 ├─────────────►│
  │                │                 │                 │                 │◄─────────────┤
  │                │                 │                 │◄────────────────┤              │
  │                │                 │                 │                 │              │
  │                │                 │                 │ [Email 已存在]  │              │
  │                │                 │                 ├─────────────────┐              │
  │                │                 │◄────────────────┤ BadRequest      │              │
  │                │◄────────────────┤                 │                 │              │
  │◄───────────────┤ 400 錯誤        │                 │                 │              │
  │                │                 │                 │                 │              │
  │                │                 │                 │ [Email 可用]    │              │
  │                │                 │                 │ hash password   │              │
  │                │                 │                 │ save(user)      │              │
  │                │                 │                 ├────────────────►│              │
  │                │                 │                 │                 │ insertOne()  │
  │                │                 │                 │                 ├─────────────►│
  │                │                 │                 │                 │◄─────────────┤
  │                │                 │                 │◄────────────────┤              │
  │                │                 │                 │ generate JWT    │              │
  │                │                 │◄────────────────┤                 │              │
  │                │◄────────────────┤ 200 + token     │                 │              │
  │◄───────────────┤                 │                 │                 │              │
  │ 註冊成功        │                 │                 │                 │              │
```

### 5.2 登入流程

```
使用者          Frontend        AuthResource      AuthService      UserRepository    MongoDB
  │                │                 │                 │                 │              │
  │ 輸入帳號密碼    │                 │                 │                 │              │
  ├───────────────►│                 │                 │                 │              │
  │                │ POST /login     │                 │                 │              │
  │                ├────────────────►│                 │                 │              │
  │                │                 │ login()         │                 │              │
  │                │                 ├────────────────►│                 │              │
  │                │                 │                 │ findByEmail()   │              │
  │                │                 │                 ├────────────────►│              │
  │                │                 │                 │                 │ findOne()    │
  │                │                 │                 │                 ├─────────────►│
  │                │                 │                 │                 │◄─────────────┤
  │                │                 │                 │◄────────────────┤              │
  │                │                 │                 │                 │              │
  │                │                 │                 │ verify password │              │
  │                │                 │                 │                 │              │
  │                │                 │                 │ [密碼正確]      │              │
  │                │                 │                 │ generate JWT    │              │
  │                │                 │◄────────────────┤                 │              │
  │                │◄────────────────┤ 200 + token     │                 │              │
  │◄───────────────┤                 │                 │                 │              │
  │ 登入成功        │                 │                 │                 │              │
```

---

## 6. Kafka 訊息流程

### 6.1 Producer 發送流程

```
CheckoutResource
      │
      │ send(order)
      ▼
KafkaCheckoutService
      │
      ├─► getProducer()
      │   │
      │   ├─► [首次呼叫] 建立 KafkaProducer
      │   │   - 設定 bootstrap.servers
      │   │   - 設定 serializers
      │   │   - 連接 Schema Registry
      │   │
      │   └─► [後續呼叫] 返回已存在的 Producer
      │
      ├─► toGenericRecord(order)
      │   │
      │   └─► 建立 Avro GenericRecord
      │       - 填充所有欄位
      │       - 遵循 RawEvent Schema
      │
      ├─► addInstanaTraceHeaders(record)
      │   │
      │   └─► 從 Instana SDK 取得 Trace Headers
      │       - X-Instana-T (Trace ID)
      │       - X-Instana-S (Span ID)
      │       - X-Instana-L (Level)
      │       - 加入到 Kafka Headers
      │
      └─► producer.send(record).get()
          │
          ├─► Schema Registry 驗證
          │
          ├─► Avro 序列化
          │
          ├─► 發送到 Kafka Broker
          │
          └─► 等待 ACK (同步發送)
```

### 6.2 Consumer 消費流程

```
OrderConsumer (啟動)
      │
      ├─► subscribe("raw_events")
      │
      └─► 持續輪詢 ───┐
                     │
          ┌──────────┘
          │
          ▼
    poll(Duration)
          │
          ├─► [有訊息]
          │   │
          │   └─► 處理每筆記錄 ───┐
          │                      │
          │       ┌──────────────┘
          │       │
          │       ├─► 提取 Instana Trace Headers
          │       │   - 恢復 Trace Context
          │       │   - 繼承 Parent Span
          │       │
          │       ├─► 解析 Avro Record
          │       │
          │       ├─► OrderProcessor.process()
          │       │   │
          │       │   ├─► 驗證訂單資料
          │       │   │
          │       │   ├─► 更新訂單狀態
          │       │   │
          │       │   ├─► 發送通知
          │       │   │
          │       │   └─► 記錄處理日誌
          │       │
          │       └─► commitSync()
          │
          └─► [無訊息] 繼續輪詢
```

---

## 7. Instana 追蹤架構

### 7.1 Span 層次結構

```
HTTP Request (ENTRY)
└─ camping-api-checkout
   │
   ├─ OrderValidateService (INTERMEDIATE)
   │  └─ camping-order-validate
   │
   ├─ PricingService (INTERMEDIATE)
   │  └─ camping-pricing-calculate
   │     │
   │     └─ SpotService (INTERMEDIATE)
   │        └─ camping-spot-lookup
   │           │
   │           └─ HTTP Client (EXIT)
   │              └─ camping-http-spot-service-exit
   │
   ├─ CouponRepository (INTERMEDIATE)
   │  └─ camping-coupon-repo-findByCouponCode
   │
   ├─ OrderRepository (INTERMEDIATE)
   │  └─ camping-order-repo-save
   │
   ├─ AuditService (INTERMEDIATE)
   │  └─ camping-audit-record
   │
   ├─ ReportingService (INTERMEDIATE)
   │  └─ camping-reporting-*
   │
   └─ Async Job (ENTRY)
      └─ camping-checkout-async-job
         │
         └─ KafkaCheckoutService (EXIT)
            └─ camping-kafka-checkout-send
               │
               ├─ Producer Init (INTERMEDIATE)
               │  └─ camping-kafka-producer-init
               │
               ├─ Record Build (INTERMEDIATE)
               │  └─ camping-kafka-record-build
               │
               └─ Add Headers (INTERMEDIATE)
                  └─ camping-kafka-trace-headers
```

### 7.2 追蹤標籤結構

```
Span: camping-api-checkout
├─ tags.http.method = "POST"
├─ tags.http.url = "service://camping-api/api/checkout"
├─ tags.http.status_code = "200"
├─ tags.service = "camping-api"
├─ tags.endpoint = "POST /api/checkout"
├─ tags.checkout.order_id = "ord_123"
├─ tags.checkout.event_id = "evt_456"
├─ tags.checkout.total = "2400"
├─ tags.checkout.discount_amount = "200"
├─ tags.checkout.final_total = "2200"
├─ tags.checkout.coupon_code = "SUMMER2026"
├─ tags.checkout.coupon_applied = "true"
├─ log.0.level = "WARN"
├─ log.0.msg = "[CHECKOUT] received - order_id: ord_123"
├─ log.1.level = "WARN"
└─ log.1.msg = "[CHECKOUT] accepted - final_total: 2200"
```

---

## 8. 錯誤處理流程

### 8.1 全域錯誤處理

```
Exception 發生
      │
      ├─► BadRequestException
      │   └─► BadRequestMapper
      │       └─► 400 Bad Request
      │
      ├─► NotFoundException
      │   └─► NotFoundMapper
      │       └─► 404 Not Found
      │
      ├─► NotAuthorizedException
      │   └─► UnauthorizedMapper
      │       └─► 401 Unauthorized
      │
      ├─► ConstraintViolationException
      │   └─► ConstraintViolationMapper
      │       └─► 400 Bad Request (with validation errors)
      │
      └─► 其他 Exception
          └─► GlobalExceptionMapper
              └─► 500 Internal Server Error
                  │
                  └─► 記錄到 Instana
                      - tags.error = "true"
                      - tags.error.message
                      - tags.error.type
                      │
                      └─► 返回 JSON 錯誤回應
                          {
                            "error": "...",
                            "message": "...",
                            "timestamp": ...
                          }
```

### 8.2 Spot Service 錯誤處理

```
呼叫 Spot Service API
      │
      ├─► [成功] 返回 SpotDto
      │
      └─► [失敗] 捕獲 Exception
          │
          ├─► ConnectException
          │   └─► 錯誤類型: connection_refused
          │       提示: "服務未啟動，請確認服務是否運行中"
          │
          ├─► SocketTimeoutException
          │   └─► 錯誤類型: timeout
          │       提示: "連線逾時，服務可能過載或未回應"
          │
          ├─► UnknownHostException
          │   └─► 錯誤類型: unknown_host
          │       提示: "無法解析主機名稱，請檢查 URL 設定"
          │
          ├─► 401 Error
          │   └─► 錯誤類型: unauthorized
          │       提示: "認證失敗，請確認 API Key"
          │
          └─► 其他
              └─► 錯誤類型: unknown
                  提示: "呼叫失敗，請查看日誌"
                  │
                  └─► 記錄到 Instana
                      - tags.error = "true"
                      - tags.spot.error_type
                      - tags.spot.error_hint
                      - tags.spot.target_url
                      │
                      └─► 返回 Fallback 資料
                          - 使用預設露營地列表
                          - tags.spot.source = "fallback"
```

---

## 9. 部署架構圖

```
                        ┌─────────────────┐
                        │  Load Balancer  │
                        │  (Nginx/HAProxy)│
                        └────────┬────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
        ┌───────▼───────┐ ┌─────▼──────┐ ┌──────▼───────┐
        │  JBoss EAP 8  │ │ JBoss EAP 8│ │ JBoss EAP 8  │
        │  Instance 1   │ │ Instance 2 │ │ Instance 3   │
        │  camping-api  │ │ camping-api│ │ camping-api  │
        └───────┬───────┘ └─────┬──────┘ └──────┬───────┘
                │               │               │
                └───────┬───────┴───────┬───────┘
                        │               │
        ┌───────────────▼───┐   ┌───────▼───────────────┐
        │   Kafka Cluster   │   │   MongoDB Cluster     │
        │  ┌──────────────┐ │   │  ┌──────────────────┐ │
        │  │  Broker 1    │ │   │  │  Primary         │ │
        │  │  Broker 2    │ │   │  │  Secondary 1     │ │
        │  │  Broker 3    │ │   │  │  Secondary 2     │ │
        │  └──────────────┘ │   │  └──────────────────┘ │
        │  ┌──────────────┐ │   └───────────────────────┘
        │  │Schema Registry│ │
        │  └──────────────┘ │
        └───────┬───────────┘
                │
        ┌───────┴───────────────────────────┐
        │                                   │
┌───────▼──────────┐  ┌──────────────────┐ │
│ order-processor  │  │ notification-    │ │
│                  │  │ service          │ │
└──────────────────┘  └──────────────────┘ │
                                           │
        ┌──────────────────────────────────┘
        │
┌───────▼──────────┐
│ analytics-       │
│ service          │
└──────────────────┘

        ┌──────────────────────────────────┐
        │      Instana Monitoring          │
        │  ┌────────────────────────────┐  │
        │  │  Instana Agent (on each)   │  │
        │  │  - JBoss Instances         │  │
        │  │  - Consumer Services       │  │
        │  └────────────┬───────────────┘  │
        │               │                  │
        │  ┌────────────▼───────────────┐  │
        │  │  Instana Backend           │  │
        │  │  - Trace Collection        │  │
        │  │  - Metrics Aggregation     │  │
        │  │  - Alerting                │  │
        │  └────────────────────────────┘  │
        └──────────────────────────────────┘
```

---

## 10. 資料流程圖

```
┌──────────┐
│  使用者   │
└─────┬────┘
      │
      │ HTTP Request
      ▼
┌─────────────────┐
│ React Frontend  │
└─────┬───────────┘
      │
      │ REST API Call
      ▼
┌─────────────────┐
│  JAX-RS API     │
│  (Resources)    │
└─────┬───────────┘
      │
      │ Business Logic
      ▼
┌─────────────────┐
│ Service Layer   │
│ - AuthService   │
│ - SpotService   │
│ - OrderService  │
└─────┬───────────┘
      │
      ├──────────────────────┬──────────────────────┐
      │                      │                      │
      ▼                      ▼                      ▼
┌─────────────┐    ┌─────────────────┐    ┌──────────────┐
│ Repository  │    │ Kafka Producer  │    │ External API │
│   Layer     │    │                 │    │              │
└─────┬───────┘    └─────┬───────────┘    └──────┬───────┘
      │                  │                       │
      ▼                  ▼                       ▼
┌─────────────┐    ┌─────────────────┐    ┌──────────────┐
│  MongoDB    │    │ Kafka Cluster   │    │ Spot Service │
│  Atlas      │    │                 │    │              │
└─────────────┘    └─────┬───────────┘    └──────────────┘
                         │
                         ▼
                   ┌─────────────────┐
                   │ Kafka Consumer  │
                   │   Services      │
                   └─────┬───────────┘
                         │
                         ├──────────────┬──────────────┐
                         ▼              ▼              ▼
                   ┌──────────┐  ┌──────────┐  ┌──────────┐
                   │  Order   │  │  Email   │  │Analytics │
                   │Processor │  │ Service  │  │ Service  │
                   └──────────┘  └──────────┘  └──────────┘

                   ┌─────────────────────────────────────┐
                   │      Instana Monitoring             │
                   │  (追蹤所有層級的呼叫和效能)          │
                   └─────────────────────────────────────┘
```

---

## 11. 技術棧架構

```
┌─────────────────────────────────────────────────────────────┐
│                        前端技術                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │ React 18 │  │   Vite   │  │TypeScript│                  │
│  └──────────┘  └──────────┘  └──────────┘                  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                        後端技術                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Java 17  │  │Jakarta EE│  │ JAX-RS   │  │   CDI    │   │
│  └──────────┘  │    10    │  │   3.1    │  │   4.0    │   │
│                └──────────┘  └──────────┘  └──────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              JBoss EAP 8 Application Server          │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      資料與訊息技術                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ MongoDB  │  │  Kafka   │  │   Avro   │  │ Schema   │   │
│  │  4.11.1  │  │  3.7.1   │  │          │  │ Registry │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      監控與可觀測性                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │ Instana  │  │ Instana  │  │  SLF4J   │                  │
│  │   SDK    │  │  Agent   │  │  2.0.13  │                  │
│  │  1.2.0   │  │          │  │          │                  │
│  └──────────┘  └──────────┘  └──────────┘                  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                        建置工具                              │
│  ┌──────────┐  ┌──────────┐                                 │
│  │ Maven 3.x│  │ npm/pnpm │                                 │
│  └──────────┘  └──────────┘                                 │
└─────────────────────────────────────────────────────────────┘
```

---

**文件版本**: 1.0.0  
**最後更新**: 2026-05-26  
**維護者**: Camping API 開發團隊

**注意**: 本文件使用 ASCII 藝術圖,可在任何文字編輯器中正確顯示。