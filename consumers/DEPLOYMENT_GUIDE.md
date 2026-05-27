# Kafka Consumer Services - 部署指南

## 📋 部署總覽

本指南說明如何部署和管理 Kafka Consumer 服務。

---

## 🎯 部署策略

### 選項 1: 獨立 Java 應用程式 (推薦)

**優點**:
- ✅ 簡單易用
- ✅ 獨立運行,不依賴應用伺服器
- ✅ 容易擴展
- ✅ 資源隔離

**適用場景**:
- 開發和測試環境
- 小規模生產環境
- 需要快速部署

### 選項 2: Windows Service

**優點**:
- ✅ 開機自動啟動
- ✅ 系統整合良好
- ✅ 易於管理

**適用場景**:
- Windows Server 生產環境
- 需要高可用性

### 選項 3: Docker 容器

**優點**:
- ✅ 環境一致性
- ✅ 易於擴展
- ✅ 資源限制

**適用場景**:
- 容器化環境
- Kubernetes 部署
- 多環境部署

---

## 🚀 部署步驟

### 方式 1: 獨立 Java 應用程式

#### 1. 建置應用程式

```powershell
cd consumers/order-processor
.\build.ps1
```

#### 2. 準備部署目錄

```powershell
# 建立部署目錄
mkdir C:\Services\order-processor

# 複製 JAR 檔案
Copy-Item target\order-processor-1.0.0-jar-with-dependencies.jar `
  C:\Services\order-processor\

# 複製執行腳本
Copy-Item run.ps1 C:\Services\order-processor\
```

#### 3. 設定環境變數

建立 `C:\Services\order-processor\config.ps1`:

```powershell
# Kafka 配置
$env:KAFKA_BOOTSTRAP_SERVER="10.107.85.239:9092"
$env:SCHEMA_REGISTRY_URL="http://10.107.85.239:8081"

# Consumer 配置
$env:CONSUMER_GROUP_ID="order-processor-group"
$env:SOURCE_TOPIC="raw_events"
$env:TARGET_TOPIC="orders_processed"

# 效能配置
$env:POLL_TIMEOUT_MS="1000"
$env:MAX_POLL_RECORDS="100"

# Instana 配置
$env:INSTANA_AGENT_HOST="localhost"
$env:INSTANA_AGENT_PORT="42699"
```

#### 4. 執行服務

```powershell
cd C:\Services\order-processor

# 載入配置
. .\config.ps1

# 執行服務
java -jar order-processor-1.0.0-jar-with-dependencies.jar
```

---

### 方式 2: Windows Service (使用 NSSM)

#### 1. 下載 NSSM

```powershell
# 下載 NSSM (Non-Sucking Service Manager)
# https://nssm.cc/download

# 解壓縮到 C:\Tools\nssm
```

#### 2. 安裝服務

```powershell
# 設定 Java 路徑
$javaPath = "C:\Program Files\Java\jdk-17\bin\java.exe"
$jarPath = "C:\Services\order-processor\order-processor-1.0.0-jar-with-dependencies.jar"

# 安裝服務
C:\Tools\nssm\nssm.exe install OrderProcessor $javaPath

# 設定參數
C:\Tools\nssm\nssm.exe set OrderProcessor AppParameters "-jar $jarPath"
C:\Tools\nssm\nssm.exe set OrderProcessor AppDirectory "C:\Services\order-processor"
C:\Tools\nssm\nssm.exe set OrderProcessor DisplayName "Order Processor Service"
C:\Tools\nssm\nssm.exe set OrderProcessor Description "Kafka Consumer for processing checkout orders"

# 設定環境變數
C:\Tools\nssm\nssm.exe set OrderProcessor AppEnvironmentExtra `
  "KAFKA_BOOTSTRAP_SERVER=10.107.85.239:9092" `
  "SCHEMA_REGISTRY_URL=http://10.107.85.239:8081" `
  "CONSUMER_GROUP_ID=order-processor-group"

# 設定日誌
C:\Tools\nssm\nssm.exe set OrderProcessor AppStdout "C:\Services\order-processor\logs\stdout.log"
C:\Tools\nssm\nssm.exe set OrderProcessor AppStderr "C:\Services\order-processor\logs\stderr.log"

# 設定自動重啟
C:\Tools\nssm\nssm.exe set OrderProcessor AppExit Default Restart
C:\Tools\nssm\nssm.exe set OrderProcessor AppRestartDelay 5000
```

#### 3. 管理服務

