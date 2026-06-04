package com.shoppingcart.backend.config;

import com.shoppingcart.backend.filter.SessionFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Session 過濾器組態類別。
 * 將 {@link SessionFilter} 透過 {@link FilterRegistrationBean} 註冊為 Servlet 過濾器，
 * 僅攔截 {@code /api/*} 路徑的所有請求。
 */
@Configuration
public class SessionConfig {

    /** SameSite 屬性，本地預設 Lax，跨域生產環境設 None */
    @Value("${session.cookie-same-site:Lax}")
    private String cookieSameSite;

    /**
     * 建立並設定 SessionFilter 的 FilterRegistrationBean。
     * 攔截路徑設為 {@code /api/*}，涵蓋所有購物車相關 API。
     *
     * @return 已設定好的 FilterRegistrationBean 實例
     */
    @Bean
    public FilterRegistrationBean<SessionFilter> sessionFilterRegistration() {
        // 建立 FilterRegistrationBean，包裝 SessionFilter 實例
        FilterRegistrationBean<SessionFilter> registration = new FilterRegistrationBean<>();

        /** 注入 SessionFilter 實作，傳入 SameSite 設定 */
        registration.setFilter(new SessionFilter(cookieSameSite));

        /** 攔截路徑：所有 /api/ 下的端點 */
        registration.addUrlPatterns("/api/*");

        /** 過濾器名稱（方便 log 識別） */
        registration.setName("sessionFilter");

        /** 執行順序（數字越小越優先，設為最高優先以確保 sessionId 最先被解析） */
        registration.setOrder(1);

        return registration;
    }
}
