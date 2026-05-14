#!/bin/bash
# ============================================
# Camping API 建置腳本
# ============================================

echo ""
echo "========================================"
echo "  Camping API 建置腳本"
echo "========================================"
echo ""

# 檢查 Maven 是否安裝
if ! command -v mvn &> /dev/null; then
    echo "[錯誤] 找不到 Maven！"
    echo ""
    echo "請確認："
    echo "1. Maven 已安裝"
    echo "2. Maven 的 bin 目錄已加入 PATH 環境變數"
    echo ""
    echo "Maven 下載位置：https://maven.apache.org/download.cgi"
    echo ""
    exit 1
fi

echo "[1/3] 檢查 Maven 版本..."
mvn -version
echo ""

echo "[2/3] 清理舊的建置檔案..."
mvn clean
if [ $? -ne 0 ]; then
    echo "[錯誤] Maven clean 失敗！"
    exit 1
fi
echo ""

echo "[3/3] 建置專案並打包 WAR..."
mvn package -DskipTests
if [ $? -ne 0 ]; then
    echo "[錯誤] Maven package 失敗！"
    exit 1
fi
echo ""

echo "========================================"
echo "  建置成功！"
echo "========================================"
echo ""
echo "WAR 檔案位置："
echo "  target/camping-api.war"
echo ""
echo "檔案大小："
ls -lh target/camping-api.war | awk '{print $5, $9}'
echo ""
echo "下一步："
echo "1. 配置 JBoss（參考 QUICK_START.md）"
echo "2. 複製 WAR 到 JBoss："
echo "   cp target/camping-api.war \$JBOSS_HOME/standalone/deployments/"
echo "3. 啟動 JBoss"
echo ""

# Made with Bob
