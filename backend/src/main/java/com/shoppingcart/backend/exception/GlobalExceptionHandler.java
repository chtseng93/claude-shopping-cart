package com.shoppingcart.backend.exception;

import com.shoppingcart.backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * 全域例外處理器，使用 @RestControllerAdvice 統一攔截所有 Controller 層例外，
 * 並轉換為符合 api.md §1.5 規格的 ErrorResponse 格式回傳。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 處理資源不存在例外（NotFoundException）→ HTTP 404 Not Found。
     *
     * @param ex      拋出的 NotFoundException
     * @param request 當前 HTTP 請求（用於取得路徑）
     * @return 標準化錯誤回應
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * 處理請求不合法例外（BadRequestException）→ HTTP 400 Bad Request。
     *
     * @param ex      拋出的 BadRequestException
     * @param request 當前 HTTP 請求
     * @return 標準化錯誤回應
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * 處理庫存不足例外（InsufficientStockException）→ HTTP 400 Bad Request。
     *
     * @param ex      拋出的 InsufficientStockException
     * @param request 當前 HTTP 請求
     * @return 標準化錯誤回應
     */
    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientStockException(InsufficientStockException ex, HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * 處理 Bean Validation 驗證失敗例外（MethodArgumentNotValidException）→ HTTP 400 Bad Request。
     * 取第一個 FieldError 的 defaultMessage 作為錯誤訊息回傳。
     *
     * @param ex      Spring 產生的驗證失敗例外
     * @param request 當前 HTTP 請求
     * @return 標準化錯誤回應，message 為第一個欄位的驗證訊息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // 取第一個 FieldError 的 defaultMessage；若無則使用通用訊息
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("請求參數驗證失敗");

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );
    }

    /**
     * 處理所有未預期例外（Exception）→ HTTP 500 Internal Server Error。
     * 回傳固定訊息「內部伺服器錯誤」，避免洩漏內部實作細節。
     *
     * @param ex      未預期的例外
     * @param request 當前 HTTP 請求
     * @return 標準化錯誤回應
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "內部伺服器錯誤",
                request.getRequestURI()
        );
    }

    // ── 私有輔助方法 ────────────────────────────────────────────────────────

    /**
     * 建立統一格式的 ErrorResponse。
     *
     * @param status  HTTP 狀態
     * @param message 錯誤訊息
     * @param path    請求路徑
     * @return 填入 timestamp、status、error、message、path 的回應物件
     */
    private ErrorResponse buildErrorResponse(HttpStatus status, String message, String path) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }
}
