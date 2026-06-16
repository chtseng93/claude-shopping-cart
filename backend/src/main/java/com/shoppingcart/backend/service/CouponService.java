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

/**
 * 優惠券核心服務，包含以下功能：
 * <ul>
 *   <li>驗證試算（validateCoupon）：驗證代碼並計算折扣，不消耗使用次數</li>
 *   <li>套用優惠券（applyCoupon）：結帳時查詢 → 驗證 → 計算 → 消耗，一次完成</li>
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
     * @param code        優惠券代碼（自動正規化為大寫）
     * @param orderAmount 訂單原始金額（由購物車合計取得，伺服器計算）
     * @return 驗證通過的試算回應（含折扣金額、折後金額）
     * @throws NotFoundException   優惠券代碼不存在
     * @throws BadRequestException 驗證不通過（已停用、過期、不符門檻、超過次數）
     */
    @Transactional(readOnly = true)
    public CouponValidateResponse validateCoupon(String code, BigDecimal orderAmount) {
        String upperCode = normalizeCode(code);

        Coupon coupon = couponRepository.findByCode(upperCode)
                .orElseThrow(() -> new NotFoundException("優惠券代碼不存在：" + upperCode));

        validateCouponRules(coupon, orderAmount);

        BigDecimal discountAmount = calculateDiscount(coupon, orderAmount);

        log.info("優惠券試算完成：code={}, orderAmount={}, discountAmount={}", upperCode, orderAmount, discountAmount);

        return new CouponValidateResponse(coupon, discountAmount, orderAmount);
    }

    // ── 優惠券套用（結帳時呼叫）─────────────────────────────────────────

    /**
     * 結帳時套用優惠券：查詢 → 驗證 → 計算折扣 → 消耗（建立使用記錄、遞增次數）。
     * 須在外層 @Transactional 方法內呼叫，以確保與庫存扣減在同一交易中。
     *
     * @param code      優惠券代碼（自動正規化為大寫）
     * @param total     訂單原始金額
     * @param cartId    已結帳購物車 UUID
     * @param sessionId 使用者 session 識別碼
     * @return 實際折扣金額（由本服務計算，不接受客戶端傳入）
     * @throws NotFoundException   優惠券代碼不存在
     * @throws BadRequestException 驗證不通過
     */
    public BigDecimal applyCoupon(String code, BigDecimal total, UUID cartId, String sessionId) {
        String upperCode = normalizeCode(code);
        Coupon coupon = couponRepository.findByCode(upperCode)
                .orElseThrow(() -> new NotFoundException("優惠券代碼不存在：" + upperCode));
        validateCouponRules(coupon, total);
        BigDecimal discountAmount = calculateDiscount(coupon, total);
        consumeCoupon(coupon, cartId, sessionId, discountAmount);
        return discountAmount;
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
        couponUsageRepository.findByCartId(cartId).ifPresent(usage -> {
            Coupon coupon = usage.getCoupon();
            coupon.setUsageCount(Math.max(0, coupon.getUsageCount() - 1));
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
                .toList();
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
     * 代碼統一正規化（trimUpperCase）存入，確保唯一性比對一致。
     *
     * @param request 優惠券建立請求（已通過 @Valid 驗證）
     * @return 建立後的 Coupon Entity
     * @throws BadRequestException 代碼已存在或 endDate 早於 startDate
     */
    @Transactional
    public Coupon createCoupon(CouponAdminRequest request) {
        String upperCode = normalizeCode(request.getCode());
        if (couponRepository.findByCode(upperCode).isPresent()) {
            throw new BadRequestException("優惠券代碼已存在：" + upperCode);
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("截止日期必須晚於開始日期");
        }

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
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("優惠券不存在"));

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("截止日期必須晚於開始日期");
        }

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
     * 正規化優惠券代碼：去除前後空白並轉大寫。
     * 所有公開方法的入口統一透過此方法處理，確保代碼格式一致。
     */
    private static String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    /**
     * 驗證優惠券規則：啟用狀態、有效期限、最低消費門檻、使用次數上限。
     *
     * @param coupon      優惠券 Entity
     * @param orderAmount 訂單原始金額
     * @throws BadRequestException 任一驗證不通過時拋出
     */
    private void validateCouponRules(Coupon coupon, BigDecimal orderAmount) {
        if (!coupon.getIsActive()) {
            throw new BadRequestException("優惠券已停用");
        }

        Instant now = Instant.now();
        if (now.isBefore(coupon.getStartDate())) {
            throw new BadRequestException("優惠券尚未開始");
        }
        if (now.isAfter(coupon.getEndDate())) {
            throw new BadRequestException("優惠券已過期");
        }

        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                "未達最低消費門檻 $" + coupon.getMinOrderAmount().stripTrailingZeros().toPlainString());
        }

        if (coupon.getMaxUsageCount() != null
            && coupon.getUsageCount() >= coupon.getMaxUsageCount()) {
            throw new BadRequestException("優惠券使用次數已達上限");
        }
    }

    /**
     * 依折扣類型計算實際折扣金額（伺服器權威，前端不得自行計算）。
     * - PERCENTAGE：orderAmount × discountValue / 100，四捨五入至小數點後 2 位
     * - FIXED：折扣金額不超過訂單金額（取最小值）
     */
    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        return switch (coupon.getDiscountType()) {
            case PERCENTAGE ->
                orderAmount.multiply(coupon.getDiscountValue())
                           .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED ->
                coupon.getDiscountValue().min(orderAmount);
        };
    }

    /**
     * 正式消耗優惠券（須在外層 @Transactional 方法內呼叫）。
     * 建立 CouponUsage 記錄，並將 Coupon.usageCount 加 1。
     */
    private void consumeCoupon(Coupon coupon, UUID cartId, String sessionId, BigDecimal discountAmount) {
        couponUsageRepository.save(new CouponUsage(coupon, cartId, sessionId, discountAmount));
        coupon.setUsageCount(coupon.getUsageCount() + 1);
        log.info("優惠券消耗：code={}, cartId={}, discountAmount={}", coupon.getCode(), cartId, discountAmount);
    }

    /**
     * 將請求 DTO 的欄位套用至 Coupon Entity（建立與更新共用）。
     */
    private void applyRequestToCoupon(Coupon coupon, CouponAdminRequest request) {
        coupon.setName(request.getName());
        coupon.setDiscountType(DiscountType.valueOf(request.getDiscountType()));
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxUsageCount(request.getMaxUsageCount());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());
        coupon.setIsActive(request.getIsActive());
        coupon.setDescription(request.getDescription());
    }
}
