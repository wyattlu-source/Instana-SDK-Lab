# ========================================
# Camping Notification Service 啟動腳本
# ========================================
# 設定 Gmail 帳號和 App Password：
#   $env:SMTP_USER = "your.gmail@gmail.com"
#   $env:SMTP_PASS = "xxxx xxxx xxxx xxxx"   # Google App Password（16碼）
#
# 取得 App Password 步驟：
#   1. Google 帳號 → 安全性 → 兩步驟驗證（需先開啟）
#   2. 搜尋「應用程式密碼」→ 選擇「郵件」→ 產生
#   3. 複製 16 碼密碼填入 SMTP_PASS
# ========================================

Set-Location $PSScriptRoot

$jar = "target/notification-service-1.0.0-jar-with-dependencies.jar"
if (-not (Test-Path $jar)) {
    Write-Host "⚠️  找不到 JAR，先執行 build.ps1" -ForegroundColor Yellow
    .\build.ps1
}

# 若未設定 SMTP 則以乾跑模式運行（只輸出 LOG，不真的寄信）
if (-not $env:SMTP_USER) {
    Write-Host "⚠️  SMTP_USER 未設定 → 乾跑模式（只 LOG，不寄信）" -ForegroundColor Yellow
    Write-Host "   設定方式: `$env:SMTP_USER = 'you@gmail.com'" -ForegroundColor Cyan
    Write-Host "             `$env:SMTP_PASS = 'xxxx xxxx xxxx xxxx'" -ForegroundColor Cyan
}

Write-Host "🚀 啟動 Notification Service..." -ForegroundColor Green

java `
  -javaagent:"C:\instana\instana-java-agent.jar" `
  -Dinstana.agent.host=localhost `
  -Dinstana.agent.port=42699 `
  -jar $jar
