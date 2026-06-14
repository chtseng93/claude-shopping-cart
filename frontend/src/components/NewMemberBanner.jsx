/**
 * NewMemberBanner — 新會員折扣碼提示橫幅元件。
 * 顯示在商品列表頁頂部，提示訪客可使用 WELCOME10 享有 10% 折扣。
 * 點擊折扣碼可自動複製至剪貼簿。
 *
 * @returns {JSX.Element} 新會員折扣碼橫幅
 */
import { useState } from 'react'
import './NewMemberBanner.css'

export default function NewMemberBanner() {
  /** 追蹤複製成功狀態，顯示短暫的「已複製」提示 */
  const [copied, setCopied] = useState(false)

  /**
   * 點擊折扣碼時，將代碼複製至剪貼簿並顯示成功提示。
   * 1.5 秒後提示自動消失。
   */
  async function handleCopyCode() {
    try {
      await navigator.clipboard.writeText('WELCOME10')
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      // 若無剪貼簿 API 權限（舊瀏覽器），靜默失敗
    }
  }

  return (
    <div className="new-member-banner" role="banner" aria-label="New member offer">
      <div className="new-member-banner__content">
        {/* 禮物圖示（SVG） */}
        <svg
          className="new-member-banner__icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <polyline points="20 12 20 22 4 22 4 12" />
          <rect x="2" y="7" width="20" height="5" />
          <line x1="12" y1="22" x2="12" y2="7" />
          <path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z" />
          <path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z" />
        </svg>

        <span className="new-member-banner__text">
          New Member Exclusive: 10% off sitewide
        </span>

        {/* 折扣碼顯示，點擊可複製 */}
        <button
          className={`new-member-banner__code${copied ? ' new-member-banner__code--copied' : ''}`}
          onClick={handleCopyCode}
          aria-label={copied ? 'Coupon code copied' : 'Click to copy coupon code WELCOME10'}
          title="Click to copy"
        >
          {copied ? 'Copied!' : 'WELCOME10'}
        </button>
      </div>
    </div>
  )
}
