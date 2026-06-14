package com.shoppingcart.backend.service;

import com.shoppingcart.backend.dto.CouponAdminRequest;
import com.shoppingcart.backend.dto.CouponSummaryResponse;
import com.shoppingcart.backend.dto.CouponValidateResponse;
import com.shoppingcart.backend.entity.Coupon;
import com.shoppingcart.backend.entity.CouponUsage;
import com.shoppingcart.backend.entity.DiscountType;
import com.shoppingcart.backend.exception.BadRequestException;
import com.shoppingcart.backend.exception.NotFoundException;
import com.shoppingcart.backend.repository.CouponRepository;
import com.shoppingcart.backend.repository.CouponUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 優惠券核心服務，包含以下功能：
 * <ul>
 *   <li>驗證試算（validateCoupon）：驗證代碼並計算折扣，不消耗使用次數</li>
 *   <li>折扣計算（calculateDiscount）：依折扣類型計算實際折扣金額（伺服器權威）</li>
 *   <li>消耗優惠券（consumeCoupon）：結帳成立後記錄使用並遞增次數</li>
 *   <li>返還優惠券（releaseCoupon）：訂單取消時刪除記錄並遞減次數</li>
 *   <li>後台管理（CRUD）：新增、更新、刪除優惠券</li>
 * </ul>
 *
 * <p>所有折扣金額均由本服務計算，不接受客戶端傳入，符合「金額權威在伺服器」原則。</p>
 */
@Service
public class CouponService {

    /** 日誌記錄器 */
    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    /** 優惠券主檔資料存取層 */
    private final CouponRepository couponRepository;

    /** 優惠券使用記錄資料存取層 */
    private final CouponUsageRepository couponUsageRepository;

