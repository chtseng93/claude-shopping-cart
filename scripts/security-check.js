#!/usr/bin/env node
/**
 * security-check.js
 * PreToolUse hook：在 gh pr create 執行前掃描本專案資安規則。
 *
 * 執行時機：
 *   - Claude Code PreToolUse hook（stdin 傳入工具輸入 JSON）
 *   - git pre-push hook（.git/hooks/pre-push）
 *   觸發條件：指令含 "git push" 或 "gh pr create"；其他指令直接放行（exit 0）。
 *
 * 報告輸出：
 *   每次掃描自動產生 reports/security-check-<YYYY-MM-DDTHH-MM-SS>.md，
 *   報告內含當下 git branch、commit hash 與 commit message，
 *   可追溯每份報告對應的程式碼版本。
 *
 * 掃描規則（針對 Spring Boot + React 架構）：
 *   R1 — 禁止硬編碼 API 金鑰 / 密碼（application.yml、.env*）
 *   R2 — DTO 不可含金額欄位（price / total / unitPrice / subtotal / amount）
 *   R3 — CartController.checkout 必須套用 @Valid CheckoutRequest
 *   R4 — CartService.updateItem / removeItem 必須包含 sessionId 參數（IDOR 防護）
 *   R5 — RecipientDto.phone / email 必須同時有 @NotBlank（防 null 值繞過驗證）
 *   R6 — SessionFilter 必須驗證 SESSION_ID UUID 格式
 */

'use strict';

const fs     = require('fs');
const path   = require('path');
const { execSync } = require('child_process');

// ── 專案根目錄（scripts/ 的上一層）──────────────────────────────────────────
const PROJECT_ROOT = path.resolve(__dirname, '..');

// ── 工具：讀取檔案，不存在回傳 null ─────────────────────────────────────────
function readFile(relPath) {
  const abs = path.join(PROJECT_ROOT, relPath);
  if (!fs.existsSync(abs)) return null;
  return fs.readFileSync(abs, 'utf-8');
}

