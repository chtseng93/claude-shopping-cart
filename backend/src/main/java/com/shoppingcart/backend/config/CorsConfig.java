package com.shoppingcart.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域資源共享（CORS）設定。
 * 允許前端 Vite 開發伺服器（http://localhost:5173）存取後端 API，
 * 並開放 credentials 傳遞以支援 SESSION_ID cookie。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 設定 CORS 規則，套用於所有 /api/** 路徑。
     *
     * <ul>
     *   <li>允許來源：http://localhost:5173（Vite 開發伺服器）</li>
     *   <li>允許方法：GET、POST、PATCH、DELETE、OPTIONS</li>
     *   <li>允許 headers：*（所有請求標頭）</li>
     *   <li>允許 credentials：true（需傳遞 SESSION_ID cookie）</li>
     * </ul>
     *
     * @param registry Spring MVC CORS 設定登錄器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 允許的前端來源（Vite 開發伺服器）
                .allowedOrigins("http://localhost:5173")
                // 允許的 HTTP 方法
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                // 允許所有請求標頭
                .allowedHeaders("*")
                // 允許傳遞 cookie（SESSION_ID），需搭配 allowedOrigins 使用具體來源
                .allowCredentials(true);
    }
}
