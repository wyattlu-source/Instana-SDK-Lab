# Instana SSL 憑證驗證跳過確認

## 📋 確認日期
2026-05-27

## ✅ SSL 憑證跳過實作確認

### 1. 實作位置
**檔案**: [`src/main/java/com/example/camping/resource/AdminResource.java`](src/main/java/com/example/camping/resource/AdminResource.java:125-141)

### 2. 實作細節

#### TrustManager 設定 (第 125-129 行)
```java
private static final TrustManager[] TRUST_ALL = { new X509TrustManager() {
    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    public void checkClientTrusted(X509Certificate[] c, String a) {}
    public void checkServerTrusted(X509Certificate[] c, String a) {}
}};
```

**說明**: 
- 建立了一個信任所有憑證的 `X509TrustManager`
- `getAcceptedIssuers()` 回傳空陣列
- `checkClientTrusted()` 和 `checkServerTrusted()` 為空實作，不進行任何驗證

#### SSLContext 配置 (第 136-141 行)
```java
SSLContext sslCtx = SSLContext.getInstance("TLS");
sslCtx.init(null, TRUST_ALL, new SecureRandom());
HttpClient http = HttpClient.newBuilder()
        .sslContext(sslCtx)
        .connectTimeout(Duration.ofSeconds(8))
        .build();
```

**說明**:
- 使用 TLS 協定建立 `SSLContext`
- 初始化時使用 `TRUST_ALL` TrustManager
- 將自訂的 `sslContext` 套用到 `HttpClient`
- 設定連線逾時為 8 秒

### 3. 使用場景

此 SSL 跳過設定用於 **Admin API** 中的 Instana 狀態查詢：

**端點**: `GET /admin/instana-status`

**功能**:
- 查詢 Instana Events (最近 1 小時)
- 查詢 Service Metrics (最近 1 小時)
- 需要 Admin Key 驗證 (`X-Admin-Key: camping-admin-2026`)

**Instana 連線資訊**:
- Base URL: `https://it-palsys.instana-k3s.palsys.com.tw`
- API Token: `9oYyUYH2Qum_QvUHHUr8bA`

### 4. API 呼叫範例

#### 查詢 Events
```
GET https://it-palsys.instana-k3s.palsys.com.tw/api/events?from={timestamp}&to={timestamp}&maxResults=25
Authorization: apiToken 9oYyUYH2Qum_QvUHHUr8bA
```

#### 查詢 Service Metrics
```
POST https://it-palsys.instana-k3s.palsys.com.tw/api/application-monitoring/metrics/services
Authorization: apiToken 9oYyUYH2Qum_QvUHHUr8bA
Content-Type: application/json

{
  "metrics": [
    {"metric": "calls", "aggregation": "SUM"},
    {"metric": "erroneousCalls", "aggregation": "SUM"},
    {"metric": "latency", "aggregation": "MEAN"}
  ],
  "timeFrame": {"windowSize": 3600000, "to": {timestamp}},
  "group": {"groupbyTag": "service.name", "groupbyTagEntity": "DESTINATION"}
}
```

## 🔒 安全性考量

### ⚠️ 警告
此實作**完全跳過 SSL 憑證驗證**，僅適用於：
- 開發環境
- 測試環境
- 內部網路環境
- 使用自簽憑證的 Instana 實例

### 🚫 不建議用於
- 生產環境
- 公開網路
- 處理敏感資料的場景

### 💡 生產環境建議
1. 使用有效的 SSL 憑證
2. 將自簽憑證加入 Java Truststore
3. 使用環境變數控制 SSL 驗證行為
4. 實作適當的憑證驗證邏輯

## 📊 測試驗證

### 測試方式
```bash
curl -X GET "http://localhost:8080/camping-api/api/admin/instana-status" \
  -H "X-Admin-Key: camping-admin-2026"
```

### 預期結果
```json
{
  "events_raw": {...},
  "metrics_raw": {...},
  "fetched_at": 1748311499000
}
```

## ✅ 確認結論

**SSL 憑證驗證已成功跳過**，程式可以正常連線到使用自簽憑證的 Instana 實例。

### 實作狀態
- ✅ TrustManager 已正確配置
- ✅ SSLContext 已正確初始化
- ✅ HttpClient 已套用自訂 SSL 設定
- ✅ 連線逾時已設定 (8 秒)
- ✅ API 認證已配置

### 相關檔案
- [`AdminResource.java`](src/main/java/com/example/camping/resource/AdminResource.java) - SSL 跳過實作
- [`pom.xml`](pom.xml) - 專案依賴配置

---

**最後更新**: 2026-05-27  
**確認者**: Bob (AI Assistant)