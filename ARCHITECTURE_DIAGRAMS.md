# Camping API 系統架構概覽

本文件使用文字描述和表格來說明系統架構,確保在任何環境下都能正確閱讀。

---

## 1. 系統整體架構

### 架構層次

| 層次 | 組件 | 說明 |
|------|------|------|
| **前端層** | React SPA | 使用 Vite 建置,打包進 WAR 檔 |
| **API 層** | JAX-RS Resources | REST 端點,運行在 JBoss EAP 8 |
| **服務層** | Business Services | 業務邏輯處理 |
| **資料層** | Repository Layer | MongoDB 資料存取 |
| **訊息層** | Kafka Integration | 事件驅動架構 |
| **監控層** | Instana SDK/Agent | 全面可觀測性 |

### 主要組件關係

**前端 → API**
- React Frontend 透過 HTTP/REST 呼叫 JAX-RS Resources

**API → 服務層**
- CheckoutResource → KafkaCheckoutService, PricingService, OrderValidateService
- SpotResource → SpotService
- AuthResource → AuthService
- OrderResource → OrderRepository
- FavoriteResource → FavoriteRepository

**服務層 → 外部系統**
- KafkaCheckoutService → Kafka Cluster (raw_events topic)
- SpotService → Spot Service API (外部 REST)
- AuthService → UserRepository → MongoDB

**Kafka → Consumer 服務**
- raw_events topic → order-processor
- raw_events topic → notification-service
- raw_events topic → analytics-service

---

## 2. REST API 端點清單

### 健康檢查
| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/health` | 服務健康狀態檢查 |

### 露營地管理
| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/spot` | 列出所有露營地 |
| GET | `/api/spot/{spot_id}` | 取得特定露營地詳情 |

### 訂單處理
| 方法 | 路徑 | 說明 | 需要認證 |
|------|------|------|----------|
| POST | `/api/checkout` | 處理訂單結帳 | 是 |
| GET | `/api/order` | 取得使用者訂單列表 | 是 |

### 認證
| 方法 | 路徑 | 說明 |
|------|------|------|
| POST | `/api/auth/register` | 使用者註冊 |
| POST | `/api/auth/login` | 使用者登入 |
| POST | `/api/auth/logout` | 使用者登出 |

### 收藏管理
| 方法 | 路徑 | 說明 | 需要認證 |
|------|------|------|----------|
| POST | `/api/favorite` | 新增收藏 | 是 |
| GET | `/api/favorite` | 取得收藏列表 | 是 |
| DELETE | `/api/favorite/{spot_id}` | 取消收藏 | 是 |

---

## 3. 訂單結帳流程步驟

### 步驟 1: 接收請求
- 使用者點擊結帳按鈕
- Frontend 發送 POST 請求到 `/api/checkout`
- CheckoutResource 接收請求並開始處理

### 步驟 2: 計算住宿天數
- 解析 checkInDate 和 checkOutDate
- 計算天數差異
- 最少為 1 天

### 步驟 3: 驗證訂單
- OrderValidateService 驗證必填欄位
- 驗證日期格式 (YYYY-MM-DD)
- 驗證 Email 格式

### 步驟 4: 取得價格
- PricingService 呼叫 SpotService
- SpotService 向外部 Spot Service API 查詢價格
- 如果失敗,使用 Fallback 預設價格
- 計算總價 = 單價 × 天數

### 步驟 5: 處理優惠券(如有)
- 查詢優惠券是否存在
- 檢查優惠券狀態 (UNUSED/USED/EXPIRED)
- 檢查優惠券是否過期
- 標記優惠券為已使用
- 計算折扣後金額

### 步驟 6: 儲存訂單
- 建立 Order 物件
- OrderRepository 儲存到 MongoDB
- 訂單狀態設為 "confirmed"

### 步驟 7: 記錄審計
- AuditService 記錄操作
- 記錄使用者 Email 和操作類型

### 步驟 8: 報表處理
- ReportingService 生成訂單摘要
- 執行稽核矩陣
- 執行冗餘訂單掃描

### 步驟 9: 非同步 Kafka 發送
- 建立背景執行緒
- KafkaCheckoutService 轉換為 Avro 格式
- 加入 Instana Trace Headers
- 發送到 Kafka raw_events topic

### 步驟 10: 返回回應
- 返回 200 OK
- 包含訂單詳情、價格、折扣資訊

---

## 4. 認證流程

### 使用者註冊流程

**步驟 1**: 使用者填寫註冊表單(姓名、Email、密碼)

**步驟 2**: Frontend 發送 POST 請求到 `/api/auth/register`

