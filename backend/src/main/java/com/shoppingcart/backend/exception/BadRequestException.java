package com.shoppingcart.backend.exception;

/**
 * 請求參數不合法例外（對應 HTTP 400 Bad Request）。
 * 當輸入資料不符合業務規則，例如數量小於零、購物車為空時拋出。
 */
public class BadRequestException extends RuntimeException {

    /**
     * 建立例外並帶入說明訊息。
     *
     * @param message 錯誤說明，例如「購物車為空」
     */
    public BadRequestException(String message) {
        super(message);
    }
}
