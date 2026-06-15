package com.shoppingcart.backend.entity;

/**
 * 優惠券折扣類型枚舉。
 * PERCENTAGE：按訂單金額百分比折扣（如 10 表示打九折）。
 * FIXED：固定金額折扣（如 500 表示折抵 500 元）。
 */
public enum DiscountType {

    /** 百分比折扣（discountValue 為 0~100 的百分比數值） */
    PERCENTAGE,

    /** 固定金額折扣（discountValue 為 TWD 金額） */
    FIXED
}