**步驟 3**: AuthService 檢查 Email 是否已存在
- 如果存在 → 返回 400 Bad Request
- 如果不存在 → 繼續處理

**步驟 4**: 密碼雜湊處理
- 使用 BCrypt 雜湊密碼
- 不儲存明文密碼

**步驟 5**: 儲存使用者
- 建立 User 物件
- UserRepository 儲存到 MongoDB

**步驟 6**: 生成 JWT Token
- 包含 userId, email, name
- 使用 JWT_SECRET 簽章

**步驟 7**: 返回認證回應
- 返回 token, userId, name, email
- Frontend 儲存 token 到 localStorage

### 使用者登入流程

**步驟 1**: 使用者輸入 Email 和密碼

**步驟 2**: Frontend 發送 POST 請求到 `/api/auth/login`

**步驟 3**: AuthService 查詢使用者
- 根據 Email 查詢 MongoDB
- 如果不存在 → 返回 401 Unauthorized

**步驟 4**: 驗證密碼
- 使用 BCrypt 驗證密碼雜湊
- 如果不符 → 返回 401 Unauthorized

**步驟 5**: 生成 JWT Token

**步驟 6**: 返回認證回應

### JWT 驗證流程

**步驟 1**: Frontend 在請求 Header 加入 `Authorization: Bearer {token}`

**步驟 2**: JwtAuthFilter 攔截請求

**步驟 3**: 提取並驗證 Token
- 檢查 Token 格式
- 驗證簽章
- 檢查是否過期

**步驟 4**: 建立 AuthenticatedUser
- 從 Token 提取使用者資訊
- 注入到 Request Context

**步驟 5**: 繼續處理請求

---

## 5. Kafka 訊息流程

### Producer 發送流程

**初始化階段** (只執行一次)
1. 建立 KafkaProducer 實例
2. 設定 bootstrap.servers
3. 設定 Avro Serializer
4. 連接 Schema Registry

**發送階段** (每次訂單)
1. 將 OrderPayload 轉換為 Avro GenericRecord
2. 填充所有欄位 (event_type, event_id, order_id 等)
3. 從 Instana SDK 取得 Trace Headers
4. 將 Trace Headers 加入 Kafka Message Headers
5. 同步發送到 Kafka (使用 .get() 等待確認)
6. 記錄發送成功日誌

### Consumer 消費流程

**啟動階段**
1. 建立 KafkaConsumer 實例
2. 訂閱 raw_events topic
3. 設定 Consumer Group ID

**消費階段** (持續輪詢)
1. 執行 poll() 取得訊息批次
2. 對每筆訊息:
   - 提取 Kafka Headers 中的 Instana Trace Headers
   - 恢復 Trace Context (繼承 Parent Span)
   - 解析 Avro Record
   - 執行業務邏輯 (OrderProcessor.process())
   - 更新訂單狀態
   - 發送通知
3. 執行 commitSync() 提交 offset

---

## 6. Instana 追蹤架構

### Span 類型說明

| Span 類型 | 用途 | 範例 |
|-----------|------|------|
| **ENTRY** | 服務入口點 | HTTP 請求、批次作業 |
| **EXIT** | 外部呼叫 | Kafka 發送、HTTP Client |
| **INTERMEDIATE** | 內部邏輯 | 業務方法、資料存取 |

### 主要 Span 清單

**HTTP Entry Spans**
- `camping-api-checkout` - 訂單結帳端點
- `camping-api-list-spots` - 列出露營地端點
- `camping-api-auth-register` - 註冊端點
- `camping-api-auth-login` - 登入端點

**Business Logic Spans**
- `camping-order-validate` - 訂單驗證
- `camping-pricing-calculate` - 價格計算
- `camping-spot-lookup` - 露營地查詢
- `camping-audit-record` - 審計記錄

**Data Access Spans**
- `camping-user-repo-save` - 儲存使用者
- `camping-order-repo-save` - 儲存訂單
- `camping-coupon-repo-findByCouponCode` - 查詢優惠券
- `camping-favorite-repo-save` - 儲存收藏

**Kafka Spans**
- `camping-kafka-checkout-send` - Kafka 發送 (EXIT)
- `camping-kafka-producer-init` - Producer 初始化
- `camping-kafka-record-build` - Record 建立

### 追蹤標籤類別

**HTTP 標籤**
- `tags.http.method` - HTTP 方法 (GET/POST)
- `tags.http.url` - 請求 URL
- `tags.http.status_code` - 狀態碼 (200/400/500)

