# Order Processor - Run Script
# 執行 order-processor 服務

Write-Host "=== Starting Order Processor Service ===" -ForegroundColor Cyan
Write-Host ""

# 檢查 JAR 檔案是否存在
$jarFile = "target/order-processor-1.0.0-jar-with-dependencies.jar"

if (-not (Test-Path $jarFile)) {
    Write-Host "❌ JAR file not found: $jarFile" -ForegroundColor Red
    Write-Host "Please run build.ps1 first" -ForegroundColor Yellow
    exit 1
}

# 顯示配置
Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Kafka Bootstrap Server: $($env:KAFKA_BOOTSTRAP_SERVER ?? '10.107.85.239:9092')" -ForegroundColor White
Write-Host "  Schema Registry URL: $($env:SCHEMA_REGISTRY_URL ?? 'http://10.107.85.239:8081')" -ForegroundColor White
Write-Host "  Consumer Group ID: $($env:CONSUMER_GROUP_ID ?? 'order-processor-group')" -ForegroundColor White
Write-Host "  Source Topic: $($env:SOURCE_TOPIC ?? 'raw_events')" -ForegroundColor White
Write-Host ""

Write-Host "Starting service..." -ForegroundColor Green
Write-Host "Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

# 執行服務
java -jar $jarFile

# Made with Bob
