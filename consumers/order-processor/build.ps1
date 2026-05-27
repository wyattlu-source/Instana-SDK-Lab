# Order Processor - Build Script
# 建置 order-processor 服務

Write-Host "=== Building Order Processor Service ===" -ForegroundColor Cyan
Write-Host ""

# 檢查 Maven
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Maven not found. Please install Maven first." -ForegroundColor Red
    exit 1
}

Write-Host "Maven version:" -ForegroundColor Yellow
mvn -version
Write-Host ""

# 清理舊的建置
Write-Host "Cleaning old build..." -ForegroundColor Yellow
mvn clean

# 建置專案
Write-Host ""
Write-Host "Building project..." -ForegroundColor Yellow
mvn package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Build successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Generated files:" -ForegroundColor Cyan
    Write-Host "  • target/order-processor-1.0.0.jar" -ForegroundColor White
    Write-Host "  • target/order-processor-1.0.0-jar-with-dependencies.jar" -ForegroundColor White
    Write-Host "  • target/lib/ (dependencies)" -ForegroundColor White
    Write-Host ""
    Write-Host "To run the service:" -ForegroundColor Cyan
    Write-Host "  java -jar target/order-processor-1.0.0-jar-with-dependencies.jar" -ForegroundColor White
    Write-Host "  或執行: .\run.ps1" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "❌ Build failed!" -ForegroundColor Red
    exit 1
}

# Made with Bob