**業務標籤**
- `tags.checkout.order_id` - 訂單 ID
- `tags.checkout.total` - 訂單總額
- `tags.checkout.discount_amount` - 折扣金額
- `tags.checkout.coupon_code` - 優惠券代碼

**Kafka 標籤**
- `tags.kafka.topic` - Topic 名稱
- `tags.kafka.key` - 訊息 Key
- `tags.event.type` - 事件類型

**錯誤標籤**
- `tags.error` - 錯誤標記 (true/false)
- `tags.error.message` - 錯誤訊息
- `tags.error.type` - 錯誤類型

**日誌標籤**
- `log.0.level` - 日誌級別 (INFO/WARN/ERROR)
- `log.0.msg` - 日誌訊息

---

## 7. 錯誤處理機制

### 全域錯誤處理器

| Exception 類型 | Mapper | HTTP 狀態碼 | 說明 |
|----------------|--------|-------------|------|
| BadRequestException | BadRequestMapper | 400 | 請求參數錯誤 |
| NotFoundException | NotFoundMapper | 404 | 資源不存在 |
| NotAuthorizedException | UnauthorizedMapper | 401 | 未授權 |
| ConstraintViolationException | ConstraintViolationMapper | 400 | 驗證失敗 |
| 其他 Exception | GlobalExceptionMapper | 500 | 伺服器錯誤 |

### Spot Service 錯誤分類

| 錯誤類型 | 原因 | 處理方式 |
|----------|------|----------|
| connection_refused | 服務未啟動 | 使用 Fallback 資料 |
| timeout | 連線逾時 | 使用 Fallback 資料 |
| unknown_host | DNS 解析失敗 | 使用 Fallback 資料 |
| unauthorized | 認證失敗 | 使用 Fallback 資料 |
| unknown | 其他錯誤 | 使用 Fallback 資料 |

所有錯誤都會:
1. 記錄到 Instana (tags.error = "true")
2. 記錄錯誤類型和提示
3. 返回 Fallback 資料確保服務可用

### 優惠券驗證錯誤

| 驗證項目 | 失敗條件 | 錯誤訊息 |
|----------|----------|----------|
| 存在性 | 優惠券不存在 | "優惠券不存在" |
| 狀態 | 已使用或已過期 | "優惠券已使用或已過期" |
| 期限 | 超過有效期限 | "優惠券已過期" |
| 使用 | 使用失敗 | "優惠券使用失敗" |

---

## 8. 資料模型

### Order (訂單)

| 欄位 | 類型 | 說明 |
|------|------|------|
| orderId | String | 訂單 ID (唯一) |
| userId | String | 使用者 ID |
| userEmail | String | 使用者 Email |
| spotId | String | 露營地 ID |
| spotName | String | 露營地名稱 |
| checkInDate | String | 入住日期 (YYYY-MM-DD) |
| checkOutDate | String | 退房日期 (YYYY-MM-DD) |
| nights | int | 住宿天數 |
| unitPrice | int | 單價 |
| total | int | 總價 |
| discountAmount | int | 折扣金額 |
| finalTotal | int | 最終金額 |
| couponCode | String | 優惠券代碼 |
| status | String | 訂單狀態 |
| createdAt | long | 建立時間戳 |

### User (使用者)

| 欄位 | 類型 | 說明 |
|------|------|------|
| userId | String | 使用者 ID (唯一) |
| name | String | 姓名 |
| email | String | Email (唯一) |
| passwordHash | String | 密碼雜湊 |
| createdAt | long | 註冊時間戳 |

### Coupon (優惠券)

| 欄位 | 類型 | 說明 |
|------|------|------|
| couponId | String | 優惠券 ID (唯一) |
| couponCode | String | 優惠券代碼 (唯一) |
| userId | String | 所屬使用者 |
| discountAmount | int | 折扣金額 |
| status | CouponStatus | 狀態 (UNUSED/USED/EXPIRED) |
| usedAt | Long | 使用時間 |
| usedOrderId | String | 使用的訂單 ID |
| expiresAt | long | 過期時間 |
| createdAt | long | 建立時間 |

### Favorite (收藏)

| 欄位 | 類型 | 說明 |
|------|------|------|
| favoriteId | String | 收藏 ID (唯一) |
| userId | String | 使用者 ID |
| spotId | String | 露營地 ID |
| active | boolean | 是否啟用 |
| createdAt | long | 建立時間 |
| updatedAt | long | 更新時間 |

---

## 9. 部署架構

### 生產環境組件

**負載平衡層**
- Nginx 或 HAProxy
- 分散流量到多個 JBoss 實例

