package com.shoppingcart.backend;

import com.shoppingcart.backend.dto.AddItemRequest;
import com.shoppingcart.backend.dto.CartResponse;
import com.shoppingcart.backend.dto.CheckoutRequest;
import com.shoppingcart.backend.dto.CouponValidateRequest;
import com.shoppingcart.backend.dto.CouponValidateResponse;
import com.shoppingcart.backend.dto.ErrorResponse;
import com.shoppingcart.backend.dto.OrderSummary;
import com.shoppingcart.backend.dto.RecipientDto;
import com.shoppingcart.backend.entity.Coupon;
import com.shoppingcart.backend.entity.DiscountType;
import com.shoppingcart.backend.repository.CartItemRepository;
import com.shoppingcart.backend.repository.CartRepository;
import com.shoppingcart.backend.repository.CouponRepository;
import com.shoppingcart.backend.repository.CouponUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 優惠券 API 整合測試。
 * 使用 Testcontainers 啟動真實 PostgreSQL 容器，涵蓋：
 * - 優惠券驗證試算（有效、無效、過期、未達門檻、超過次數）
 * - 可用優惠券清單查詢
 * - 結帳時套用優惠券（折扣計算、消耗次數）
 * - 無優惠券結帳（向後相容）
 * - 後台 CRUD 操作
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CouponApiIntegrationTest {

    /** 共享 PostgreSQL 容器（所有測試方法重用同一個，節省啟動時間） */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    /**
     * 動態覆寫 datasource 設定，指向 Testcontainers 啟動的臨時 PostgreSQL。
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
    private CouponRepository couponRepository;

    @Autowired
    private CouponUsageRepository couponUsageRepository;

    // ── 固定 UUID（對應 data.sql seed 資料） ─────────────────────────────────

    /** Scandinavian Sofa，price=899，stock=15 */
    private static final UUID P1 = UUID.fromString("11111111-0000-0000-0000-000000000001");

    /** 新會員優惠 10% 折扣，seed code = WELCOME10 */
    private static final UUID C1 = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

    /** 滿千折五百，seed code = SAVE500 */
    private static final UUID C2 = UUID.fromString("cccccccc-0000-0000-0000-000000000002");

    /** 有效的收件人資料（結帳用） */
    private static final RecipientDto VALID_RECIPIENT =
            new RecipientDto("測試收件人", "0912345678", "test@example.com", "台北市中正區測試路 1 號");

    /**
     * 每個測試前清除購物車資料與優惠券使用記錄，重置 seed 優惠券狀態。
     */
    @BeforeEach
    void setUp() {
        // 清除使用記錄（須先清，因為有外鍵限制）
        couponUsageRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();

        // 重置 seed 優惠券使用次數為 0
        couponRepository.findById(C1).ifPresent(c -> {
            c.setUsageCount(0);
            c.setIsActive(true);
            c.setEndDate(Instant.now().plus(365, ChronoUnit.DAYS));
            couponRepository.save(c);
        });
        couponRepository.findById(C2).ifPresent(c -> {
            c.setUsageCount(0);
            c.setIsActive(true);
            c.setEndDate(Instant.now().plus(365, ChronoUnit.DAYS));
            couponRepository.save(c);
        });
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────────

    /**
     * 從回應 Header 中提取 SESSION_ID cookie 值。
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
     * 建立帶有 SESSION_ID cookie 的 HttpHeaders。
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
     * 加入商品至購物車並取得 sessionId。
     */
    private String addItemAndGetSession(UUID productId, int quantity) {
        AddItemRequest addReq = new AddItemRequest(productId, quantity);
        ResponseEntity<CartResponse> addResp = restTemplate.exchange(
                "/api/cart/items",
                HttpMethod.POST,
                new HttpEntity<>(addReq, new HttpHeaders()),
                CartResponse.class);
        return extractSessionId(addResp);
    }

    // ── 測試 1：有效優惠券驗證試算 ─────────────────────────────────────────

    @Test
    @DisplayName("有效優惠券 WELCOME10 — 試算 10% 折扣正確")
    void validateCoupon_validPercentage_returnsDiscount() {
        // 準備：訂單金額 1000
        BigDecimal orderAmount = new BigDecimal("1000.00");
        CouponValidateRequest request = new CouponValidateRequest("WELCOME10", orderAmount);

        ResponseEntity<CouponValidateResponse> response = restTemplate.postForEntity(
                "/api/coupons/validate",
                request,
                CouponValidateResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CouponValidateResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo("WELCOME10");
        // 折扣金額 = 1000 × 10% = 100
        assertThat(body.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        // 折後金額 = 1000 - 100 = 900
        assertThat(body.getFinalAmount()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(body.getOriginalAmount()).isEqualByComparingTo(orderAmount);
        assertThat(body.getDiscountType()).isEqualTo("PERCENTAGE");
    }

    // ── 測試 2：固定金額優惠券試算 ─────────────────────────────────────────

    @Test
    @DisplayName("有效優惠券 SAVE500 — 試算固定折扣 $500 正確")
    void validateCoupon_validFixed_returnsDiscount() {
        BigDecimal orderAmount = new BigDecimal("1500.00");
        CouponValidateRequest request = new CouponValidateRequest("SAVE500", orderAmount);

        ResponseEntity<CouponValidateResponse> response = restTemplate.postForEntity(
                "/api/coupons/validate",
                request,
                CouponValidateResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CouponValidateResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(body.getFinalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    // ── 測試 3：不存在的優惠券代碼 ────────────────────────────────────────

    @Test
    @DisplayName("不存在的優惠券代碼 — 回傳 404")
    void validateCoupon_notFound_returns404() {
        CouponValidateRequest request = new CouponValidateRequest("NOTEXIST", new BigDecimal("1000.00"));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/coupons/validate",
                request,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── 測試 4：已停用的優惠券 ────────────────────────────────────────────

    @Test
    @DisplayName("已停用的優惠券 — 回傳 400")
    void validateCoupon_inactive_returns400() {
        // 先停用 C1
        couponRepository.findById(C1).ifPresent(c -> {
            c.setIsActive(false);
            couponRepository.save(c);
        });

        CouponValidateRequest request = new CouponValidateRequest("WELCOME10", new BigDecimal("1000.00"));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/coupons/validate",
                request,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 測試 5：已過期的優惠券 ────────────────────────────────────────────

    @Test
    @DisplayName("已過期的優惠券 — 回傳 400")
    void validateCoupon_expired_returns400() {
        // 將 C1 的截止日期設為過去
        couponRepository.findById(C1).ifPresent(c -> {
            c.setEndDate(Instant.now().minus(1, ChronoUnit.DAYS));
            couponRepository.save(c);
        });

        CouponValidateRequest request = new CouponValidateRequest("WELCOME10", new BigDecimal("1000.00"));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/coupons/validate",
                request,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 測試 6：未達最低消費門檻 ──────────────────────────────────────────

    @Test
    @DisplayName("SAVE500 未達最低消費 $1000 — 回傳 400")
    void validateCoupon_belowMinOrderAmount_returns400() {
        // SAVE500 最低消費 $1000，傳入 $999
        CouponValidateRequest request = new CouponValidateRequest("SAVE500", new BigDecimal("999.00"));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/coupons/validate",
                request,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 測試 7：使用次數已達上限 ──────────────────────────────────────────

    @Test
    @DisplayName("優惠券使用次數達上限 — 回傳 400")
    void validateCoupon_usageCountExceeded_returns400() {
        // 將 C2 的使用次數設為上限（maxUsageCount=50）
        couponRepository.findById(C2).ifPresent(c -> {
            c.setUsageCount(50);
            couponRepository.save(c);
        });

        CouponValidateRequest request = new CouponValidateRequest("SAVE500", new BigDecimal("1500.00"));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/coupons/validate",
                request,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 測試 8：可用優惠券清單 ────────────────────────────────────────────

    @Test
    @DisplayName("查詢可用優惠券清單 — 回傳啟用且在有效期限內的優惠券")
    void getAvailableCoupons_returnsActive() {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/coupons/available",
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // seed 資料中 WELCOME10 和 SAVE500 在有效期內（setUp 已重置），SUMMER20 依日期而定
        assertThat(response.getBody()).isNotNull();
        // 至少有 WELCOME10 和 SAVE500
        assertThat(response.getBody().size()).isGreaterThanOrEqualTo(2);
    }

    // ── 測試 9：結帳時套用優惠券（百分比折扣） ─────────────────────────────

    @Test
    @DisplayName("結帳套用 WELCOME10 — 折扣金額正確，優惠券使用次數遞增")
    void checkout_withPercentageCoupon_discountApplied() {
        // 加入 1 個 P1（Scandinavian Sofa，$899）
        String sessionId = addItemAndGetSession(P1, 1);

        // 結帳並傳入優惠券代碼
        CheckoutRequest checkoutReq = new CheckoutRequest(VALID_RECIPIENT, "WELCOME10");
        ResponseEntity<OrderSummary> response = restTemplate.exchange(
                "/api/cart/checkout",
                HttpMethod.POST,
                new HttpEntity<>(checkoutReq, headersWithSession(sessionId)),
                OrderSummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderSummary summary = response.getBody();
        assertThat(summary).isNotNull();
        // total = 899
        assertThat(summary.getTotal()).isEqualByComparingTo(new BigDecimal("899.00"));
        // discountAmount = 899 × 10% = 89.90
        assertThat(summary.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("89.90"));
        // finalTotal = 899 - 89.90 = 809.10
        assertThat(summary.getFinalTotal()).isEqualByComparingTo(new BigDecimal("809.10"));
        assertThat(summary.getCouponCode()).isEqualTo("WELCOME10");

        // 驗證優惠券使用次數已遞增至 1
        Coupon updatedCoupon = couponRepository.findById(C1).orElseThrow();
        assertThat(updatedCoupon.getUsageCount()).isEqualTo(1);
    }

    // ── 測試 10：結帳時套用固定金額折扣 ────────────────────────────────────

    @Test
    @DisplayName("結帳套用 SAVE500（滿千折五百）— 折扣 $500 正確")
    void checkout_withFixedCoupon_discountApplied() {
        // 加入 2 個 P1（$899 × 2 = $1798）
        String sessionId = addItemAndGetSession(P1, 2);

        CheckoutRequest checkoutReq = new CheckoutRequest(VALID_RECIPIENT, "SAVE500");
        ResponseEntity<OrderSummary> response = restTemplate.exchange(
                "/api/cart/checkout",
                HttpMethod.POST,
                new HttpEntity<>(checkoutReq, headersWithSession(sessionId)),
                OrderSummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderSummary summary = response.getBody();
        assertThat(summary).isNotNull();
        // total = 1798
        assertThat(summary.getTotal()).isEqualByComparingTo(new BigDecimal("1798.00"));
        // discountAmount = 500（固定金額）
        assertThat(summary.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        // finalTotal = 1798 - 500 = 1298
        assertThat(summary.getFinalTotal()).isEqualByComparingTo(new BigDecimal("1298.00"));
    }

    // ── 測試 11：結帳不帶優惠券（向後相容） ────────────────────────────────

    @Test
    @DisplayName("結帳不帶優惠券 — discountAmount=0，finalTotal=total")
    void checkout_withoutCoupon_noDiscount() {
        String sessionId = addItemAndGetSession(P1, 1);

        CheckoutRequest checkoutReq = new CheckoutRequest(VALID_RECIPIENT, null);
        ResponseEntity<OrderSummary> response = restTemplate.exchange(
                "/api/cart/checkout",
                HttpMethod.POST,
                new HttpEntity<>(checkoutReq, headersWithSession(sessionId)),
                OrderSummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderSummary summary = response.getBody();
        assertThat(summary).isNotNull();
        assertThat(summary.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getFinalTotal()).isEqualByComparingTo(summary.getTotal());
        assertThat(summary.getCouponCode()).isNull();
    }

    // ── 測試 12：結帳帶不存在的優惠券代碼 ────────────────────────────────

    @Test
    @DisplayName("結帳帶不存在的優惠券代碼 — 回傳 404")
    void checkout_invalidCouponCode_returns404() {
        String sessionId = addItemAndGetSession(P1, 1);

        CheckoutRequest checkoutReq = new CheckoutRequest(VALID_RECIPIENT, "INVALID123");
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/cart/checkout",
                HttpMethod.POST,
                new HttpEntity<>(checkoutReq, headersWithSession(sessionId)),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── 測試 13：後台建立優惠券 ────────────────────────────────────────────

    @Test
    @DisplayName("後台建立優惠券 — 201 Created，代碼可查詢")
    void adminCreateCoupon_success() {
        // 建立新優惠券請求 body（使用 JSON 字串避免 DTO import 問題）
        String requestBody = """
                {
                  "code": "TESTCODE",
                  "name": "測試優惠",
                  "discountType": "PERCENTAGE",
                  "discountValue": 15,
                  "minOrderAmount": 0,
                  "startDate": "2026-01-01T00:00:00Z",
                  "endDate": "2026-12-31T23:59:59Z",
                  "isActive": true,
                  "description": "測試用優惠券"
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Coupon> response = restTemplate.postForEntity(
                "/api/admin/coupons",
                new HttpEntity<>(requestBody, headers),
                Coupon.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // 驗證可以用新代碼試算
        CouponValidateRequest validateReq = new CouponValidateRequest("TESTCODE", new BigDecimal("1000.00"));
        ResponseEntity<CouponValidateResponse> validateResp = restTemplate.postForEntity(
                "/api/coupons/validate",
                validateReq,
                CouponValidateResponse.class);
        assertThat(validateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(validateResp.getBody().getDiscountAmount())
                .isEqualByComparingTo(new BigDecimal("150.00")); // 1000 × 15%
    }

    // ── 測試 14：代碼大小寫不敏感 ────────────────────────────────────────

    @Test
    @DisplayName("優惠券代碼大小寫不敏感 — 小寫輸入 welcome10 等同 WELCOME10")
    void validateCoupon_caseInsensitive() {
        CouponValidateRequest request = new CouponValidateRequest("welcome10", new BigDecimal("1000.00"));

        ResponseEntity<CouponValidateResponse> response = restTemplate.postForEntity(
                "/api/coupons/validate",
                request,
                CouponValidateResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo("WELCOME10");
    }
}
