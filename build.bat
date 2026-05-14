@echo off
REM ============================================
REM Camping API 建置腳本
REM ============================================

echo.
echo ========================================
echo   Camping API 建置腳本
echo ========================================
echo.

REM 檢查 Maven 是否安裝
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [錯誤] 找不到 Maven！
    echo.
    echo 請確認：
    echo 1. Maven 已安裝
    echo 2. Maven 的 bin 目錄已加入 PATH 環境變數
    echo.
    echo Maven 下載位置：https://maven.apache.org/download.cgi
    echo.
    pause
    exit /b 1
)

echo [1/3] 檢查 Maven 版本...
call mvn -version
echo.

echo [2/3] 清理舊的建置檔案...
call mvn clean
if %ERRORLEVEL% NEQ 0 (
    echo [錯誤] Maven clean 失敗！
    pause
    exit /b 1
)
echo.

echo [3/3] 建置專案並打包 WAR...
call mvn package -DskipTests -DskipFrontend=true
if %ERRORLEVEL% NEQ 0 (
    echo [錯誤] Maven package 失敗！
    pause
    exit /b 1
)
echo.

echo ========================================
echo   建置成功！
echo ========================================
echo.
echo WAR 檔案位置：
echo   target\camping-api.war
echo.
echo 檔案大小：
dir target\camping-api.war | find "camping-api.war"
echo.
echo 下一步：
echo 1. 複製 WAR 到 JBoss：
echo    copy target\camping-api.war %%JBOSS_HOME%%\standalone\deployments\
echo 2. 啟動 JBoss
echo.
pause

@REM Made with Bob