**應用伺服器層**
- JBoss EAP 8 Instance 1
- JBoss EAP 8 Instance 2
- JBoss EAP 8 Instance 3
- 每個實例部署 camping-api.war

**訊息佇列層**
- Kafka Broker 1
- Kafka Broker 2
- Kafka Broker 3
- Schema Registry

**資料庫層**
- MongoDB Primary
- MongoDB Secondary 1
- MongoDB Secondary 2
- 自動複製和故障轉移

**Consumer 服務層**
- order-processor (獨立 Java 應用)
- notification-service (獨立 Java 應用)
- analytics-service (獨立 Java 應用)

**監控層**
- Instana Agent (安裝在每個節點)
- Instana Backend (集中式)

**外部服務**
- Spot Service API
- Email Service

---

## 10. 技術棧總覽

### 前端技術
- **React 18** - UI 框架
- **Vite** - 建置工具
- **TypeScript** - 型別安全

### 後端技術
- **Java 17** - 程式語言
- **Jakarta EE 10** - 企業級框架
- **JAX-RS 3.1** - REST API
- **CDI 4.0** - 依賴注入
- **JBoss EAP 8** - 應用伺服器

### 資料技術
- **MongoDB 4.11.1** - NoSQL 資料庫
- **Apache Kafka 3.7.1** - 訊息佇列
- **Avro** - 訊息序列化
- **Schema Registry 7.7.1** - Schema 管理

### 監控技術
- **Instana SDK 1.2.0** - 手動追蹤
- **Instana Agent** - 自動追蹤
- **SLF4J 2.0.13** - 日誌框架

### 建置工具
- **Maven 3.x** - 後端建置
- **npm/pnpm** - 前端建置

---

## 11. 關鍵設計決策

### 為什麼選擇 Kafka?
- **解耦**: Producer 和 Consumer 獨立部署
- **可擴展**: 輕鬆增加 Consumer 實例
- **可靠**: 訊息持久化,不會遺失
- **順序**: 保證同一 Key 的訊息順序

### 為什麼使用 MongoDB?
- **靈活 Schema**: 訂單結構可能變化
- **高效能**: 讀寫效能優異
- **易擴展**: 支援水平擴展
- **JSON 原生**: 與 REST API 完美配合

### 為什麼整合 Instana?
- **端到端追蹤**: 從 HTTP 到 Kafka 到 Consumer
- **自動發現**: 無需手動配置服務
- **低開銷**: 對效能影響極小
- **豐富視覺化**: 完整的呼叫鏈和效能分析

### 為什麼使用 Avro?
- **Schema 演進**: 支援向後/向前相容
- **緊湊**: 二進位格式,節省空間
- **型別安全**: Schema 強制驗證
- **跨語言**: Java, Python, Go 都支援

---

## 12. 效能考量

### 快取策略
- Spot Service 回應可快取 (TTL: 5 分鐘)
- JWT Token 驗證結果可快取
- 使用 CDI @ApplicationScoped 單例

### 連線池
- MongoDB 連線池: 最小 10, 最大 100
- HTTP Client 連線池: 最大 50
- Kafka Producer: 單例重用

### 非同步處理
- Kafka 發送使用背景執行緒
- 報表處理不阻塞主流程
- 使用 ManagedExecutorService

### 資料庫索引
- users.email (唯一索引)
- orders.userId (索引)
- orders.orderId (唯一索引)
- coupons.couponCode (唯一索引)
- favorites.userId + spotId (複合索引)

---

## 13. 安全性措施

### 認證與授權
- JWT Token 認證
- Token 包含使用者資訊
- 敏感端點需要認證

### 密碼安全
- BCrypt 雜湊
- 不儲存明文密碼
- Salt 自動生成

### 輸入驗證
- Bean Validation 註解
- 驗證所有使用者輸入
- 防止 SQL/NoSQL 注入

### CORS 設定
- 配置允許的來源
- 限制 HTTP 方法
- 控制 Headers

### 錯誤處理
- 不洩漏敏感資訊
- 統一錯誤回應格式
- 記錄到監控系統

---

**文件版本**: 1.0.0  
**最後更新**: 2026-05-26  
**維護者**: Camping API 開發團隊

**注意**: 本文件使用表格和文字描述,確保在任何環境下都能正確閱讀。如需視覺化圖表,請參考:
- **ARCHITECTURE_DIAGRAMS.md** - Mermaid 圖表版本(需支援 Mermaid 的環境)
- **TECHNICAL_DOCUMENTATION.md** - 完整技術文件