# Shopping Cart 購物車

<small>依據 **Claude Code 全端專案開發實作課程** 做練習，使用 Claude Code 搭配 Antigravity 開發的全端購物車專案，採 Spec-Driven Development 流程：先寫規格 → 拆解任務 → 最後實作，並實作 Harness Engineering。</small>

---

### Demo

> 完整購物流程：瀏覽商品 → 加入購物車 → 套用 WELCOME10 優惠券 → 結帳成功

<video src="assets/demo.mp4" autoplay loop muted playsinline width="100%"></video>

---

### 技術棧

| 層級 | 技術 |
|------|------|
| 後端 | Java 17 + Spring Boot 3.x + Spring Data JPA |
| 前端 | React + Vite |
| 資料庫 | PostgreSQL 18 |
| 建置 | Maven（後端）／ npm（前端） |
| 測試 | Testcontainers + JUnit 5 ／ Playwright E2E |
| CI/CD & 部署 | GitHub Actions → Render（後端 Docker、前端 Static Site、資料庫 PostgreSQL） |
| 開發 Agent | Claude Code — 規劃／規格／API：Opus；實作：Sonnet |
| IDE | Antigravity |
| Claude Code Skills | `/review`、`/simplify`、`/agents`、`UI/UX Pro Max`、`claude-mem`（跨對話記憶）|

---

### 開發過程

1. 建立 [prd.md](prd.md) — 描述產品需求（技術棧、核心實體、功能範圍）
2. 依 prd.md 修改 [CLAUDE.md](CLAUDE.md) — 設定開發規範與技術棧
3. 撰寫 [spec.md](spec.md) — 依需求產出規格
4. 撰寫 [api.md](api.md) — 定義 RESTful API 端點與格式
5. 拆分任務 — 用 TodoWrite 建立 [todolist.md](todolist.md)
6. 請 AI 分析開發順序 — 分析任務相依與可平行部分
7. 開始實作 — 請 AI 依 todolist / api.md / spec.md 開發
8. 撰寫測試 — Playwright E2E 驗證完整流程
9. 使用 agent-browser 驗證本地購物流程，產出 reports/checkout-sop.md
10. 建立 Agent Browser Checkout Skill
11. 使用 UI/UX Pro Max 優化介面設計
12. PreToolUse hook 資安掃描；PostToolUse hook 自動測試；封鎖 `docker compose up`
13. 使用 `/agents` 建立背景 Agent 產生商品測試資料
14. 使用 `/agents` + worktree 分支 `feature/coupon` 開發優惠券功能
15. 使用 `/review` 檢查，`/simplify` 簡化內容

---

### Harness Engineering

<sub>為 AI Agent 提供可控、可靠、可觀測的執行環境</sub>

| 面向 | 本專案實作 |
|------|-----------|
| ① 上下文 | `CLAUDE.md` 開發規範；`MEMORY.md` 跨對話記憶；`spec.md` / `api.md` 任務知識庫 |
| ② 工具 | Skills：`agent-browser`、`skill-creator`、`security-check`、`render-security` |
| ③ 控制流 | SDD 規格先行；TodoWrite 三態任務追蹤；Wave 平行開發規劃 |
| ④ 驗證 | PreToolUse R1–R6 資安掃描；PostToolUse 單元測試；Testcontainers + Playwright E2E |
| ⑤ 狀態 | `todolist.md` 進度追蹤；git worktree 隔離支線（`feature/coupon`） |
| ⑥ 可觀測性 | 資安報告 `reports/security-check-<timestamp>.md`；JUnit / Playwright 測試報告 |
| ⑦ 安全 | `permissions.deny` 封鎖 `docker compose up`；IDOR 防護；UUID 驗證；`@Valid` + `@NotBlank` |

<sub>Loop Engineering：PostToolUse 測試失敗 → Claude 自動修正 → 重跑測試，閉迴路直到通過（最多 3 次）。設定於 CLAUDE.md。</sub>

詳細設定見 [`.claude/settings.json`](.claude/settings.json)、[`scripts/`](scripts/)

---

### 文件

| 文件 | 用途 |
|------|------|
| [prd.md](prd.md) | 產品需求 |
| [spec.md](spec.md) | 規格文件 |
| [api.md](api.md) | API 文件 |
| [todolist.md](todolist.md) | 任務追蹤 |
| [commands.md](commands.md) | 常用指令（Docker / 測試） |
| [docs/session-and-cart.md](docs/session-and-cart.md) | Session 與購物車機制說明 |

---

### 快速啟動

詳細指令請見 [commands.md](commands.md)。

```powershell
docker compose up --build   # 首次啟動
docker compose up -d        # 日常啟動
```

---

### 參考資料

- Claude Code 全端專案開發實作課程 (Kevin)
