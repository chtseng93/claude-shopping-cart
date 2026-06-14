package com.shoppingcart.backend;

import com.shoppingcart.backend.dto.CouponValidateResponse;
import com.shoppingcart.backend.entity.Coupon;
import com.shoppingcart.backend.entity.DiscountType;
import com.shoppingcart.backend.exception.BadRequestException;
import com.shoppingcart.backend.exception.NotFoundException;
import com.shoppingcart.backend.repository.CouponRepository;
import com.shoppingcart.backend.repository.CouponUsageRepository;
import com.shoppingcart.backend.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * CouponService 純單元測試（Mockito，無需 Spring Context 或 Docker）。
 * 涵蓋驗證邏輯各邊界條件：
 * - 有效優惠券（百分比 / 固定金額）
 * - 代碼不存在
 * - 已停用
 * - 已過期（尚未開始 / 已截止）
 * - 未達最低消費門檻
 * - 使用次數已達上限
 * - 折扣計算精度（四捨五入）
 * - 固定折扣不超過訂單金額
 * - 消耗優惠券（遞增次數）
 * - 返還優惠券（遞減次數）
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceUnitTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @InjectMocks
    private CouponService couponService;

    /** 測試用優惠券（百分比 10%，無限次數，寬鬆條件） */
    private Coupon percentageCoupon;

    /** 測試用優惠券（固定金額 500，最低消費 1000，最大 2 次） */
    private Coupon fixedCoupon;

    @BeforeEach
    void setUp() {
        // 建立測試用百分比優惠券
        percentageCoupon = Coupon.create();
        percentageCoupon.setCode("PTEST10");
        percentageCoupon.setName("測試百分比折扣");
        percentageCoupon.setDiscountType(DiscountType.PERCENTAGE);
        percentageCoupon.setDiscountValue(new BigDecimal("10"));
        percentageCoupon.setMinOrderAmount(BigDecimal.ZERO);
        percentageCoupon.setMaxUsageCount(null);   // 無限制
        percentageCoupon.setUsageCount(0);
        percentageCoupon.setIsActive(true);
        percentageCoupon.setStartDate(Instant.now().minus(1, ChronoUnit.DAYS));
        percentageCoupon.setEndDate(Instant.now().plus(30, ChronoUnit.DAYS));

        // 建立測試用固定金額優惠券
        fixedCoupon = Coupon.create();
        fixedCoupon.setCode("FIXED500");
        fixedCoupon.setName("測試固定折扣");
        fixedCoupon.setDiscountType(DiscountType.FIXED);
        fixedCoupon.setDiscountValue(new BigDecimal("500"));
        fixedCoupon.setMinOrderAmount(new BigDecimal("1000"));
        fixedCoupon.setMaxUsageCount(2);
        fixedCoupon.setUsageCount(0);
        fixedCoupon.setIsActive(true);
        fixedCoupon.setStartDate(Instant.now().minus(1, ChronoUnit.DAYS));
        fixedCoupon.setEndDate(Instant.now().plus(30, ChronoUnit.DAYS));
    }

    // ── 測試 1：有效百分比優惠券驗證試算 ───────────────────────────────────

    @Test
    @DisplayName("有效百分比優惠券 — 折扣金額 = 訂單 × 10%")
    void validateCoupon_validPercentage_returnsCorrectDiscount() {
        when(couponRepository.findByCode("PTEST10")).thenReturn(Optional.of(percentageCoupon));

        CouponValidateResponse result = couponService.validateCoupon("PTEST10", new BigDecimal("1000.00"));

        assertThat(result.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.getFinalAmount()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(result.getOriginalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getDiscountType()).isEqualTo("PERCENTAGE");
    }

    // ── 測試 2：有效固定金額優惠券 ────────────────────────────────────────

    @Test
    @DisplayName("有效固定金額優惠券 — 折扣金額 = 500")
    void validateCoupon_validFixed_returnsFixedDiscount() {
        when(couponRepository.findByCode("FIXED500")).thenReturn(Optional.of(fixedCoupon));

        CouponValidateResponse result = couponService.validateCoupon("FIXED500", new BigDecimal("1500.00"));

        assertThat(result.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.getFinalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    // ── 測試 3：代碼不存在 ────────────────────────────────────────────────

    @Test
    @DisplayName("優惠券代碼不存在 — 拋出 NotFoundException")
    void validateCoupon_notFound_throwsNotFoundException() {
        when(couponRepository.findByCode("NOTEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.validateCoupon("NOTEXIST", new BigDecimal("1000.00")))
                .isInstanceOf(NotFoundException.class);
    }

    // ── 測試 4：已停用的優惠券 ────────────────────────────────────────────

    @Test
    @DisplayName("優惠券已停用 — 拋出 BadRequestException（優惠券已停用）")
    void validateCoupon_inactive_throwsBadRequest() {
        percentageCoupon.setIsActive(false);
        when(couponRepository.findByCode("PTEST10")).thenReturn(Optional.of(percentageCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon("PTEST10", new BigDecimal("1000.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("優惠券已停用");
    }

    // ── 測試 5：優惠券尚未開始 ────────────────────────────────────────────

    @Test
    @DisplayName("優惠券尚未開始 — 拋出 BadRequestException（優惠券尚未開始）")
    void validateCoupon_notStarted_throwsBadRequest() {
        percentageCoupon.setStartDate(Instant.now().plus(1, ChronoUnit.DAYS));
        when(couponRepository.findByCode("PTEST10")).thenReturn(Optional.of(percentageCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon("PTEST10", new BigDecimal("1000.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("尚未開始");
    }

    // ── 測試 6：已過期的優惠券 ────────────────────────────────────────────

    @Test
    @DisplayName("優惠券已過期 — 拋出 BadRequestException（優惠券已過期）")
    void validateCoupon_expired_throwsBadRequest() {
        percentageCoupon.setEndDate(Instant.now().minus(1, ChronoUnit.DAYS));
        when(couponRepository.findByCode("PTEST10")).thenReturn(Optional.of(percentageCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon("PTEST10", new BigDecimal("1000.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("已過期");
    }

    // ── 測試 7：未達最低消費門檻 ─────────────────────────────────────────

    @Test
    @DisplayName("FIXED500 未達最低消費 $1000 — 拋出 BadRequestException")
    void validateCoupon_belowMinOrderAmount_throwsBadRequest() {
        when(couponRepository.findByCode("FIXED500")).thenReturn(Optional.of(fixedCoupon));

        // 傳入 $999（< minOrderAmount $1000）
        assertThatThrownBy(() -> couponService.validateCoupon("FIXED500", new BigDecimal("999.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("最低消費門檻");
    }

    // ── 測試 8：使用次數已達上限 ──────────────────────────────────────────

    @Test
    @DisplayName("使用次數達上限（maxUsageCount=2, usageCount=2）— 拋出 BadRequestException")
    void validateCoupon_usageLimitReached_throwsBadRequest() {
        fixedCoupon.setUsageCount(2);  // 等於上限
        when(couponRepository.findByCode("FIXED500")).thenReturn(Optional.of(fixedCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon("FIXED500", new BigDecimal("1500.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("使用次數已達上限");
    }

    // ── 測試 9：百分比折扣計算精度（四捨五入至小數 2 位） ─────────────────

    @Test
    @DisplayName("百分比折扣四捨五入 — $1000 × 10% = $100.00")
    void calculateDiscount_percentage_roundsHalfUp() {
        // 1/3 折扣：$100 × 33.33% = $33.33
        percentageCoupon.setDiscountValue(new BigDecimal("33.33"));

        BigDecimal discount = couponService.calculateDiscount(percentageCoupon, new BigDecimal("100.00"));

        assertThat(discount).isEqualByComparingTo(new BigDecimal("33.33"));
    }

    // ── 測試 10：固定折扣不超過訂單金額 ──────────────────────────────────

    @Test
    @DisplayName("固定折扣不超過訂單金額 — 訂單 $300 折 $500 → 折扣僅 $300")
    void calculateDiscount_fixed_doesNotExceedOrderAmount() {
        // fixedCoupon 折扣 $500，但訂單只有 $300
        // minOrderAmount 設為 0 避免門檻驗證干擾
        fixedCoupon.setMinOrderAmount(BigDecimal.ZERO);

        BigDecimal discount = couponService.calculateDiscount(fixedCoupon, new BigDecimal("300.00"));

        // 折扣取最小值（300 < 500）
        assertThat(discount).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    // ── 測試 11：代碼大小寫不敏感 ────────────────────────────────────────

    @Test
    @DisplayName("代碼大小寫不敏感 — 輸入 ptest10 等同 PTEST10")
    void validateCoupon_codeIsUpperCased() {
        when(couponRepository.findByCode("PTEST10")).thenReturn(Optional.of(percentageCoupon));

        // 輸入小寫，系統應轉大寫後查詢
        CouponValidateResponse result = couponService.validateCoupon("ptest10", new BigDecimal("1000.00"));

        assertThat(result.getCode()).isEqualTo("PTEST10");
        verify(couponRepository).findByCode("PTEST10");
    }

    // ── 測試 12：無使用次數上限的優惠券 ──────────────────────────────────

    @Test
    @DisplayName("maxUsageCount=null（無限制）— 高使用次數仍可通過驗證")
    void validateCoupon_unlimitedUsage_alwaysValid() {
        percentageCoupon.setMaxUsageCount(null);    // 無限制
        percentageCoupon.setUsageCount(99999);      // 高使用次數
        when(couponRepository.findByCode("PTEST10")).thenReturn(Optional.of(percentageCoupon));

        // 不應拋出例外
        CouponValidateResponse result = couponService.validateCoupon("PTEST10", new BigDecimal("1000.00"));
        assertThat(result).isNotNull();
    }

    // ── 測試 13：consumeCoupon 遞增 usageCount ───────────────────────────

    @Test
    @DisplayName("consumeCoupon — 使用次數由 0 遞增至 1，使用記錄儲存")
    void consumeCoupon_incrementsUsageCount() {
        assertThat(percentageCoupon.getUsageCount()).isEqualTo(0);

        couponService.consumeCoupon(
                percentageCoupon,
                UUID.randomUUID(),
                "test-session-id",
                new BigDecimal("100.00"));

        // 驗證 usageCount 已遞增
        assertThat(percentageCoupon.getUsageCount()).isEqualTo(1);
        // 驗證 CouponUsage 已儲存
        verify(couponUsageRepository, times(1)).save(any());
    }

    // ── 測試 14：releaseCoupon 遞減 usageCount ────────────────────────────

    @Test
    @DisplayName("releaseCoupon — 有使用記錄時遞減次數並刪除記錄")
    void releaseCoupon_decrementsUsageCount() {
        percentageCoupon.setUsageCount(3);
        UUID cartId = UUID.randomUUID();

        com.shoppingcart.backend.entity.CouponUsage usage =
                new com.shoppingcart.backend.entity.CouponUsage(
                        percentageCoupon, cartId, "session", new BigDecimal("100.00"));

        when(couponUsageRepository.findByCartId(cartId)).thenReturn(Optional.of(usage));

        couponService.releaseCoupon(cartId);

        // 驗證次數遞減
        assertThat(percentageCoupon.getUsageCount()).isEqualTo(2);
        // 驗證使用記錄已刪除
        verify(couponUsageRepository, times(1)).delete(usage);
    }

    // ── 測試 15：releaseCoupon 無使用記錄時不做任何事 ─────────────────────

    @Test
    @DisplayName("releaseCoupon — 無使用記錄時不修改次數")
    void releaseCoupon_noUsageRecord_doesNothing() {
        UUID cartId = UUID.randomUUID();
        when(couponUsageRepository.findByCartId(cartId)).thenReturn(Optional.empty());

        couponService.releaseCoupon(cartId);

        // 不應有任何刪除操作
        verify(couponUsageRepository, never()).delete(any());
    }

    // ── 測試 16：usageCount 遞減不低於 0 ──────────────────────────────────

    @Test
    @DisplayName("releaseCoupon — usageCount 已為 0 時，遞減後仍維持 0")
    void releaseCoupon_usageCountZero_staysAtZero() {
        percentageCoupon.setUsageCount(0);   // 已為 0
        UUID cartId = UUID.randomUUID();

        com.shoppingcart.backend.entity.CouponUsage usage =
                new com.shoppingcart.backend.entity.CouponUsage(
                        percentageCoupon, cartId, "session", new BigDecimal("50.00"));

        when(couponUsageRepository.findByCartId(cartId)).thenReturn(Optional.of(usage));

        couponService.releaseCoupon(cartId);

        // 驗證次數維持 0，不為負數
        assertThat(percentageCoupon.getUsageCount()).isEqualTo(0);
    }
}
