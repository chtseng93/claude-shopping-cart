package com.shoppingcart.backend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 後台管理優惠券請求 DTO（POST/PUT /api/admin/coupons）。
 * 供管理員建立或更新優惠券使用，包含完整欄位驗證。
 */
public class CouponAdminRequest {

    /** 優惠券代碼（建立時必填，更新時忽略 code 欄位不修改） */
    @NotBlank(message = "優惠券代碼不可為空")
    @Pattern(regexp = "^[A-Z0-9]{1,50}$", message = "優惠券代碼只能包含大寫字母與數字，長度 1~50")
    private String code;

    /** 優惠券名稱，必填 */
    @NotBlank(message = "優惠券名稱不可為空")
    @Size(max = 255, message = "名稱最多 255 字元")
    private String name;

    /** 折扣類型：PERCENTAGE 或 FIXED，必填 */
    @NotBlank(message = "折扣類型不可為空")
    @Pattern(regexp = "^(PERCENTAGE|FIXED)$", message = "折扣類型只能是 PERCENTAGE 或 FIXED")
    private String discountType;

    /** 折扣值，必須大於 0 */
    @NotNull(message = "折扣值不可為空")
    @DecimalMin(value = "0.01", message = "折扣值必須大於 0")
    private BigDecimal discountValue;

    /** 最低訂單金額門檻，選填，預設 0 */
    @DecimalMin(value = "0", message = "最低訂單金額不可小於 0")
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /** 最大使用次數上限，選填（null 表示無限制） */
    @Min(value = 1, message = "最大使用次數必須大於 0")
    private Integer maxUsageCount;

    /** 有效開始時間，必填 */
    @NotNull(message = "有效開始時間不可為空")
    private Instant startDate;

    /** 有效截止時間，必填（需晚於 startDate） */
    @NotNull(message = "有效截止時間不可為空")
    private Instant endDate;

    /** 是否啟用，選填，預設 true */
    private Boolean isActive = true;

    /** 優惠券說明，選填 */
    private String description;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 反序列化用無參數建構子 */
    public CouponAdminRequest() {}

    // ── Getters / Setters ────────────────────────────────────────────────

    /** 取得優惠券代碼 */
    public String getCode() { return code; }

    /** 設定優惠券代碼 */
    public void setCode(String code) { this.code = code; }

    /** 取得名稱 */
    public String getName() { return name; }

    /** 設定名稱 */
    public void setName(String name) { this.name = name; }

    /** 取得折扣類型 */
    public String getDiscountType() { return discountType; }

    /** 設定折扣類型 */
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    /** 取得折扣值 */
    public BigDecimal getDiscountValue() { return discountValue; }

    /** 設定折扣值 */
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }

    /** 取得最低訂單金額門檻 */
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }

    /** 設定最低訂單金額門檻 */
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    /** 取得最大使用次數 */
    public Integer getMaxUsageCount() { return maxUsageCount; }

    /** 設定最大使用次數 */
    public void setMaxUsageCount(Integer maxUsageCount) { this.maxUsageCount = maxUsageCount; }

    /** 取得有效開始時間 */
    public Instant getStartDate() { return startDate; }

    /** 設定有效開始時間 */
    public void setStartDate(Instant startDate) { this.startDate = startDate; }

    /** 取得有效截止時間 */
    public Instant getEndDate() { return endDate; }

    /** 設定有效截止時間 */
    public void setEndDate(Instant endDate) { this.endDate = endDate; }

    /** 取得是否啟用 */
    public Boolean getIsActive() { return isActive; }

    /** 設定是否啟用 */
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    /** 取得說明文字 */
    public String getDescription() { return description; }

    /** 設定說明文字 */
    public void setDescription(String description) { this.description = description; }
}