// ── 掃描規則定義 ─────────────────────────────────────────────────────────────
const RULES = [

  {
    id: 'R1',
    name: '硬編碼 secrets',
    files: [
      'backend/src/main/resources/application.yml',
      'backend/src/main/resources/application.properties',
    ],
    check(content, file) {
      const hits = [];
      // 逐行解析，避免 ${VAR:default} 語法中的內容被誤判
      for (const line of content.split('\n')) {
        const trimmed = line.trim();
        if (trimmed.startsWith('#')) continue; // 略過註解行

        // password: <值>（不是 ${...} 佔位符）
        const pwMatch = trimmed.match(/^password\s*:\s*(.+)$/i);
        if (pwMatch) {
          const val = pwMatch[1].trim();
          if (!val.startsWith('${') && !val.startsWith('#') && val.length >= 3) {
            hits.push(`${file} 含硬編碼密碼（應改用環境變數 \${...}）`);
          }
        }

        // api-key / api_key = <長字串>
        const keyMatch = trimmed.match(/^api[_-]?key\s*[:=]\s*["']?([A-Za-z0-9_\-]{16,})["']?/i);
        if (keyMatch) {
          hits.push(`${file} 含硬編碼 API Key`);
        }
      }
      return hits;
    },
  },

  {
    id: 'R2',
    name: 'DTO 金額欄位防護',
    files: [
      'backend/src/main/java/com/shoppingcart/backend/dto/AddItemRequest.java',
      'backend/src/main/java/com/shoppingcart/backend/dto/CheckoutRequest.java',
      'backend/src/main/java/com/shoppingcart/backend/dto/UpdateItemRequest.java',
    ],
    check(content, file) {
      const hits = [];
      // 禁止在 DTO 中出現金額相關 private 欄位
      const priceFields = /(price|total|totalAmount|unitPrice|subtotal|amount)\s*;/;
      if (priceFields.test(content)) {
        hits.push(`${file} 含金額欄位（客戶端可能傳入偽造金額）`);
      }
      return hits;
    },
  },

  {
    id: 'R3',
    name: '結帳收件資料驗證',
    files: [
      'backend/src/main/java/com/shoppingcart/backend/controller/CartController.java',
    ],
    check(content, file) {
      const hits = [];
      // checkout 方法必須同時含 @Valid 與 CheckoutRequest
      const hasCheckout     = /\bcheckout\b/.test(content);
      const hasValidRequest = /@Valid\s+CheckoutRequest/.test(content);
      if (hasCheckout && !hasValidRequest) {
        hits.push(`${file} checkout 端點缺少 @Valid CheckoutRequest（收件資料未驗證）`);
      }
      return hits;
    },
  },

  {
    id: 'R4',
    name: 'IDOR 防護（updateItem / removeItem）',
    files: [
      'backend/src/main/java/com/shoppingcart/backend/service/CartService.java',
    ],
    check(content, file) {
      const hits = [];
      // updateItem(UUID itemId, int quantity, String sessionId) — 需含 sessionId
      const updateSig = content.match(/public\s+CartResponse\s+updateItem\s*\(([^)]*)\)/);
      if (updateSig && !updateSig[1].includes('sessionId')) {
        hits.push(`${file} updateItem 缺少 sessionId 參數（任何人可修改他人購物車）`);
      }
      // removeItem(UUID itemId, String sessionId) — 需含 sessionId
      const removeSig = content.match(/public\s+CartResponse\s+removeItem\s*\(([^)]*)\)/);
      if (removeSig && !removeSig[1].includes('sessionId')) {
        hits.push(`${file} removeItem 缺少 sessionId 參數（任何人可刪除他人明細）`);
      }
      return hits;
    },
  },

  {
    id: 'R5',
    name: 'RecipientDto 必填驗證（@NotBlank）',
    files: [
      'backend/src/main/java/com/shoppingcart/backend/dto/RecipientDto.java',
    ],
    check(content, file) {
      const hits = [];
      // 取出 phone 欄位上方的 annotation 區塊（含換行）
      const phoneBlock = content.match(/((?:@\w+[^;]*\n\s*)+private\s+String\s+phone\s*;)/s);
      if (phoneBlock && !phoneBlock[0].includes('@NotBlank')) {
        hits.push(`${file} phone 缺少 @NotBlank（null 值可繞過 @Pattern 驗證）`);
      }
      // 取出 email 欄位上方的 annotation 區塊
      const emailBlock = content.match(/((?:@\w+[^;]*\n\s*)+private\s+String\s+email\s*;)/s);
      if (emailBlock && !emailBlock[0].includes('@NotBlank')) {
        hits.push(`${file} email 缺少 @NotBlank（null 值可繞過 @Email 驗證）`);
      }
      return hits;
    },
  },

  {
    id: 'R6',
    name: 'SESSION_ID UUID 格式驗證',
    files: [
      'backend/src/main/java/com/shoppingcart/backend/filter/SessionFilter.java',
    ],
    check(content, file) {
      const hits = [];
      // 需有 UUID.fromString 或 isValidUuid 方法
      const hasUuidValidation =
        content.includes('UUID.fromString') || content.includes('isValidUuid');
      if (!hasUuidValidation) {
        hits.push(`${file} 未驗證 SESSION_ID UUID 格式（客戶端可偽造任意字串）`);
      }
      return hits;
    },
  },

];

