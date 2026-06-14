package com.shoppingcart.backend.controller;

import com.shoppingcart.backend.dto.AddItemRequest;
import com.shoppingcart.backend.dto.CartResponse;
import com.shoppingcart.backend.dto.CheckoutRequest;
import com.shoppingcart.backend.dto.OrderSummary;
import com.shoppingcart.backend.dto.UpdateItemRequest;
import com.shoppingcart.backend.service.CartService;
import com.shoppingcart.backend.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 購物車 Controller，負責接收 HTTP 請求並委派給 CartService 處理業務邏輯。
 * 對應 api.md §4（購物車）與 §5（結帳）的所有端點。
 * sessionId 透過 {@link SessionUtils#getSessionId(HttpServletRequest)} 從
 * SessionFilter 預先設入的 request attribute 中取得。
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    /** 購物車服務，處理所有購物車業務邏輯 */
    private final CartService cartService;

    /**
     * 建構子注入 CartService。
     *
     * @param cartService 購物車服務
     */
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // ── T05：查詢購物車 ──────────────────────────────────────────────────

    /**
     * 取得當前訪客的購物車（含 total）。
     * 購物車為空時 items 為空清單，total 為 0。
     *
     * @param request HTTP 請求（用於取得 sessionId）
     * @return 200 OK 購物車回應 DTO
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public CartResponse getCart(HttpServletRequest request) {
        String sessionId = SessionUtils.getSessionId(request);
        return cartService.getCart(sessionId);
    }

    // ── T06：加入商品 ────────────────────────────────────────────────────

    /**
     * 加入商品至購物車，相同商品自動合併數量。
     * 新明細會快照當下商品單價，後續商品調價不影響既有購物車。
     *
     * @param request HTTP 請求（用於取得 sessionId）
     * @param body    加入商品請求 DTO（含 productId 與 quantity）
     * @return 201 Created 更新後的完整購物車回應
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(HttpServletRequest request,
                                @RequestBody @Valid AddItemRequest body) {
        String sessionId = SessionUtils.getSessionId(request);
        return cartService.addToCart(sessionId, body.getProductId(), body.getQuantity());
    }

    // ── T07a：更新明細數量 ────────────────────────────────────────────────

    /**
     * 更新購物車明細數量。quantity = 0 時自動移除該明細，quantity > 0 則更新。
     * sessionId 傳入 CartService 以驗證明細所有權，防止 IDOR。
     *
     * @param request HTTP 請求（用於取得 sessionId）
     * @param itemId  路徑參數，購物車明細 UUID
     * @param body    更新請求 DTO（quantity ≥ 0）
     * @return 200 OK 更新後的完整購物車回應
     */
    @PatchMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    public CartResponse updateItem(HttpServletRequest request,
                                   @PathVariable UUID itemId,
                                   @RequestBody @Valid UpdateItemRequest body) {
        String sessionId = SessionUtils.getSessionId(request);
        return cartService.updateItem(itemId, body.getQuantity(), sessionId);
    }

    // ── T07b：移除明細 ────────────────────────────────────────────────────

    /**
     * 直接移除指定購物車明細（無論數量為何）。
     * sessionId 傳入 CartService 以驗證明細所有權，防止 IDOR。
     *
     * @param request HTTP 請求（用於取得 sessionId）
     * @param itemId  路徑參數，購物車明細 UUID
     * @return 200 OK 移除後的完整購物車回應
     */
    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    public CartResponse removeItem(HttpServletRequest request,
                                   @PathVariable UUID itemId) {
        String sessionId = SessionUtils.getSessionId(request);
        return cartService.removeItem(itemId, sessionId);
    }

    // ── T08：結帳 ────────────────────────────────────────────────────────

    /**
     * 結帳端點：驗證收件資料、套用優惠券（選填）、檢查庫存、扣減庫存、標記購物車已結帳，回傳訂單摘要。
     * 採 @Transactional 保護，庫存不足時整筆 rollback。
     * couponCode 為選填，若傳入則驗證優惠券並計算折扣，折扣金額由伺服器計算。
     *
     * @param request HTTP 請求（用於取得 sessionId）
     * @param body    結帳請求 DTO（含 recipient 收件資料與選填 couponCode，@Valid 觸發巢狀驗證）
     * @return 200 OK 訂單摘要 DTO（含收件資料、明細清單、合計金額與折扣資訊）
     */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.OK)
    public OrderSummary checkout(HttpServletRequest request,
                                 @RequestBody @Valid CheckoutRequest body) {
        String sessionId = SessionUtils.getSessionId(request);
        return cartService.checkout(sessionId, body.getRecipient(), body.getCouponCode());
    }
}
