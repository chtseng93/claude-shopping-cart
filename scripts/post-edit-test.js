#!/usr/bin/env node
/**
 * post-edit-test.js
 * PostToolUse hook：偵測後端 Java 檔案被編輯後，自動執行單元測試。
 *
 * 觸發條件：Edit 或 Write 工具修改的檔案路徑包含 backend 且副檔名為 .java
 * 跳過條件：非 Java 後端檔案（直接 exit 0 放行）
 *
 * 執行內容：
 *   mvn compile       — 快速編譯檢查（捕捉語法錯誤）
 *   mvn test          — 執行所有測試（排除需 Docker 的整合測試）
 *
 * 排除整合測試原因：IntegrationTest 依賴 Testcontainers（需 Docker 啟動容器），
 * 每次編輯後觸發會增加約 20–30 秒等待，改為只跑不需 Docker 的單元測試。
 */

'use strict';

const { execSync } = require('child_process');
const path = require('path');

let input = '';
process.stdin.on('data', d => input += d);
process.stdin.on('end', () => {
    let data;
    try {
        data = JSON.parse(input);
    } catch {
        process.exit(0);
    }

    const toolName  = data.tool_name  || '';
    const filePath  = (data.tool_input && data.tool_input.file_path) || '';

    // 只在 Edit / Write 後觸發
    if (!['Edit', 'Write'].includes(toolName)) process.exit(0);

    // 只針對後端 Java 檔案
    const normalizedPath = filePath.replace(/\\/g, '/');
    if (!normalizedPath.includes('/backend/') || !filePath.endsWith('.java')) process.exit(0);

    console.log(`\n[post-edit] 偵測到後端 Java 檔案變更：${path.basename(filePath)}`);
    console.log('[post-edit] 執行單元測試（排除整合測試）...\n');

    const projectRoot = path.resolve(__dirname, '..');
    const pomPath = path.join(projectRoot, 'backend', 'pom.xml');

    try {
        execSync(
            `mvn -f "${pomPath}" test -Dexcludes="**/*IntegrationTest.java" -q`,
            { stdio: 'inherit', cwd: projectRoot }
        );
        console.log('\n[post-edit] ✅ 測試通過');
    } catch {
        console.error('\n[post-edit] ❌ 測試失敗，請修正後再繼續');
        process.exit(1);
    }
});
