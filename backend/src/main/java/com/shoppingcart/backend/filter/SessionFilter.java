package com.shoppingcart.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Session 識別過濾器。
 * 每個請求只執行一次（繼承 OncePerRequestFilter），負責從 cookie 讀取或自動產生 SESSION_ID，
 * 並將其存入 request attribute，供後續 Controller 取用。
 */
public class SessionFilter extends OncePerRequestFilter {

    /** cookie 名稱常數 */
    private static final String COOKIE_NAME = "SESSION_ID";

    /** request attribute 的 key 名稱 */
    private static final String ATTR_KEY = "sessionId";

    /** SameSite 屬性，本地用 Lax，跨域生產環境用 None */
    private final String sameSite;

    public SessionFilter(String sameSite) {
        this.sameSite = sameSite;
    }

    /**
     * 核心過濾邏輯：
     * 1. 從請求 cookie 中尋找 SESSION_ID。
     * 2. 若不存在，以 UUID 產生新值，並寫入 Set-Cookie 回應標頭。
     * 3. 將最終 sessionId 存入 request attribute，讓後續處理鏈可取用。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 從 cookie 陣列中尋找 SESSION_ID
        String sessionId = extractSessionIdFromCookies(request.getCookies());

        if (sessionId == null) {
            // 無 SESSION_ID，產生新的 UUID 並回寫給客戶端
            sessionId = UUID.randomUUID().toString();
            writeSessionCookie(response, sessionId);
        }

        // 存入 request attribute，供 Controller 透過 SessionUtils 取用
        request.setAttribute(ATTR_KEY, sessionId);

        // 繼續執行後續過濾器或 Servlet
        filterChain.doFilter(request, response);
    }

    /**
     * 從 cookie 陣列中找出名稱為 SESSION_ID 的值，並驗證其為合法 UUID 格式。
     * 格式不符時視為無效（回傳 null），伺服器將重新產生新的 SESSION_ID，
     * 防止客戶端偽造任意字串作為 session 識別碼。
     *
     * @param cookies 請求攜帶的 cookie 陣列，可能為 null
     * @return 合法 UUID 格式的 SESSION_ID 值，或 null（不存在/格式錯誤）
     */
    private String extractSessionIdFromCookies(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return isValidUuid(cookie.getValue()) ? cookie.getValue() : null;
            }
        }
        return null;
    }

    /**
     * 驗證字串是否為合法的 UUID 格式（標準 8-4-4-4-12 十六進位）。
     *
     * @param value 待驗證的字串
     * @return true 若格式合法，否則 false
     */
    private boolean isValidUuid(String value) {
        if (value == null) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 將新的 SESSION_ID 以 Set-Cookie 標頭寫入回應。
     * 屬性設定：HttpOnly（防 XSS）、SameSite=Lax（防 CSRF）、Path=/（全站有效）。
     *
     * @param response  HTTP 回應物件
     * @param sessionId 要設定的 session ID 值
     */
    private void writeSessionCookie(HttpServletResponse response, String sessionId) {
        // SameSite=None 時必須加 Secure（瀏覽器規範要求）
        String secure = "None".equalsIgnoreCase(sameSite) ? "; Secure" : "";
        String cookieValue = COOKIE_NAME + "=" + sessionId
                + "; HttpOnly; SameSite=" + sameSite + secure + "; Path=/";
        response.addHeader("Set-Cookie", cookieValue);
    }
}
