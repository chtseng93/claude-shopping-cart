package com.shoppingcart.backend.exception;

/**
 * 資源不存在例外（對應 HTTP 404 Not Found）。
 * 當查詢的商品、購物車明細等資源不存在時拋出。
 */
public class NotFoundException extends RuntimeException {

    /**
     * 建立例外並帶入說明訊息。
     *
     * @param message 錯誤說明，例如「商品不存在」
     */
    public NotFoundException(String message) {
        super(message);
    }
}
