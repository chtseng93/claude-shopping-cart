package com.shoppingcart.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 優惠券主檔 Entity — 對應資料表 coupon。
 * 儲存優惠券代碼、折扣類型、使用限制、有效期間等資訊。
 * 折扣金額由服務層（CouponService）根據此 Entity 計算，不接受客戶端傳入折扣金額。
 */
@Entity
@Table(name = "coupon")
public class Coupon {

    /** 優惠券唯一識別碼（UUID），由資料庫自動產生 */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** 優惠券代碼（唯一，大寫英數字），使用者輸入時以此查詢 */
    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    /** 優惠券名稱，顯示給使用者 */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * 折扣類型：PERCENTAGE（百分比）或 FIXED（固定金額）。
     * 使用 EnumType.STRING 確保資料庫存字串值，易於維護。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    /**
     * 折扣值。
     * - PERCENTAGE 類型：0~100（百分比，如 10 表示九折）
     * - FIXED 類型：固定折抵金額（TWD）
     */
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /** 最低訂單金額門檻；訂單金額須達此門檻才可使用此優惠券 */
    @Column(name = "min_order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /** 全局最大使用次數上限；NULL 表示無次數限制 */
    @Column(name = "max_usage_count")
    private Integer maxUsageCount;

    /** 目前已使用次數，每次消耗（consumeCoupon）時遞增 */
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    /** 優惠券有效開始時間（UTC） */
    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    /** 優惠券有效截止時間（UTC）；逾期後不可使用 */
    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    /** 是否啟用；停用的優惠券即使在有效期限內也無法使用 */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** 優惠券說明文字，顯示給使用者（可為空） */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 建立時間，由 Hibernate 於 INSERT 時自動填入 now() */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** JPA 必要的無參數建構子（protected 防止外部直接實例化） */
    protected Coupon() {}

    /** 供服務層建立新優惠券使用的公開工廠方法 */
    public static Coupon create() {
        return new Coupon();
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    /** 取得優惠券 ID */
    public UUID getId() { return id; }

    /** 取得優惠券代碼 */
    public String getCode() { return code; }

    /** 設定優惠券代碼 */
    public void setCode(String code) { this.code = code; }

    /** 取得優惠券名稱 */
    public String getName() { return name; }

    /** 設定優惠券名稱 */
    public void setName(String name) { this.name = name; }

    /** 取得折扣類型 */
    public DiscountType getDiscountType() { return discountType; }

    /** 設定折扣類型 */
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    /** 取得折扣值 */
    public BigDecimal getDiscountValue() { return discountValue; }

    /** 設定折扣值 */
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }

    /** 取得最低訂單金額門檻 */
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }

    /** 設定最低訂單金額門檻 */
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    /** 取得最大使用次數上限（null 表示無限制） */
    public Integer getMaxUsageCount() { return maxUsageCount; }

    /** 設定最大使用次數上限 */
    public void setMaxUsageCount(Integer maxUsageCount) { this.maxUsageCount = maxUsageCount; }

    /** 取得目前已使用次數 */
    public Integer getUsageCount() { return usageCount; }

    /** 設定已使用次數（消耗/返還時使用） */
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

    /** 取得有效開始時間 */
    public Instant getStartDate() { return startDate; }

    /** 設定有效開始時間 */
    public void setStartDate(Instant startDate) { this.startDate = startDate; }

    /** 取得有效截止時間 */
    public Instant getEndDate() { return endDate; }

    /** 設定有效截止時間 */
    public void setEndDate(Instant endDate) { this.endDate = endDate; }

    /** 取得是否啟用狀態 */
    public Boolean getIsActive() { return isActive; }

    /** 設定啟用狀態 */
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    /** 取得優惠券說明 */
    public String getDescription() { return description; }

    /** 設定優惠券說明 */
    public void setDescription(String description) { this.description = description; }

    /** 取得建立時間 */
    public Instant getCreatedAt() { return createdAt; }
}
