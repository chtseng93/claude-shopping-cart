package com.shoppingcart.backend;

import com.shoppingcart.backend.dto.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品 API 整合測試（T03）。
 * 涵蓋 GET /api/products 與 GET /api/products/{id}。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductApiIntegrationTest {

    /** 共享 PostgreSQL 容器 */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    /** 固定 UUID（對應 data.sql Scandinavian Sofa） */
    private static final UUID P1 = UUID.fromString("11111111-0000-0000-0000-000000000001");

    @Test
    @DisplayName("GET /api/products → 200 OK，回傳 5 筆 seed 商品")
    void getAllProducts_returnsSeedData() {
        ResponseEntity<ProductResponse[]> resp =
                restTemplate.getForEntity("/api/products", ProductResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody()).hasSize(5);
    }

    @Test
    @DisplayName("GET /api/products/{id} → 200 OK，回傳對應商品")
    void getProductById_found() {
        ResponseEntity<ProductResponse> resp =
                restTemplate.getForEntity("/api/products/" + P1, ProductResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ProductResponse product = resp.getBody();
        assertThat(product).isNotNull();
        assertThat(product.getId()).isEqualTo(P1);
        assertThat(product.getName()).isEqualTo("Scandinavian Sofa");
    }

    @Test
    @DisplayName("GET /api/products/{id} 不存在的 ID → 404 Not Found")
    void getProductById_notFound() {
        UUID fakeId = UUID.randomUUID();
        ResponseEntity<String> resp =
                restTemplate.getForEntity("/api/products/" + fakeId, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
