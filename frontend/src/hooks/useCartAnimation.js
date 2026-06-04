import { useCallback, useRef, useState } from 'react'

/**
 * 管理購物車徽章彈跳動畫與 Toast 提示狀態的自訂 hook。
 *
 * 提供兩組功能：
 * 1. 徽章彈跳動畫：透過暫時附加 CSS class 觸發 scale keyframe 動畫
 * 2. Toast 提示：顯示短暫訊息，2 秒後自動隱藏
 *
 * @returns {{
 *   badgeAnimating: boolean,
 *   triggerAnimation: () => void,
 *   showToast: boolean,
 *   toastMessage: string,
 *   triggerToast: (message: string) => void
 * }}
 */
function useCartAnimation() {
  /** @type {[boolean, Function]} 徽章目前是否正在執行彈跳動畫 */
  const [badgeAnimating, setBadgeAnimating] = useState(false)

  /** @type {[boolean, Function]} Toast 是否正在顯示 */
  const [showToast, setShowToast] = useState(false)

  /** @type {[string, Function]} Toast 顯示的訊息文字 */
  const [toastMessage, setToastMessage] = useState('')

  /**
   * 用於追蹤 Toast 自動隱藏的 timer，
   * 避免短時間多次觸發時 timer 相互干擾。
   * @type {React.MutableRefObject<ReturnType<typeof setTimeout>|null>}
   */
  const toastTimerRef = useRef(null)

  /**
   * 觸發徽章彈跳動畫。
   * 設定 badgeAnimating = true（附加 CSS class），
   * 300ms 後自動移除，與 CSS keyframe 動畫時長對齊。
   */
  const triggerAnimation = useCallback(() => {
    setBadgeAnimating(true)
    setTimeout(() => {
      setBadgeAnimating(false)
    }, 300)
  }, [])

  /**
   * 顯示 Toast 提示訊息，2000ms 後自動隱藏。
   * 若在隱藏前再次觸發，會重置計時器並更新訊息。
   *
   * @param {string} message - 要顯示的提示文字
   */
  const triggerToast = useCallback((message) => {
    // 清除上一個尚未執行的隱藏 timer，避免提早消失
    if (toastTimerRef.current) {
      clearTimeout(toastTimerRef.current)
    }

    setToastMessage(message)
    setShowToast(true)

    toastTimerRef.current = setTimeout(() => {
      setShowToast(false)
      toastTimerRef.current = null
    }, 2000)
  }, [])

  return {
    badgeAnimating,
    triggerAnimation,
    showToast,
    toastMessage,
    triggerToast,
  }
}

export default useCartAnimation
