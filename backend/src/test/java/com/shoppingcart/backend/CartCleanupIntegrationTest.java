package com.shoppingcart.backend;

import com.shoppingcart.backend.entity.Cart;
import com.shoppingcart.backend.entity.CartItem;
import com.shoppingcart.backend.entity.Product;
import com.shoppingcart.backend.repository.CartItemRepository;
import com.shoppingcart.backend.repository.CartRepository;
import com.shoppingcart.backend.repository.ProductRepository;
import com.shoppingcart.backend.service.CartCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 廢棄購物車清理排程整合測試。
 * 使用 Testcontainers 啟動真實 PostgreSQL，驗證：
 * - 超過 30 天的未結帳購物車（含明細）被正確刪除
 * - 30 天內的購物車不受影響
 * - 已結帳的購物車不受影響
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CartCleanupIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CartCleanupService cartCleanupService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 每個測試前清空所有購物車與明細，確保測試隔離 */
    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
    }

    @Test
    @DisplayName("超過 30 天的未結帳購物車（含明細）應被清除")
    void shouldDeleteAbandonedCartOlderThan30Days() {
        // Arrange：建立一筆 31 天前的未結帳購物車，並加入一筆明細
        Cart oldCart = cartRepository.save(new Cart("old-session"));
        // 以 JDBC 直接更新 created_at，繞過 @CreationTimestamp 的限制
        Instant oldTime = Instant.now().minus(31, ChronoUnit.DAYS);
        jdbcTemplate.update("UPDATE cart SET created_at = ? WHERE id = ?",
                Timestamp.from(oldTime), oldCart.getId());

        Product product = productRepository.save(
                new Product("測試商品", "desc", new BigDecimal("100"), 10, null));
        cartItemRepository.save(new CartItem(oldCart, product, 1, new BigDecimal("100")));

        assertThat(cartRepository.count()).isEqualTo(1);
        assertThat(cartItemRepository.count()).isEqualTo(1);

        // Act
        cartCleanupService.purgeAbandonedCarts();

        // Assert：購物車與明細都應被刪除（cascade）
        assertThat(cartRepository.count()).isZero();
        assertThat(cartItemRepository.count()).isZero();
    }

    @Test
    @DisplayName("30 天內的未結帳購物車不應被清除")
    void shouldNotDeleteRecentCart() {
        // Arrange：建立一筆 10 天前的未結帳購物車
        Cart recentCart = cartRepository.save(new Cart("recent-session"));
        Instant recentTime = Instant.now().minus(10, ChronoUnit.DAYS);
        jdbcTemplate.update("UPDATE cart SET created_at = ? WHERE id = ?",
                Timestamp.from(recentTime), recentCart.getId());

        // Act
        cartCleanupService.purgeAbandonedCarts();

        // Assert：購物車應保留
        assertThat(cartRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("已結帳的舊購物車不應被清除")
    void shouldNotDeleteCheckedOutCart() {
        // Arrange：建立一筆 31 天前、已結帳的購物車
        Cart checkedOutCart = cartRepository.save(new Cart("done-session"));
        Instant oldTime = Instant.now().minus(31, ChronoUnit.DAYS);
        jdbcTemplate.update(
                "UPDATE cart SET created_at = ?, checked_out_at = ? WHERE id = ?",
                Timestamp.from(oldTime),
                Timestamp.from(Instant.now().minus(30, ChronoUnit.DAYS)),
                checkedOutCart.getId());

        // Act
        cartCleanupService.purgeAbandonedCarts();

        // Assert：已結帳的購物車應保留（不在清理範圍內）
        assertThat(cartRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("同時存在多種購物車時，只清除符合條件的廢棄購物車")
    void shouldOnlyDeleteAbandonedCarts() {
        // Arrange
        // 廢棄：31 天前，未結帳
        Cart abandoned = cartRepository.save(new Cart("abandoned-session"));
        jdbcTemplate.update("UPDATE cart SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(31, ChronoUnit.DAYS)), abandoned.getId());

        // 保留：10 天前，未結帳
        Cart recent = cartRepository.save(new Cart("recent-session"));
        jdbcTemplate.update("UPDATE cart SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS)), recent.getId());

        // 保留：31 天前，已結帳
        Cart checkedOut = cartRepository.save(new Cart("done-session"));
        jdbcTemplate.update(
                "UPDATE cart SET created_at = ?, checked_out_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(31, ChronoUnit.DAYS)),
                Timestamp.from(Instant.now().minus(30, ChronoUnit.DAYS)),
                checkedOut.getId());

        assertThat(cartRepository.count()).isEqualTo(3);

        // Act
        cartCleanupService.purgeAbandonedCarts();

        // Assert：只有廢棄的那筆被刪除，剩餘 2 筆
        assertThat(cartRepository.count()).isEqualTo(2);
        assertThat(cartRepository.findById(recent.getId())).isPresent();
        assertThat(cartRepository.findById(checkedOut.getId())).isPresent();
        assertThat(cartRepository.findById(abandoned.getId())).isEmpty();
    }
}