```powershell
# 啟動服務
C:\Tools\nssm\nssm.exe start OrderProcessor

# 查看狀態
C:\Tools\nssm\nssm.exe status OrderProcessor

# 停止服務
C:\Tools\nssm\nssm.exe stop OrderProcessor

# 重啟服務
C:\Tools\nssm\nssm.exe restart OrderProcessor

# 移除服務
C:\Tools\nssm\nssm.exe remove OrderProcessor confirm
```

#### 4. 使用 Windows 服務管理員

```powershell
# 開啟服務管理員
services.msc

# 或使用 PowerShell
Get-Service OrderProcessor
Start-Service OrderProcessor
Stop-Service OrderProcessor
Restart-Service OrderProcessor
```

---

### 方式 3: Docker 容器

#### 1. 建立 Dockerfile

建立 `consumers/order-processor/Dockerfile`:

```dockerfile
FROM openjdk:17-slim

# 設定工作目錄
WORKDIR /app

# 複製 JAR 檔案
COPY target/order-processor-1.0.0-jar-with-dependencies.jar app.jar

# 設定預設環境變數
ENV KAFKA_BOOTSTRAP_SERVER=10.107.85.239:9092
ENV SCHEMA_REGISTRY_URL=http://10.107.85.239:8081
ENV CONSUMER_GROUP_ID=order-processor-group
ENV SOURCE_TOPIC=raw_events
ENV TARGET_TOPIC=orders_processed

# 健康檢查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD ps aux | grep java || exit 1

# 執行應用程式
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 2. 建置 Docker Image

```powershell
cd consumers/order-processor

# 建置 Maven 專案
mvn clean package

# 建置 Docker Image
docker build -t order-processor:1.0.0 .

# 查看 Image
docker images | Select-String order-processor
```

#### 3. 執行容器

```powershell
# 執行容器
docker run -d `
  --name order-processor `
  --restart unless-stopped `
  -e KAFKA_BOOTSTRAP_SERVER=10.107.85.239:9092 `
  -e SCHEMA_REGISTRY_URL=http://10.107.85.239:8081 `
  -e CONSUMER_GROUP_ID=order-processor-group `
  order-processor:1.0.0

# 查看日誌
docker logs -f order-processor

# 查看狀態
docker ps | Select-String order-processor

# 停止容器
docker stop order-processor

# 移除容器
docker rm order-processor
```

#### 4. Docker Compose

建立 `consumers/docker-compose.yml`:

```yaml
version: '3.8'

services:
  order-processor:
    build: ./order-processor
    container_name: order-processor
    restart: unless-stopped
    environment:
      - KAFKA_BOOTSTRAP_SERVER=10.107.85.239:9092
      - SCHEMA_REGISTRY_URL=http://10.107.85.239:8081
      - CONSUMER_GROUP_ID=order-processor-group
      - SOURCE_TOPIC=raw_events
      - TARGET_TOPIC=orders_processed
    networks:
      - kafka-network

networks:
  kafka-network:
    external: true
```

執行:

```powershell
# 啟動所有服務
docker-compose up -d

# 查看日誌
docker-compose logs -f

# 停止所有服務
docker-compose down
```

---

## 📊 監控和維護

### 1. 日誌管理

#### 查看即時日誌

```powershell
# 獨立應用程式
# 日誌輸出到 stdout

# Windows Service
Get-Content C:\Services\order-processor\logs\stdout.log -Tail 50 -Wait

# Docker
docker logs -f order-processor
```

#### 日誌輪替

建立 `C:\Services\order-processor\rotate-logs.ps1`:

```powershell
$logDir = "C:\Services\order-processor\logs"
$maxSize = 10MB
$maxFiles = 5

Get-ChildItem $logDir -Filter "*.log" | ForEach-Object {
    if ($_.Length -gt $maxSize) {
        # 輪替日誌
        for ($i = $maxFiles; $i -gt 0; $i--) {
            $oldFile = "$($_.FullName).$i"
            $newFile = "$($_.FullName).$($i+1)"
            if (Test-Path $oldFile) {
                Move-Item $oldFile $newFile -Force
            }
        }
        Move-Item $_.FullName "$($_.FullName).1" -Force
        New-Item $_.FullName -ItemType File
    }
}
```

設定排程任務每天執行。

### 2. 健康檢查

建立 `C:\Services\order-processor\health-check.ps1`:

