import { defineConfig, devices } from '@playwright/test'

/**
 * Demo 錄影專用設定（demo-video2 用）。
 * 與預設 config 差異：
 * - 只跑 demo-video2.spec.ts
 * - video 永遠錄製（mode: 'on'）
 * - 視窗設為 1440×900（標準桌面比例）
 * - headless 錄製，畫面乾淨
 */
export default defineConfig({
  testDir: './tests',
  testMatch: '**/demo-video2.spec.ts',
  timeout: 90_000,
  fullyParallel: false,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:5173',
    headless: true,
    viewport: { width: 1440, height: 900 },
    video: {
      mode: 'on',
      size: { width: 1440, height: 900 },
    },
  },
  projects: [
    {
      name: 'chromium',
      use: {
        browserName: 'chromium',
        viewport: { width: 1440, height: 900 },
      },
    },
  ],
})
