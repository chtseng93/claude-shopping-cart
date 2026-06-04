package com.shoppingcart.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域資源共享（CORS）設定。
 * 允許來源由環境變數 CORS_ALLOWED_ORIGINS 控制（逗號分隔多個來源），
 * 預設允許本地 Vite 開發伺服器。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /** 允許的前端來源，可透過環境變數設定多個（逗號分隔） */
    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    /**
     * 設定 CORS 規則，套用於所有 /api/** 路徑。
     *
     * @param registry Spring MVC CORS 設定登錄器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 允許傳遞 cookie（SESSION_ID），需搭配具體來源使用
                .allowCredentials(true);
    }
}
