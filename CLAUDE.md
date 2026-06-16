# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案概述

**shopping-cart** — 購物車系統。

## 開發環境

- **平台**：Windows 11，Shell 使用 PowerShell 7+
- **語言**：以繁體中文撰寫所有文件、註解與溝通

## 技術棧

- **後端**：Java 17 + Spring Boot 3.x + PostgreSQL 18（Docker 啟動）
- **前端**：React + Vite
- **建置工具**：後端使用 Maven；前端套件管理器使用 npm
- **資料存取**：Spring Data JPA（Hibernate）
- **Docker**：操作使用 `docker compose`（非 `docker-compose`）

### Java 路徑

- Java 17 目錄：`C:\Program Files\Java\openjdk-17.0.12`（Spring Boot 3 需 Java 17+）

## 文件規範

- 一個專案若分不同領域（前端、後端），CLAUDE.md 需在前後端目錄下分別撰寫
- 任何修改前都需要先更新文件（spec.md、api.md）
- 撰寫程式前必須充分理解規格文件內容，並將理解內容與開發者確認
- 若有後端程式需先規劃 API 文件（api.md），RESTful 風格

規格文件須包含以下內容，流程圖一律使用 mermaid 製作：

1. 架構與選型
2. 資料模型
3. 關鍵流程
4. 虛擬碼
5. 系統脈絡圖
6. 容器/部署概觀
7. 模組關係圖（Backend / Frontend）
8. 序列圖
9. ER 圖
10. 類別圖

## 程式規範

- 程式碼需有函式級別註解（註解使用中文），重要變數或物件也需加上註解
- 單一任務原則，勿過度開發

## 任務管理

- 進行開發前須先進行任務拆分，任務都能獨立開發互不干擾，並將任務寫入 todolist
- 進行任務、完成任務都需要修改 todolist
- 新任務開始前都需先確認 todolist

## 測試

- 任務完成前都須完成測試，測試完畢才能繼續下一任務
- PostToolUse hook 執行測試失敗時，**不需詢問使用者**，直接分析錯誤訊息、修正程式碼並重新觸發測試，持續循環直到測試通過（最多重試 3 次，超過則回報錯誤請使用者介入）

## 任務啟動協議（強制）

當開啟新任務或觸發任何技能時，必須先讀取並執行 auto-skill 技能的 SKILL.md。