```powershell
# 檢查程序是否運行
$process = Get-Process -Name java -ErrorAction SilentlyContinue | 
    Where-Object { $_.CommandLine -like "*order-processor*" }

if ($process) {
    Write-Host "✅ Service is running (PID: $($process.Id))" -ForegroundColor Green
    
    # 檢查 Consumer Lag
    $lag = & kafka-consumer-groups --bootstrap-server 10.107.85.239:9092 `
        --group order-processor-group --describe | 
        Select-String "LAG"
    
    Write-Host "Consumer Lag:" -ForegroundColor Yellow
    Write-Host $lag
    
    exit 0
} else {
    Write-Host "❌ Service is not running" -ForegroundColor Red
    exit 1
}
```

### 3. 效能監控

```powershell
# CPU 和記憶體使用
Get-Process -Name java | 
    Where-Object { $_.CommandLine -like "*order-processor*" } |
    Select-Object ProcessName, CPU, WorkingSet64, Threads

# Consumer Lag
kafka-consumer-groups --bootstrap-server 10.107.85.239:9092 `
    --group order-processor-group --describe

# Kafka Topic 訊息數
kafka-run-class kafka.tools.GetOffsetShell `
    --broker-list 10.107.85.239:9092 `
    --topic raw_events --time -1
```

---

## 🔧 故障排除

### 問題 1: 服務無法啟動

**檢查清單**:
```powershell
# 1. 檢查 Java 版本
java -version

# 2. 檢查 JAR 檔案
Test-Path C:\Services\order-processor\order-processor-1.0.0-jar-with-dependencies.jar

# 3. 檢查 Kafka 連線
Test-NetConnection -ComputerName 10.107.85.239 -Port 9092

# 4. 檢查環境變數
Get-ChildItem Env: | Where-Object { $_.Name -like "KAFKA*" }

# 5. 查看錯誤日誌
Get-Content C:\Services\order-processor\logs\stderr.log -Tail 50
```

### 問題 2: Consumer Lag 持續增加

**解決方案**:
```powershell
# 1. 增加 Consumer 實例數
# 修改 CONSUMER_GROUP_ID 為不同的值,啟動多個實例

# 2. 增加 MAX_POLL_RECORDS
$env:MAX_POLL_RECORDS="500"

# 3. 檢查處理邏輯效能
# 查看日誌中的處理時間
```

### 問題 3: 記憶體洩漏

**解決方案**:
```powershell
# 1. 設定 JVM 記憶體限制
java -Xms512m -Xmx1024m -jar order-processor-1.0.0-jar-with-dependencies.jar

# 2. 啟用 GC 日誌
java -Xlog:gc*:file=gc.log -jar order-processor-1.0.0-jar-with-dependencies.jar

# 3. 定期重啟服務 (臨時方案)
# 使用排程任務每天重啟
```

---

## 📈 擴展部署

### 水平擴展 (多個 Consumer 實例)

```powershell
# 實例 1
$env:CONSUMER_GROUP_ID="order-processor-group"
java -jar order-processor-1.0.0-jar-with-dependencies.jar

# 實例 2 (不同機器或不同終端)
$env:CONSUMER_GROUP_ID="order-processor-group"
java -jar order-processor-1.0.0-jar-with-dependencies.jar

# 實例 3
$env:CONSUMER_GROUP_ID="order-processor-group"
java -jar order-processor-1.0.0-jar-with-dependencies.jar
```

**注意**: 最多 3 個實例 (對應 raw_events 的 3 個 partitions)

### 負載平衡

Kafka 會自動在 Consumer Group 中的實例之間平衡 partitions:

```
raw_events (3 partitions)
├─ Partition 0 → Instance 1
├─ Partition 1 → Instance 2
└─ Partition 2 → Instance 3
```

---

## ✅ 部署檢查清單

### 部署前

- [ ] Java 17 已安裝
- [ ] Maven 已安裝並建置成功
- [ ] Kafka 可連接
- [ ] Schema Registry 可連接
- [ ] 環境變數已設定
- [ ] 部署目錄已建立
- [ ] 日誌目錄已建立

### 部署後

- [ ] 服務成功啟動
- [ ] 成功訂閱 raw_events topic
- [ ] Consumer Lag 為 0 或接近 0
- [ ] 可以處理訊息
- [ ] Instana 可以追蹤
- [ ] 日誌正常輸出
- [ ] 健康檢查通過

### 生產環境

- [ ] 設定為 Windows Service 或 systemd
- [ ] 配置自動重啟
- [ ] 設定日誌輪替
- [ ] 配置監控告警
- [ ] 文件化部署流程
- [ ] 建立備份計劃

---

## 📚 相關資源

- [QUICK_START.md](QUICK_START.md) - 快速開始指南
- [README.md](README.md) - 服務總覽
- [order-processor/README.md](order-processor/README.md) - 詳細文件

---

**部署完成後,記得測試和監控服務運行狀況!** 🚀