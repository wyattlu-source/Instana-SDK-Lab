Write-Host "=== 建置 notification-service ===" -ForegroundColor Green
Set-Location $PSScriptRoot
mvn clean package -DskipTests
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ 建置成功: target/notification-service-1.0.0-jar-with-dependencies.jar" -ForegroundColor Green
} else {
    Write-Host "❌ 建置失敗" -ForegroundColor Red
    exit 1
}
