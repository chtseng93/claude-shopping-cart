package com.shoppingcart.backend;

import com.shoppingcart.backend.dto.AddItemRequest;
import com.shoppingcart.backend.dto.CartResponse;
import com.shoppingcart.backend.dto.CheckoutRequest;
import com.shoppingcart.backend.dto.ItemDto;
import com.shoppingcart.backend.dto.OrderSummary;
import com.shoppingcart.backend.dto.RecipientDto;
import com.shoppingcart.backend.dto.UpdateItemRequest;
import com.shoppingcart.backend.entity.Product;
import com.shoppingcart.backend.repository.CartItemRepository;
import com.shoppingcart.backend.repository.CartRepository;
import com.shoppingcart.backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 購物車 API 整合測試（T15）。
 * 使用 Testcontainers 啟動真實 PostgreSQL 容器，涵蓋：
 * - 空購物車查詢
 * - 加入商品（含自動合併相同商品）
 * - 更新數量（0 時自動移除）
 * - 直接刪除明細
 * - 合計計算正確性
 * - 結帳成功（庫存扣減、購物車清空）
 * - 庫存不足結帳（409 Conflict）
 * - 空購物車結帳（400 Bad Request）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CartApiIntegrationTest {

    /** 共享 PostgreSQL 容器（所有測試方法重用同一個，節省啟動時間） */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    /**
     * 動態覆寫 datasource 設定，指向 Testcontainers 啟動的臨時 PostgreSQL。
     * ddl-auto 改為 none，讓 schema.sql 負責建立資料表。
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    // ── 固定 UUID（對應 data.sql seed 資料） ─────────────────────────────────

    /** Scandinavian Sofa，price=899，stock=15 */
    private static final UUID P1 = UUID.fromString("11111111-0000-0000-0000-000000000001");

    /** Accent Armchair，price=1200，stock=8 */
    private static final UUID P2 = UUID.fromString("11111111-0000-0000-0000-000000000002");

    /** Open Bookshelf，price=590，stock=3（庫存稀少，用於庫存不足測試） */
    private static final UUID P4 = UUID.fromString("11111111-0000-0000-0000-000000000004");

    /** Arc Floor Lamp，price=780，stock=0（已售完） */
    private static final UUID P5 = UUID.fromString("11111111-0000-0000-0000-000000000005");

    /** 有效的收件人資料（結帳用） */
    private static final RecipientDto VALID_RECIPIENT =
            new RecipientDto("測試收件人", "0912345678", "test@example.com", "台北市中正區測試路 1 號");

    /**
     * 每個測試前清除所有購物車與明細，並重置 P4 的庫存回 3（防止測試間互相污染）。
     */
    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();

        // 重置稀少庫存商品的庫存數量
        productRepository.findById(P4).ifPresent(p -> {
            p.setStock(3);
            productRepository.save(p);
        });
        // 重置售完商品確保 stock 仍為 0（防止意外被修改）
        productRepository.findById(P5).ifPresent(p -> {
            p.setStock(0);
            productRepository.save(p);
        });
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────────

    /**
     * 從回應 Header 中提取 SESSION_ID cookie 值。
     *
     * @param response 任意 HTTP 回應
     * @return SESSION_ID 值，若不存在則為 null
     */
    private String extractSessionId(ResponseEntity<?> response) {
        List<String> setCookieHeaders = response.getHeaders().get("Set-Cookie");
        if (setCookieHeaders == null) return null;
        for (String header : setCookieHeaders) {
            if (header.startsWith("SESSION_ID=")) {
                return header.split(";")[0].substring("SESSION_ID=".length());
            }
        }
        return null;
    }

    /**
     * 建立帶有 SESSION_ID cookie 的 HttpHeaders（Content-Type: application/json）。
     *
     * @param sessionId 測試的 session 識別碼
     * @return 已設定 Cookie 與 Content-Type 的 HttpHeaders
     */
    private HttpHeaders headersWithSession(String sessionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (sessionId != null) {
            headers.add("Cookie", "SESSION_ID=" + sessionId);
        }
        return headers;
    }

    /**
     * 取得該 session 的購物車（GET /api/cart），並返回回應物件。
     *
     * @param sessionId session 識別碼
     * @return CartResponse 的 ResponseEntity
     */
    private ResponseEntity<CartResponse> getCart(String sessionId) {
        return restTemplate.exchange(
                "/api/cart",
                HttpMethod.GET,
                new HttpEntity<>(headersWithSession(sessionId)),
                CartResponse.class
        );
    }

    /**
     * 加入商品至購物車（POST /api/cart/items）。
     *
     * @param sessionId session 識別碼
     * @param productId 商品 UUID
     * @param quantity  加入數量
     * @return CartResponse 的 ResponseEntity
     */
    private ResponseEntity<CartResponse> addItem(String sessionId, UUID productId, int quantity) {
        AddItemRequest body = new AddItemRequest(productId, quantity);
        return restTemplate.exchange(
                "/api/cart/items",
                HttpMethod.POST,
                new HttpEntity<>(body, headersWithSession(sessionId)),
                CartResponse.class
        );
    }

    /**
     * 更新購物車明細數量（PATCH /api/cart/items/{itemId}）。
     *
     * @param sessionId session 識別碼
     * @param itemId    明細 UUID
     * @param quantity  新數量
     * @return CartResponse 的 ResponseEntity
     */
    private ResponseEntity<CartResponse> updateItem(String sessionId, UUID itemId, int quantity) {
        UpdateItemRequest body = new UpdateItemRequest();
        body.setQuantity(quantity);
        return restTemplate.exchange(
                "/api/cart/items/" + itemId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, headersWithSession(sessionId)),
                CartResponse.class
        );
    }

    /**
     * 刪除購物車明細（DELETE /api/cart/items/{itemId}）。
     *
     * @param sessionId session 識別碼
     * @param itemId    明細 UUID
     * @return CartResponse 的 ResponseEntity
     */
    private ResponseEntity<CartResponse> deleteItem(String sessionId, UUID itemId) {
        return restTemplate.exchange(
                "/api/cart/items/" + itemId,
                HttpMethod.DELETE,
                new HttpEntity<>(headersWithSession(sessionId)),
                CartResponse.class
        );
    }

    /**
     * 結帳（POST /api/cart/checkout）。
     *
     * @param sessionId session 識別碼
     * @param recipient 收件人資料
     * @return OrderSummary 的 ResponseEntity
     */
    private ResponseEntity<OrderSummary> checkout(String sessionId, RecipientDto recipient) {
        CheckoutRequest body = new CheckoutRequest(recipient, null);
        return restTemplate.exchange(
                "/api/cart/checkout",
                HttpMethod.POST,
                new HttpEntity<>(body, headersWithSession(sessionId)),
                OrderSummary.class
        );
    }

    // ── 測試案例 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("查詢空購物車 → 200 OK，items 為空清單，total 為 0")
    void getCart_empty() {
        // 第一次呼叫無 cookie，Server 會自動建立 SESSION_ID 並回傳
        ResponseEntity<CartResponse> firstResp = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)),
                CartResponse.class
        );
        String sessionId = extractSessionId(firstResp);

        ResponseEntity<CartResponse> resp = getCart(sessionId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getItems()).isEmpty();
        assertThat(resp.getBody().getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getBody().getTotalQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("加入商品 → 201 Created，購物車含該明細，合計正確")
    void addItem_success() {
        // 取得 session
        ResponseEntity<CartResponse> initResp = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionId = extractSessionId(initResp);

        // 加入 P1（price=899）x 2
        ResponseEntity<CartResponse> resp = addItem(sessionId, P1, 2);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CartResponse cart = resp.getBody();
        assertThat(cart).isNotNull();
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        // subtotal = 899 * 2 = 1798
        assertThat(cart.getTotal()).isEqualByComparingTo(new BigDecimal("1798.00"));
        assertThat(cart.getTotalQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("加入相同商品兩次 → 自動合併數量，明細僅一筆")
    void addItem_mergeSameProduct() {
        ResponseEntity<CartResponse> initResp = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionId = extractSessionId(initResp);

        addItem(sessionId, P1, 1); // 第一次加入 1 個
        ResponseEntity<CartResponse> resp = addItem(sessionId, P1, 3); // 再加入 3 個

        CartResponse cart = resp.getBody();
        assertThat(cart).isNotNull();
        // 相同商品合併：明細仍為 1 筆，但數量為 4
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(4);
        // total = 899 * 4 = 3596
        assertThat(cart.getTotal()).isEqualByComparingTo(new BigDecimal("3596.00"));
        assertThat(cart.getTotalQuantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("加入多種商品 → 合計為各明細小計加總")
    void addItem_multipleProducts_totalCorrect() {
        ResponseEntity<CartResponse> initResp = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionId = extractSessionId(initResp);

        // P1=899×2 + P2=1200×1 = 1798 + 1200 = 2998
        addItem(sessionId, P1, 2);
        ResponseEntity<CartResponse> resp = addItem(sessionId, P2, 1);

        CartResponse cart = resp.getBody();
        assertThat(cart).isNotNull();
        assertThat(cart.getItems()).hasSize(2);
        assertThat(cart.getTotal()).isEqualByComparingTo(new BigDecimal("2998.00"));
        assertThat(cart.getTotalQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("PATCH 更新數量 → 200 OK，明細數量改為指定值")
    void updateItem_changeQuantity() {
        ResponseEntity<CartResponse> initResp = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionId = extractSessionId(initResp);

        // 加入 P1 x 3
        ResponseEntity<CartResponse> addResp = addItem(sessionId, P1, 3);
        UUID itemId = addResp.getBody().getItems().get(0).getItemId();

        // 更新為 5
        ResponseEntity<CartResponse> resp = updateItem(sessionId, itemId, 5);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        CartResponse cart = resp.getBody();
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        // total = 899 * 5 = 4495
        assertThat(cart.getTotal()).isEqualByComparingTo(new BigDecimal("4495.00"));
    }

    @Test
    @DisplayName("PATCH 數量設為 0 → 自動移除明細，購物車變空")
    void updateItem_quantityZero_removesItem() {
        ResponseEntity<CartResponse> initResp = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionId = extractSessionId(initResp);

        ResponseEntity<CartResponse> addResp = addItem(sessionId, P1, 2);
        UUID itemId = addResp.getBody().getItems().get(0).getItemId();

        // 數量設為 0 → 應自動移除
        ResponseEntity<CartResponse> resp = updateItem(sessionId, itemId, 0);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getItems()).isEmpty();
        assertThat(resp.getBody().getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("DELETE 直接刪除明細 → 200 OK，購物車移除該筆")
    void removeItem_success() {
        ResponseEntity<CartResponse> initResp = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionId = extractSessionId(initResp);

        // 加入兩種商品
        addItem(sessionId, P1, 1);
        ResponseEntity<CartResponse> addResp = addItem(sessionId, P2, 1);
        UUID p2ItemId = addResp.getBody().getItems().stream()
                .filter(i -> i.getProductId().equals(P2))
                .findFirst().orElseThrow().getItemId();

        // 刪除 P2 明細
        ResponseEntity<CartResponse> resp = deleteItem(sessionId, p2ItemId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        CartResponse cart = resp.getBody();
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getProductId()).isEqualTo(P1);
    }

    @Test
    @DisplayName("結帳成功 → 200 OK，訂單摘要正確，庫存扣減，購物車清空")
    void checkout_success() {
        ResponseEntity<CartResponse> initResp = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionId = extractSessionId(initResp);

        // 加入 P4（Open Bookshelf，stock=3）x 2
        addItem(sessionId, P4, 2);

        ResponseEntity<OrderSummary> resp = checkout(sessionId, VALID_RECIPIENT);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderSummary summary = resp.getBody();
        assertThat(summary).isNotNull();
        assertThat(summary.getRecipient().getName()).isEqualTo("測試收件人");
        assertThat(summary.getItems()).hasSize(1);
        // total = 590 * 2 = 1180
        assertThat(summary.getTotal()).isEqualByComparingTo(new BigDecimal("1180.00"));

        // 確認庫存已扣減：3 - 2 = 1
        Product p4 = productRepository.findById(P4).orElseThrow();
        assertThat(p4.getStock()).isEqualTo(1);

        // 確認結帳後購物車已清空（再次查詢應取得空購物車）
        ResponseEntity<CartResponse> cartAfter = getCart(sessionId);
        assertThat(cartAfter.getBody().getItems()).isEmpty();
    }

    @Test
    @DisplayName("庫存不足結帳 → 400 Bad Request，庫存不扣減（交易 rollback）")
    void checkout_insufficientStock_returns400() {
        ResponseEntity<CartResponse> initResp = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionId = extractSessionId(initResp);

        // P4（Open Bookshelf）stock=3，加入 10 個（超出庫存）
        addItem(sessionId, P4, 10);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/cart/checkout",
                HttpMethod.POST,
                new HttpEntity<>(new CheckoutRequest(VALID_RECIPIENT, null), headersWithSession(sessionId)),
                String.class
        );

        // InsufficientStockException 由 GlobalExceptionHandler 對應為 400
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // 確認庫存未被扣減（rollback 成功）
        Product p4 = productRepository.findById(P4).orElseThrow();
        assertThat(p4.getStock()).isEqualTo(3);
    }

    @Test
    @DisplayName("空購物車結帳 → 400 Bad Request")
    void checkout_emptyCart_returns400() {
        ResponseEntity<CartResponse> initResp = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionId = extractSessionId(initResp);

        // 不加任何商品直接結帳
        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/cart/checkout",
                HttpMethod.POST,
                new HttpEntity<>(new CheckoutRequest(VALID_RECIPIENT, null), headersWithSession(sessionId)),
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("IDOR 防護：使用其他 session 嘗試 PATCH 明細 → 404 Not Found")
    void updateItem_otherSessionId_returns404() {
        // Session A 建立購物車並加入商品
        ResponseEntity<CartResponse> initA = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionA = extractSessionId(initA);
        ResponseEntity<CartResponse> addResp = addItem(sessionA, P1, 1);
        UUID itemId = addResp.getBody().getItems().get(0).getItemId();

        // Session B（不同 session）嘗試更新 Session A 的明細
        ResponseEntity<CartResponse> initB = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionB = extractSessionId(initB);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/cart/items/" + itemId,
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateItemRequest(5), headersWithSession(sessionB)),
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("IDOR 防護：使用其他 session 嘗試 DELETE 明細 → 404 Not Found")
    void removeItem_otherSessionId_returns404() {
        // Session A 建立購物車並加入商品
        ResponseEntity<CartResponse> initA = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionA = extractSessionId(initA);
        ResponseEntity<CartResponse> addResp = addItem(sessionA, P1, 1);
        UUID itemId = addResp.getBody().getItems().get(0).getItemId();

        // Session B 嘗試刪除 Session A 的明細
        ResponseEntity<CartResponse> initB = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionB = extractSessionId(initB);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/cart/items/" + itemId,
                HttpMethod.DELETE,
                new HttpEntity<>(headersWithSession(sessionB)),
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("加入不存在的商品 → 404 Not Found")
    void addItem_nonExistentProduct_returns404() {
        ResponseEntity<CartResponse> initResp = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headersWithSession(null)), CartResponse.class);
        String sessionId = extractSessionId(initResp);

        UUID fakeId = UUID.randomUUID();
        AddItemRequest body = new AddItemRequest(fakeId, 1);
        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/cart/items",
                HttpMethod.POST,
                new HttpEntity<>(body, headersWithSession(sessionId)),
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
