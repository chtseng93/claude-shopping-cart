# 常用指令

## Docker Compose

**首次啟動 或 Dockerfile 有異動：**

```powershell
docker compose up --build
```

**日常開發指令：**

| 指令 | 說明 |
|------|------|
| `docker compose up -d` | 背景啟動所有服務（不重建 image） |
| `docker compose up -d db` | 只啟動資料庫 |
| `docker compose up -d --build backend` | 只重建並重啟後端 |
| `docker compose restart backend` | 重啟某個服務（不重建） |
| `docker compose logs -f backend` | 即時查看後端 log |
| `docker compose down` | 停止所有容器（保留資料庫 volume） |
| `docker compose down -v` | 停止並移除所有容器與 volume（**資料庫資料會清空**） |

---

## 測試

**後端整合測試（Testcontainers，需 Docker）：**

```powershell
# 執行全部測試
mvn -f backend/pom.xml test

# 執行單一測試類別
mvn -f backend/pom.xml test -Dtest=CartCleanupIntegrationTest
```

**E2E 測試（Playwright，需先啟動服務）：**

```powershell
cd e2e
npx playwright test
npx playwright test --ui        # 開啟 UI 模式
npx playwright show-report      # 查看測試報告
```