// ── 主流程 ────────────────────────────────────────────────────────────────────
function main(toolInput) {
  // 相容兩種 stdin 結構：直接 {command} 或包在 {tool_input: {command}} 內
  const command = toolInput.command
    || (toolInput.tool_input && toolInput.tool_input.command)
    || '';

  // 僅在 git push 或 gh pr create 時觸發掃描
  const isPush     = /\bgit\s+push\b/.test(command);
  const isPrCreate = command.includes('gh pr create');
  if (!isPush && !isPrCreate) {
    process.exit(0);
  }

  // ── 執行掃描 ───────────────────────────────────────────────────────────────
  const issues = [];

  for (const rule of RULES) {
    for (const relPath of rule.files) {
      const content = readFile(relPath);
      if (content === null) continue; // 檔案不存在則略過

      const hits = rule.check(content, relPath);
      if (hits.length > 0) {
        issues.push({ rule: rule.id, name: rule.name, hits });
      }
    }
  }

  // ── 產生報告（不影響執行結果） ────────────────────────────────────────────
  const passed   = issues.length === 0;
  const now      = new Date();
  const trigger  = isPrCreate ? 'gh pr create' : 'git push';
  const lines    = [];

  // 取得當前 git commit 資訊
  let gitHash = 'unknown', gitMsg = 'unknown', gitBranch = 'unknown';
  try {
    gitHash   = execSync('git log -1 --format=%H',            { cwd: PROJECT_ROOT }).toString().trim();
    gitMsg    = execSync('git log -1 --format=%s',            { cwd: PROJECT_ROOT }).toString().trim();
    gitBranch = execSync('git rev-parse --abbrev-ref HEAD',   { cwd: PROJECT_ROOT }).toString().trim();
  } catch (_) { /* git 指令失敗時略過 */ }

  lines.push(`# Security Check Report`);
  lines.push(`\n**時間**：${now.toISOString()}`);
  lines.push(`**觸發指令**：\`${trigger}\``);
  lines.push(`**結果**：${passed ? '✅ 通過' : '🔴 失敗'}`);
  lines.push(`\n## Commit 資訊\n`);
  lines.push(`| 項目 | 值 |`);
  lines.push(`|------|-----|`);
  lines.push(`| Branch | \`${gitBranch}\` |`);
  lines.push(`| Commit | \`${gitHash}\` |`);
  lines.push(`| Message | ${gitMsg} |\n`);
  lines.push(`---\n`);

  if (passed) {
    lines.push(`## ✅ 所有規則通過（R1–R6）\n`);
    lines.push(`| 規則 | 說明 | 狀態 |`);
    lines.push(`|------|------|------|`);
    for (const r of RULES) {
      lines.push(`| ${r.id} | ${r.name} | ✅ |`);
    }
  } else {
    lines.push(`## 🔴 發現問題\n`);
    for (const { rule, name, hits } of issues) {
      lines.push(`### [${rule}] ${name}\n`);
      for (const h of hits) {
        lines.push(`- ✗ ${h}`);
      }
      lines.push('');
    }
    lines.push(`---\n`);
    lines.push(`## ✅ 通過規則\n`);
    const failedIds = new Set(issues.map(i => i.rule));
    for (const r of RULES) {
      if (!failedIds.has(r.id)) {
        lines.push(`- [${r.id}] ${r.name}`);
      }
    }
  }

  // 檔名以日期時間命名，格式：security-check-YYYY-MM-DDTHH-MM-SS.md
  const ts         = now.toISOString().replace(/:/g, '-').replace(/\.\d{3}Z$/, '');
  const reportDir  = path.join(PROJECT_ROOT, 'reports');
  const reportFile = `security-check-${ts}.md`;
  const reportPath = path.join(reportDir, reportFile);
  try {
    if (!fs.existsSync(reportDir)) fs.mkdirSync(reportDir, { recursive: true });
    fs.writeFileSync(reportPath, lines.join('\n'), 'utf-8');
  } catch (_) {
    // 報告寫入失敗不影響主流程
  }

  // ── 輸出至終端並決定是否阻斷 ──────────────────────────────────────────────
  if (!passed) {
    console.error('\n╔══════════════════════════════════════════════════════╗');
    console.error('║  🔴 資安掃描失敗 — 操作已中止                        ║');
    console.error('╚══════════════════════════════════════════════════════╝\n');
    for (const { rule, name, hits } of issues) {
      console.error(`[${rule}] ${name}`);
      for (const h of hits) {
        console.error(`  ✗ ${h}`);
      }
    }
    console.error(`\n報告已儲存至 reports/${reportFile}`);
    console.error(`請修正上述問題後重新執行 ${trigger}\n`);
    process.exit(1);
  }

  console.log(`✅ 資安掃描通過（R1–R6 全部合規），允許繼續`);
  console.log(`報告已儲存至 reports/${reportFile}`);
  process.exit(0);
}

// ── 讀取 stdin ────────────────────────────────────────────────────────────────
let raw = '';
process.stdin.setEncoding('utf-8');
process.stdin.on('data', chunk => { raw += chunk; });
process.stdin.on('end', () => {
  try {
    const parsed = JSON.parse(raw);
    main(parsed);
  } catch (_) {
    // stdin 非 JSON（不預期情況），放行不阻斷
    process.exit(0);
  }
});
