# Shopping Cart 購物車

依據**Claude Code 全端專案開發實作課程**做練習，使用 **Claude Code** 搭配 **Antigravity** 開發的全端購物車專案，採 **SDD（Spec-Driven Development，規格驅動開發）** 流程：**先寫規格 → 拆解任務 → 最後實作**。

---

## 技術棧

- **後端**：Java 17 + Spring Boot 3.x + Spring Data JPA
- **前端**：React + Vite
- **資料庫**：PostgreSQL 18
- **建置**：Maven（後端）／ npm（前端）
- **測試**：Testcontainers + JUnit 5（後端整合測試）／ Playwright（E2E 測試）

---

## 快速啟動

```powershell
# 首次啟動 或 有程式碼異動需要重新建置
```

| 指令 | 說明 |
|------|------|
| `docker compose down -v` | 停止並移除所有容器與 volume（含資料庫資料） |
| `docker compose up --build` | 重新建置 image 並啟動所有服務 |


---

## 開發方法：SDD（規格先行）

不直接寫程式，而是先把需求與規格定義清楚，讓 AI 依文件實作。流程如下：

1. **建立 [prd.md](prd.md)** — 描述產品需求（技術棧、核心實體、功能範圍）。
2. **依 prd.md 修改 [CLAUDE.md](CLAUDE.md)** — 設定專案開發規範與技術棧。
3. **撰寫 [spec.md](spec.md)** — 依需求產出規格。
4. **撰寫 [api.md](api.md)** — 定義 RESTful API 端點與請求/回應格式。
5. **拆分任務** — 用 TodoWrite 拆解任務，並建立 [todolist.md](todolist.md) 紀錄。
6. **請 AI 分析開發順序** — 分析任務相依與可平行的部分。
7. **開始實作** — 請 AI「根據 todolist.md / api.md / spec.md 開發」。
8. **撰寫測試** — E2E 測試（Playwright）驗證完整流程。

---

## 文件

| 文件 | 用途 |
|------|------|
| [prd.md](prd.md) | 產品需求 |
| [spec.md](spec.md) | 規格文件 |
| [api.md](api.md) | API 文件 |
| [CLAUDE.md](CLAUDE.md) | 開發規範 |
| [todolist.md](todolist.md) | 任務追蹤 |

---

## 參考資料

- Claude Code 全端專案開發實作課程 (Kevin)