    /**
     * 建構子注入所有依賴的 Repository。
     *
     * @param couponRepository      優惠券 Repository
     * @param couponUsageRepository 優惠券使用記錄 Repository
     */
    public CouponService(CouponRepository couponRepository,
                         CouponUsageRepository couponUsageRepository) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
    }

    // ── 優惠券驗證試算 ────────────────────────────────────────────────────

    /**
     * 驗證優惠券並試算折扣金額（不消耗使用次數，僅用於前端即時顯示折扣）。
     * 依序驗證：存在 → 啟用 → 有效期限 → 最低消費 → 使用次數。
     *
     * @param code        優惠券代碼（自動轉大寫）
     * @param orderAmount 訂單原始金額（由購物車合計取得，伺服器計算）
     * @return 驗證通過的試算回應（含折扣金額、折後金額）
     * @throws NotFoundException   優惠券代碼不存在
     * @throws BadRequestException 驗證不通過（已停用、過期、不符門檻、超過次數）
     */
    @Transactional(readOnly = true)
    public CouponValidateResponse validateCoupon(String code, BigDecimal orderAmount) {
        // 代碼統一轉大寫，確保大小寫不敏感
        String upperCode = code.trim().toUpperCase();

        // 查詢優惠券，不存在則拋 404
        Coupon coupon = couponRepository.findByCode(upperCode)
                .orElseThrow(() -> new NotFoundException("優惠券代碼不存在：" + upperCode));

        // 逐項驗證優惠券規則
        validateCouponRules(coupon, orderAmount);

        // 由伺服器計算折扣金額（不修改資料庫）
        BigDecimal discountAmount = calculateDiscount(coupon, orderAmount);

        log.info("優惠券試算完成：code={}, orderAmount={}, discountAmount={}", upperCode, orderAmount, discountAmount);

        return new CouponValidateResponse(coupon, discountAmount, orderAmount);
    }

    /**
     * 驗證優惠券規則（內部使用，結帳時也呼叫此方法）。
     * 依序驗證啟用狀態、有效期限、最低消費門檻、使用次數上限。
     *
     * @param coupon      優惠券 Entity
     * @param orderAmount 訂單原始金額
     * @throws BadRequestException 任一驗證不通過時拋出
     */
    public void validateCouponRules(Coupon coupon, BigDecimal orderAmount) {
        // 驗證 1：是否啟用
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new BadRequestException("優惠券已停用");
        }

        // 驗證 2：有效期限（startDate <= now <= endDate）
        Instant now = Instant.now();
        if (now.isBefore(coupon.getStartDate())) {
            throw new BadRequestException("優惠券尚未開始");
        }
        if (now.isAfter(coupon.getEndDate())) {
            throw new BadRequestException("優惠券已過期");
        }

        // 驗證 3：最低消費門檻
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                "未達最低消費門檻 $" + coupon.getMinOrderAmount().stripTrailingZeros().toPlainString());
        }

        // 驗證 4：使用次數上限（maxUsageCount 為 null 表示無限制）
        if (coupon.getMaxUsageCount() != null
            && coupon.getUsageCount() >= coupon.getMaxUsageCount()) {
            throw new BadRequestException("優惠券使用次數已達上限");
        }
    }

    /**
     * 依折扣類型計算實際折扣金額（伺服器權威，前端不得自行計算）。
     * - PERCENTAGE：orderAmount × discountValue / 100，四捨五入至整數元
     * - FIXED：折扣金額不超過訂單金額（取最小值）
     *
     * @param coupon      優惠券 Entity
     * @param orderAmount 訂單原始金額
     * @return 折扣金額（BigDecimal，不小於 0）
     */
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        return switch (coupon.getDiscountType()) {
            case PERCENTAGE ->
                // 百分比折扣：訂單金額 × 折扣百分比 ÷ 100，無條件四捨五入至小數點後 2 位
                orderAmount.multiply(coupon.getDiscountValue())
                           .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED ->
                // 固定金額折扣：取折扣金額與訂單金額的最小值（折扣不超過訂單金額）
                coupon.getDiscountValue().min(orderAmount);
        };
    }

    // ── 優惠券消耗（結帳時呼叫）─────────────────────────────────────────

    /**
     * 正式消耗優惠券（於結帳 @Transactional 方法內呼叫）。
     * 建立 CouponUsage 記錄，並將 Coupon.usageCount 加 1。
     * 此方法需在已有 @Transactional 的外層方法中呼叫，以確保交易一致性。
     *
     * @param coupon         已驗證通過的優惠券 Entity
     * @param cartId         已結帳的購物車（訂單）UUID
     * @param sessionId      使用者 session 識別碼
     * @param discountAmount 實際折扣金額（由 calculateDiscount 計算）
     */
    public void consumeCoupon(Coupon coupon, UUID cartId, String sessionId, BigDecimal discountAmount) {
        // 建立使用記錄（存入 coupon_usage 資料表）
        CouponUsage usage = new CouponUsage(coupon, cartId, sessionId, discountAmount);
        couponUsageRepository.save(usage);

        // 更新已使用次數（自動由 JPA dirty checking 於交易提交時 UPDATE）
        coupon.setUsageCount(coupon.getUsageCount() + 1);

        log.info("優惠券消耗：code={}, cartId={}, discountAmount={}", coupon.getCode(), cartId, discountAmount);
    }

    // ── 優惠券返還（訂單取消時呼叫）──────────────────────────────────────

    /**
     * 返還優惠券使用次數（訂單取消或付款失敗時呼叫）。
     * 刪除對應的 CouponUsage 記錄，並將 Coupon.usageCount 減 1（最小為 0）。
     *
     * @param cartId 已取消訂單的購物車 UUID
     */
    @Transactional
    public void releaseCoupon(UUID cartId) {
        // 查詢該訂單是否有使用優惠券記錄
        couponUsageRepository.findByCartId(cartId).ifPresent(usage -> {
            Coupon coupon = usage.getCoupon();

            // 使用次數遞減（確保不小於 0 防止資料異常）
            coupon.setUsageCount(Math.max(0, coupon.getUsageCount() - 1));

            // 刪除使用記錄
            couponUsageRepository.delete(usage);

            log.info("優惠券返還：code={}, cartId={}", coupon.getCode(), cartId);
        });
    }

    // ── 查詢可用優惠券（前端選擇清單）──────────────────────────────────

    /**
     * 取得目前可用的優惠券清單（啟用中、在有效期限內、未超過使用次數上限）。
     * 供前端結帳頁顯示可選擇的優惠券。
     *
     * @return 可用優惠券摘要清單（不含敏感的後台欄位）
     */
    @Transactional(readOnly = true)
    public List<CouponSummaryResponse> getAvailableCoupons() {
        Instant now = Instant.now();
        return couponRepository.findAvailableCoupons(now)
                .stream()
                .map(CouponSummaryResponse::from)
                .collect(Collectors.toList());
    }

    // ── 後台管理：查詢所有優惠券 ────────────────────────────────────────

    /**
     * 取得所有優惠券（含已停用），供後台管理員檢視。
     *
     * @return 所有優惠券清單
     */
    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    // ── 後台管理：建立優惠券 ────────────────────────────────────────────

    /**
     * 建立新優惠券（後台管理）。
     * 代碼統一轉大寫存入，確保唯一性比對一致。
     *
     * @param request 優惠券建立請求（已通過 @Valid 驗證）
     * @return 建立後的 Coupon Entity
     * @throws BadRequestException 代碼已存在或 endDate 早於 startDate
     */
    @Transactional
    public Coupon createCoupon(CouponAdminRequest request) {
        // 代碼轉大寫並驗證唯一性
        String upperCode = request.getCode().trim().toUpperCase();
        if (couponRepository.findByCode(upperCode).isPresent()) {
            throw new BadRequestException("優惠券代碼已存在：" + upperCode);
        }

        // 驗證截止日期須晚於開始日期
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("截止日期必須晚於開始日期");
        }

        // 建立 Coupon Entity 並填入欄位（使用工廠方法，不直接呼叫 protected 建構子）
        Coupon coupon = Coupon.create();
        coupon.setCode(upperCode);
        applyRequestToCoupon(coupon, request);

        Coupon saved = couponRepository.save(coupon);
        log.info("優惠券建立：code={}", upperCode);
        return saved;
    }

    // ── 後台管理：更新優惠券 ────────────────────────────────────────────

    /**
     * 更新優惠券資料（後台管理）。
     * 代碼建立後不可修改；其他欄位可更新。
     *
     * @param id      優惠券 UUID
     * @param request 更新請求（已通過 @Valid 驗證）
     * @return 更新後的 Coupon Entity
     * @throws NotFoundException   優惠券不存在
     * @throws BadRequestException endDate 早於 startDate
     */
    @Transactional
    public Coupon updateCoupon(UUID id, CouponAdminRequest request) {
        // 查詢優惠券
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("優惠券不存在"));

        // 驗證截止日期須晚於開始日期
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("截止日期必須晚於開始日期");
        }

        // 套用更新（code 欄位不修改）
        applyRequestToCoupon(coupon, request);

        log.info("優惠券更新：id={}, code={}", id, coupon.getCode());
        return coupon;
    }

    // ── 後台管理：刪除優惠券 ────────────────────────────────────────────

    /**
     * 刪除優惠券（後台管理）。
     * 注意：若已有使用記錄（coupon_usage），需先處理參照完整性問題。
     *
     * @param id 優惠券 UUID
     * @throws NotFoundException 優惠券不存在
     */
    @Transactional
    public void deleteCoupon(UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("優惠券不存在"));
        couponRepository.delete(coupon);
        log.info("優惠券刪除：id={}, code={}", id, coupon.getCode());
    }

    // ── 私有輔助方法 ──────────────────────────────────────────────────────

    /**
     * 將請求 DTO 的欄位套用至 Coupon Entity（建立與更新共用）。
     *
     * @param coupon  目標優惠券 Entity
     * @param request 建立/更新請求 DTO
     */
    private void applyRequestToCoupon(Coupon coupon, CouponAdminRequest request) {
        coupon.setName(request.getName());
        coupon.setDiscountType(DiscountType.valueOf(request.getDiscountType()));
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(
            request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO);
        coupon.setMaxUsageCount(request.getMaxUsageCount());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());
        coupon.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        coupon.setDescription(request.getDescription());
    }
}
