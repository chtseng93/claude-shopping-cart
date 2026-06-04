package com.shoppingcart.backend.dto;

import java.time.Instant;

/**
 * 統一錯誤回應 DTO，對應 api.md §1.5 的錯誤格式。
 * 所有例外均由 GlobalExceptionHandler 轉換為此格式後回傳。
 */
public class ErrorResponse {

    /** 錯誤發生的 UTC 時間戳（ISO-8601 格式，例如 2026-06-03T08:00:00Z） */
    private Instant timestamp;

    /** HTTP 狀態碼數值，例如 400、404、500 */
    private int status;

    /** HTTP 狀態文字說明，例如 "Bad Request"、"Not Found" */
    private String error;

    /** 業務層錯誤訊息，例如「庫存不足」、「商品不存在」 */
    private String message;

    /** 觸發錯誤的請求路徑，例如 /api/cart/checkout */
    private String path;

    /**
     * 建立完整錯誤回應物件。
     *
     * @param timestamp 錯誤發生時間
     * @param status    HTTP 狀態碼
     * @param error     HTTP 狀態文字
     * @param message   業務錯誤訊息
     * @param path      請求路徑
     */
    public ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // ── Getter ──────────────────────────────────────────────────────────────

    /** 取得錯誤發生時間 */
    public Instant getTimestamp() {
        return timestamp;
    }

    /** 取得 HTTP 狀態碼 */
    public int getStatus() {
        return status;
    }

    /** 取得 HTTP 狀態文字 */
    public String getError() {
        return error;
    }

    /** 取得業務錯誤訊息 */
    public String getMessage() {
        return message;
    }

    /** 取得請求路徑 */
    public String getPath() {
        return path;
    }
}
