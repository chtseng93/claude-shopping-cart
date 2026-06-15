package com.shoppingcart.backend.controller;

import com.shoppingcart.backend.dto.CouponAdminRequest;
import com.shoppingcart.backend.dto.CouponSummaryResponse;
import com.shoppingcart.backend.dto.CouponValidateRequest;
import com.shoppingcart.backend.dto.CouponValidateResponse;
import com.shoppingcart.backend.entity.Coupon;
import com.shoppingcart.backend.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 優惠券 REST 控制器，提供以下端點：
 * <ul>
 *   <li>POST /api/coupons/validate — 優惠券驗證試算（前端使用）</li>
 *   <li>GET /api/coupons/available — 查詢可用優惠券清單（前端使用）</li>
 *   <li>GET /api/admin/coupons — 後台查詢所有優惠券</li>
 *   <li>POST /api/admin/coupons — 後台建立優惠券</li>
 *   <li>PUT /api/admin/coupons/{id} — 後台更新優惠券</li>
 *   <li>DELETE /api/admin/coupons/{id} — 後台刪除優惠券</li>
 * </ul>
 *
 * <p>不含商業邏輯，所有邏輯委由 {@link CouponService} 處理。</p>
 */
@RestController
public class CouponController {

    /** 優惠券服務層 */
    private final CouponService couponService;

    /**
     * 建構子注入 CouponService。
     *
     * @param couponService 優惠券服務
     */
    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // ── 前端 API ─────────────────────────────────────────────────────────

    /**
     * 優惠券驗證試算（POST /api/coupons/validate）。
     * 驗證代碼是否有效並計算折扣，不消耗使用次數。
     * 折扣金額由伺服器計算後回傳，前端不得自行計算。
     *
     * @param request 驗證請求（code + orderAmount），已通過 @Valid 驗證
     * @return 200 含折扣試算結果；400 驗證不通過；404 代碼不存在
     */
    @PostMapping("/api/coupons/validate")
    public ResponseEntity<CouponValidateResponse> validateCoupon(
            @Valid @RequestBody CouponValidateRequest request) {
        CouponValidateResponse response = couponService.validateCoupon(
                request.getCode(), request.getOrderAmount());
        return ResponseEntity.ok(response);
    }

    /**
     * 查詢可用優惠券清單（GET /api/coupons/available）。
     * 回傳目前啟用中、在有效期限內且未超過次數上限的優惠券。
     * 供前端結帳頁顯示可選擇的優惠券清單。
     *
     * @return 200 可用優惠券摘要清單
     */
    @GetMapping("/api/coupons/available")
    public ResponseEntity<List<CouponSummaryResponse>> getAvailableCoupons() {
        List<CouponSummaryResponse> coupons = couponService.getAvailableCoupons();
        return ResponseEntity.ok(coupons);
    }

    // ── 後台管理 API ─────────────────────────────────────────────────────

    /**
     * 取得所有優惠券（GET /api/admin/coupons）。
     * 含已停用的優惠券，供後台管理員使用。
     *
     * @return 200 所有優惠券清單
     */
    @GetMapping("/api/admin/coupons")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        List<Coupon> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(coupons);
    }

    /**
     * 建立優惠券（POST /api/admin/coupons）。
     * 代碼需唯一，系統自動轉大寫存入。
     *
     * @param request 建立請求，已通過 @Valid 驗證
     * @return 201 建立成功含完整 Coupon；400 代碼已存在或欄位驗證失敗
     */
    @PostMapping("/api/admin/coupons")
    public ResponseEntity<Coupon> createCoupon(
            @Valid @RequestBody CouponAdminRequest request) {
        Coupon coupon = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(coupon);
    }

    /**
     * 更新優惠券（PUT /api/admin/coupons/{id}）。
     * 代碼建立後不可修改，其他欄位可更新。
     *
     * @param id      優惠券 UUID（path 參數）
     * @param request 更新請求，已通過 @Valid 驗證
     * @return 200 更新後的 Coupon；404 不存在；400 欄位驗證失敗
     */
    @PutMapping("/api/admin/coupons/{id}")
    public ResponseEntity<Coupon> updateCoupon(
            @PathVariable UUID id,
            @Valid @RequestBody CouponAdminRequest request) {
        Coupon coupon = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(coupon);
    }

    /**
     * 刪除優惠券（DELETE /api/admin/coupons/{id}）。
     *
     * @param id 優惠券 UUID（path 參數）
     * @return 204 刪除成功；404 不存在
     */
    @DeleteMapping("/api/admin/coupons/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable UUID id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }
}
