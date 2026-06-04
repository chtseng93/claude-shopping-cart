import './Toast.css'

/**
 * 浮動提示訊息元件（Toast）。
 *
 * 定位於視窗右下角，show=true 時從右下角滑入，
 * show 切換為 false 時淡出消失。
 * 動畫效果純粹透過 CSS transition 實現（無 JS 動畫）。
 *
 * @param {{ message: string, show: boolean }} props
 * @param {string}  props.message - 要顯示的提示文字
 * @param {boolean} props.show    - true 時顯示，false 時隱藏（淡出）
 */
function Toast({ message, show }) {
  return (
    <div
      className={`toast ${show ? 'toast--show' : 'toast--hide'}`}
      role="status"
      aria-live="polite"
      aria-atomic="true"
    >
      {message}
    </div>
  )
}

export default Toast
