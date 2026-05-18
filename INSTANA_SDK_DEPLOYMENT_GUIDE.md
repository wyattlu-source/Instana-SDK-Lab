# Instana SDK 追蹤開關 - JBoss 部署指南

## 📋 概述

本指南說明如何在 JBoss 環境中啟用或停用 Instana SDK 追蹤功能,以便進行前後效能比較。

---

## 🎯 快速開始

### 方案 A: 啟用 SDK 追蹤 (預設行為)

```bash
# 不設定環境變數,或明確設定為 true
export INSTANA_SDK_ENABLED=true

# 重啟 JBoss
$JBOSS_HOME/bin/standalone.sh
```

### 方案 B: 停用 SDK 追蹤

```bash
# 設定環境變數為 false
export INSTANA_SDK_ENABLED=false

# 重啟 JBoss
$JBOSS_HOME/bin/standalone.sh
```

---

## 🔧 詳細部署步驟

### 1. Windows 環境 (PowerShell)

#### 啟用追蹤
```powershell
# 設定環境變數
$env:INSTANA_SDK_ENABLED = "true"

# 啟動 JBoss
& "$env:JBOSS_HOME\bin\standalone.bat"
```

#### 停用追蹤
```powershell
# 設定環境變數
$env:INSTANA_SDK_ENABLED = "false"

# 啟動 JBoss
& "$env:JBOSS_HOME\bin\standalone.bat"
```

### 2. Linux/Unix 環境

#### 啟用追蹤
```bash
# 設定環境變數
export INSTANA_SDK_ENABLED=true

# 啟動 JBoss
$JBOSS_HOME/bin/standalone.sh
```

#### 停用追蹤
```bash
# 設定環境變數
export INSTANA_SDK_ENABLED=false

# 啟動 JBoss
$JBOSS_HOME/bin/standalone.sh
```

### 3. 永久設定 (推薦用於測試環境)

#### Windows - 系統環境變數
```powershell
# 設定系統環境變數 (需要管理員權限)
[System.Environment]::SetEnvironmentVariable("INSTANA_SDK_ENABLED", "false", "Machine")

# 或使用 GUI: 控制台 > 系統 > 進階系統設定 > 環境變數
```

#### Linux - Profile 設定
```bash
# 編輯 ~/.bashrc 或 ~/.bash_profile
echo 'export INSTANA_SDK_ENABLED=false' >> ~/.bashrc
source ~/.bashrc

# 或編輯 JBoss 啟動腳本
vi $JBOSS_HOME/bin/standalone.conf
# 加入: JAVA_OPTS="$JAVA_OPTS -DINSTANA_SDK_ENABLED=false"
```

---

## 📦 部署流程

### 完整部署步驟

```bash
# 1. 建置應用程式
mvn clean package

# 2. 停止 JBoss (如果正在運行)
$JBOSS_HOME/bin/jboss-cli.sh --connect command=:shutdown

# 3. 設定環境變數
export INSTANA_SDK_ENABLED=false  # 或 true

# 4. 部署 WAR 檔案
cp target/camping-api.war $JBOSS_HOME/standalone/deployments/

# 5. 啟動 JBoss
$JBOSS_HOME/bin/standalone.sh

# 6. 驗證部署
curl http://localhost:8080/camping-api/health
```

---

## 🔍 驗證 SDK 狀態

### 方法 1: 檢查應用程式日誌

```bash
# 查看 JBoss 日誌
tail -f $JBOSS_HOME/standalone/log/server.log

# 如果 SDK 啟用,會看到 [INSTANA-TRACE] 相關日誌
# 如果 SDK 停用,不會有這些日誌
```

### 方法 2: 檢查 Instana Dashboard

- **SDK 啟用**: Instana 會顯示詳細的追蹤資訊,包括自訂 Span
- **SDK 停用**: Instana 只會顯示 Java Agent 自動追蹤的基本資訊

### 方法 3: 測試 API 端點

```bash
# 呼叫 API 並觀察回應時間
curl -w "\nTime: %{time_total}s\n" http://localhost:8080/camping-api/spots

# 比較啟用/停用 SDK 的效能差異
```

---

## 📊 效能比較測試

### 測試腳本範例

```bash
#!/bin/bash

echo "=== Instana SDK 效能比較測試 ==="

# 測試函數
run_test() {
    local sdk_status=$1
    echo ""
    echo "測試配置: INSTANA_SDK_ENABLED=$sdk_status"
    echo "----------------------------------------"
    
    # 重啟 JBoss
    export INSTANA_SDK_ENABLED=$sdk_status
    $JBOSS_HOME/bin/jboss-cli.sh --connect command=:shutdown
    sleep 5
    $JBOSS_HOME/bin/standalone.sh > /dev/null 2>&1 &
    sleep 30
    
    # 執行效能測試
    echo "執行 100 次請求..."
    for i in {1..100}; do
        curl -s -w "%{time_total}\n" -o /dev/null \
            http://localhost:8080/camping-api/spots
    done | awk '{sum+=$1; count++} END {print "平均回應時間:", sum/count, "秒"}'
}

# 測試 SDK 啟用
run_test "true"

# 測試 SDK 停用
run_test "false"

echo ""
echo "測試完成!"
```

