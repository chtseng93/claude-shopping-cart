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
     * 從 cookie 陣列中找出名稱為 SESSION_ID 的值；找不到則回傳 null。
     *
     * @param cookies 請求攜帶的 cookie 陣列，可能為 null
     * @return SESSION_ID 的值，或 null
     */
    private String extractSessionIdFromCookies(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 將新的 SESSION_ID 以 Set-Cookie 標頭寫入回應。
     * 屬性設定：HttpOnly（防 XSS）、SameSite=Lax（防 CSRF）、Path=/（全站有效）。
     *
     * @param response  HTTP 回應物件
     * @param sessionId 要設定的 session ID 值
     */
    private void writeSessionCookie(HttpServletResponse response, String sessionId) {
        // Jakarta Servlet 6 的 Cookie API 尚未直接支援 SameSite，改用 addHeader 手動組裝
        String cookieValue = COOKIE_NAME + "=" + sessionId
                + "; HttpOnly; SameSite=Lax; Path=/";
        response.addHeader("Set-Cookie", cookieValue);
    }
}
