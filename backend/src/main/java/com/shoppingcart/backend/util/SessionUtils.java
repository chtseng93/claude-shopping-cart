package com.shoppingcart.backend.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Session 工具類別。
 * 提供靜態方法供 Controller 層快速取得由 {@code SessionFilter} 預先解析並存入的 sessionId，
 * 避免各 Controller 重複撰寫相同的 attribute 讀取邏輯。
 */
public final class SessionUtils {

    /** request attribute 的 key 名稱，需與 SessionFilter 中的定義一致 */
    private static final String ATTR_KEY = "sessionId";

    /**
     * 工具類別不允許實例化。
     */
    private SessionUtils() {
        throw new UnsupportedOperationException("工具類別不可實例化");
    }

    /**
     * 從 {@link HttpServletRequest} 的 attribute 中取出 sessionId。
     * <p>
     * 此值由 {@code SessionFilter} 在請求進入過濾器鏈時設定，
     * 保證在 {@code /api/*} 路徑下一定存在（不會回傳 null）。
     * </p>
     *
     * @param request 當前 HTTP 請求物件
     * @return sessionId 字串（UUID 格式）
     * @throws IllegalStateException 若 sessionId 不存在於 attribute（代表過濾器未正確執行）
     */
    public static String getSessionId(HttpServletRequest request) {
        Object sessionId = request.getAttribute(ATTR_KEY);
        if (sessionId == null) {
            throw new IllegalStateException(
                    "sessionId 不存在於 request attribute，請確認 SessionFilter 已正確註冊於此路徑");
        }
        return (String) sessionId;
    }
}
