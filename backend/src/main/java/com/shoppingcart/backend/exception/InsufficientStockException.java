package com.shoppingcart.backend.exception;

/**
 * 庫存不足例外（對應 HTTP 400 Bad Request）。
 * 結帳時若任一商品的庫存數量小於購物車需求數量即拋出，
 * 並觸發整筆交易 rollback。
 */
public class InsufficientStockException extends RuntimeException {

    /**
     * 建立例外並帶入說明訊息。
     *
     * @param message 錯誤說明，例如「庫存不足」
     */
    public InsufficientStockException(String message) {
        super(message);
    }
}