### 使用 Apache Bench 進行壓力測試

```bash
# SDK 啟用
export INSTANA_SDK_ENABLED=true
# 重啟 JBoss...
ab -n 1000 -c 10 http://localhost:8080/camping-api/spots

# SDK 停用
export INSTANA_SDK_ENABLED=false
# 重啟 JBoss...
ab -n 1000 -c 10 http://localhost:8080/camping-api/spots
```

---

## 🎛️ 進階配置

### 同時控制 Java Agent 追蹤

如果想要完全停用所有 Instana 追蹤(包括 Agent 自動追蹤):

```bash
# 1. 停用 SDK (應用層)
export INSTANA_SDK_ENABLED=false

# 2. 修改 instana-agent-config.json
{
  "sdk": {
    "enabled": false
  }
}

# 3. 或停止 Instana Agent
sudo systemctl stop instana-agent
```

### JBoss 系統屬性方式

```bash
# 在 standalone.conf 中加入
JAVA_OPTS="$JAVA_OPTS -DINSTANA_SDK_ENABLED=false"

# 或在啟動時指定
$JBOSS_HOME/bin/standalone.sh -Dinstana.sdk.enabled=false
```

---

## ⚠️ 注意事項

### 1. 環境變數優先順序
- 環境變數 `INSTANA_SDK_ENABLED` 優先
- 預設值為 `true` (啟用)
- 只接受 `true` 或 `false` (不區分大小寫)

### 2. 重啟需求
- **必須重啟 JBoss** 才能讓環境變數生效
- 熱部署 (hot deploy) 不會重新讀取環境變數

### 3. @Span 註解行為
- `@Span` 註解由 Instana Agent 處理
- SDK 停用時,`@Span` 仍會被 Agent 處理(如果 Agent 啟用)
- 要完全停用,需同時停用 SDK 和 Agent

### 4. 效能影響
- SDK 停用時,所有 `SpanSupport.annotate()` 呼叫會被跳過
- 效能開銷降至最低 (僅一次布林值檢查)
- 不會影響應用程式功能

---

## 🐛 疑難排解

### 問題 1: 環境變數未生效

**症狀**: 設定 `INSTANA_SDK_ENABLED=false` 但仍看到追蹤日誌

**解決方案**:
```bash
# 確認環境變數已設定
echo $INSTANA_SDK_ENABLED  # Linux/Mac
echo %INSTANA_SDK_ENABLED%  # Windows CMD
echo $env:INSTANA_SDK_ENABLED  # Windows PowerShell

# 確認 JBoss 已重啟
ps aux | grep jboss  # 檢查 JBoss 程序
```

### 問題 2: 應用程式無法啟動

**症狀**: 設定環境變數後應用程式啟動失敗

**解決方案**:
```bash
# 檢查環境變數值是否正確
# 只接受 "true" 或 "false"

# 檢查 JBoss 日誌
tail -f $JBOSS_HOME/standalone/log/server.log

# 移除環境變數恢復預設行為
unset INSTANA_SDK_ENABLED
```

### 問題 3: Instana 仍顯示部分追蹤

**症狀**: SDK 停用但 Instana 仍有追蹤資料

**原因**: Instana Agent 的自動追蹤仍在運作

**說明**: 這是正常的!Agent 會自動追蹤:
- HTTP 請求/回應
- 資料庫查詢
- Kafka 訊息
- 等等

**如需完全停用**: 停止 Instana Agent 或修改 Agent 配置

---

## 📈 比較指標建議

進行前後比較時,建議收集以下指標:

### 應用程式指標
- ✅ 平均回應時間
- ✅ P95/P99 回應時間
- ✅ 吞吐量 (TPS)
- ✅ 錯誤率

### 系統資源指標
- ✅ CPU 使用率
- ✅ 記憶體使用量
- ✅ GC 頻率和時間
- ✅ 執行緒數量

### JBoss 指標
- ✅ 啟動時間
- ✅ 部署時間
- ✅ 連線池使用率

---

## 📝 測試檢查清單

- [ ] 確認環境變數已正確設定
- [ ] 重啟 JBoss 應用伺服器
- [ ] 驗證應用程式正常運作
- [ ] 檢查日誌確認 SDK 狀態
- [ ] 執行基準測試 (SDK 啟用)
- [ ] 執行比較測試 (SDK 停用)
- [ ] 收集並分析效能指標
- [ ] 記錄測試結果和觀察

---

## 🔗 相關文件

- [SDK_TOGGLE_DESIGN.md](SDK_TOGGLE_DESIGN.md) - 完整設計方案
- [TESTING.md](TESTING.md) - 測試指南
- [README.md](README.md) - 專案說明

---

## 📞 支援

如有問題或需要協助,請參考:
- Instana 官方文件: https://www.ibm.com/docs/en/instana-observability
- JBoss 官方文件: https://docs.jboss.org/

---

**最後更新**: 2026-05-18
**版本**: 1.0.0