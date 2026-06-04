package com.shoppingcart.backend.service;

import com.shoppingcart.backend.dto.CartResponse;
import com.shoppingcart.backend.dto.ItemDto;
import com.shoppingcart.backend.dto.OrderSummary;
import com.shoppingcart.backend.dto.RecipientDto;
import com.shoppingcart.backend.entity.Cart;
import com.shoppingcart.backend.entity.CartItem;
import com.shoppingcart.backend.entity.Product;
import com.shoppingcart.backend.exception.BadRequestException;
import com.shoppingcart.backend.exception.InsufficientStockException;
import com.shoppingcart.backend.exception.NotFoundException;
import com.shoppingcart.backend.repository.CartItemRepository;
import com.shoppingcart.backend.repository.CartRepository;
import com.shoppingcart.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 購物車核心服務，包含 T05~T08 全部業務邏輯：
 * <ul>
 *   <li>T05：查詢當前購物車（GET /api/cart）</li>
 *   <li>T06：加入商品，自動合併相同商品數量，快照當下單價（POST /api/cart/items）</li>
 *   <li>T07：更新明細數量（數量為 0 自動移除）或直接移除明細（PATCH / DELETE）</li>
 *   <li>T08：結帳，@Transactional 保護庫存扣減一致性（POST /api/cart/checkout）</li>
 * </ul>
 *
 * <p>所有金額計算（subtotal、total）均由 {@link #buildCartResponse(Cart)} 在伺服器端執行，
 * 不接受客戶端傳入金額，符合「金額權威在伺服器」原則。</p>
 */
@Service
public class CartService {

    /** 購物車資料存取層 */
    private final CartRepository cartRepository;

    /** 購物車明細資料存取層 */
    private final CartItemRepository cartItemRepository;

    /** 商品資料存取層（含悲觀鎖查詢） */
    private final ProductRepository productRepository;

    /**
     * 建構子注入所有依賴的 Repository。
     *
     * @param cartRepository     購物車 Repository
     * @param cartItemRepository 購物車明細 Repository
     * @param productRepository  商品 Repository
     */
    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    // ── T05：查詢購物車 ──────────────────────────────────────────────────

    /**
     * 取得指定 sessionId 對應的活躍購物車。
     * 若該 session 尚無購物車，回傳空購物車回應（items 為空清單，total 為 0）。
     *
     * @param sessionId 訪客 cookie 中的 session 識別碼
     * @return 購物車回應 DTO（含伺服器計算的合計）
     */
    public CartResponse getCart(String sessionId) {
        return cartRepository.findBySessionIdAndCheckedOutAtIsNull(sessionId)
                .map(this::buildCartResponse)
                .orElseGet(() -> {
                    // 尚無購物車時，回傳虛擬空購物車（不建立 DB 紀錄）
                    // cartId 以 nil UUID 表示「尚無購物車」，前端應能處理此情況
                    return new CartResponse(null, List.of(), 0, BigDecimal.ZERO);
                });
    }

    // ── T06：加入商品 ────────────────────────────────────────────────────

    /**
     * 將商品加入購物車，若該 session 無購物車則自動建立。
     * 相同商品（productId）已存在時自動合併數量；新商品則建立明細並快照當下單價。
     *
     * @param sessionId 訪客 session 識別碼
     * @param productId 欲加入的商品 UUID
     * @param quantity  加入數量（≥ 1，由 Controller 層 @Valid 確保）
     * @return 更新後的完整購物車回應（含伺服器計算合計）
     * @throws NotFoundException 若商品不存在
     */
    @Transactional
    public CartResponse addToCart(String sessionId, UUID productId, int quantity) {
        // 取得或建立活躍購物車
        Cart cart = cartRepository.findBySessionIdAndCheckedOutAtIsNull(sessionId)
                .orElseGet(() -> cartRepository.save(new Cart(sessionId)));

        // 驗證商品存在
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("商品不存在"));

        // 判斷同商品是否已在購物車：存在則合併數量，否則建立新明細並快照單價
        cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .ifPresentOrElse(
                        existingItem -> {
                            // 已存在：累加數量（自動合併）
                            existingItem.setQuantity(existingItem.getQuantity() + quantity);
                        },
                        () -> {
                            // 不存在：新增明細，以商品當下 price 快照單價
                            CartItem newItem = new CartItem(cart, product, quantity, product.getPrice());
                            cartItemRepository.save(newItem);
                        }
                );

        // 由伺服器重新計算並回傳完整購物車（金額權威在伺服器）
        return buildCartResponse(cart);
    }

    // ── T07a：更新明細數量 ────────────────────────────────────────────────

    /**
     * 更新指定購物車明細的數量。
     * 若 quantity = 0，則自動移除該明細；quantity > 0 則更新為指定數量。
     *
     * @param itemId   購物車明細 UUID
     * @param quantity 新數量（≥ 0；0 表示移除，由 @Valid 確保不為負）
     * @return 更新後的完整購物車回應
     * @throws NotFoundException 若明細不存在
     */
    @Transactional
    public CartResponse updateItem(UUID itemId, int quantity) {
        // 查詢明細，不存在則拋 404
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("購物車明細不存在"));

        Cart cart = item.getCart();

        if (quantity == 0) {
            // 數量為 0：自動移除該明細
            cartItemRepository.delete(item);
        } else {
            // 數量 > 0：更新為指定數量
            item.setQuantity(quantity);
        }

        // 重新計算並回傳完整購物車
        return buildCartResponse(cart);
    }

    // ── T07b：直接移除明細 ───────────────────────────────────────────────

    /**
     * 直接移除指定的購物車明細（無論數量為何）。
     *
     * @param itemId 購物車明細 UUID
     * @return 移除後的完整購物車回應
     * @throws NotFoundException 若明細不存在
     */
    @Transactional
    public CartResponse removeItem(UUID itemId) {
        // 查詢明細，不存在則拋 404
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("購物車明細不存在"));

        Cart cart = item.getCart();

        // 直接刪除明細
        cartItemRepository.delete(item);

        // 重新計算並回傳完整購物車
        return buildCartResponse(cart);
    }

    // ── T08：結帳 ────────────────────────────────────────────────────────

    /**
     * 結帳流程（@Transactional 保護整筆交易一致性）：
     * <ol>
     *   <li>查詢活躍購物車，找不到拋 BadRequestException</li>
     *   <li>確認購物車非空，否則拋 BadRequestException("購物車為空")</li>
     *   <li>逐項以 SELECT FOR UPDATE 鎖定商品列，驗證庫存 ≥ 需求數量</li>
     *   <li>庫存不足則拋 InsufficientStockException，觸發整筆 rollback</li>
     *   <li>扣減各商品庫存</li>
     *   <li>標記 cart.checkedOutAt = now()（購物車視為已結帳）</li>
     *   <li>回傳訂單摘要（含收件資料、明細、合計）</li>
     * </ol>
     *
     * @param sessionId 訪客 session 識別碼
     * @param recipient 收件人資料（由 Controller 層 @Valid 已完成格式驗證）
     * @return 訂單摘要 DTO
     * @throws BadRequestException       若找不到活躍購物車或購物車為空
     * @throws InsufficientStockException 若任一商品庫存不足（觸發 rollback）
     */
    @Transactional
    public OrderSummary checkout(String sessionId, RecipientDto recipient) {
        // 步驟 1：查詢活躍購物車
        Cart cart = cartRepository.findBySessionIdAndCheckedOutAtIsNull(sessionId)
                .orElseThrow(() -> new BadRequestException("找不到有效的購物車"));

        // 步驟 2：確認購物車非空
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new BadRequestException("購物車為空");
        }

        // 步驟 3 & 4 & 5：逐項鎖定商品、驗證庫存、扣減庫存
        for (CartItem cartItem : items) {
            // SELECT ... FOR UPDATE：悲觀鎖定商品列，防止並發超賣
            Product product = productRepository.findWithLockById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new NotFoundException("商品不存在：" + cartItem.getProduct().getId()));

            // 驗證庫存是否足夠
            if (product.getStock() < cartItem.getQuantity()) {
                // 庫存不足：拋例外觸發整筆交易 rollback
                throw new InsufficientStockException("庫存不足");
            }

            // 庫存充足：扣減庫存
            product.setStock(product.getStock() - cartItem.getQuantity());
        }

        // 步驟 6：標記購物車已結帳（checkedOutAt = now()）
        Instant checkedOutAt = Instant.now();
        cart.setCheckedOutAt(checkedOutAt);

        // 步驟 7：建立並回傳訂單摘要
        List<ItemDto> itemDtos = items.stream()
                .map(ItemDto::from)
                .toList();

        BigDecimal total = itemDtos.stream()
                .map(ItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderSummary(cart.getId(), checkedOutAt, recipient, itemDtos, total);
    }

    // ── 私有輔助方法 ──────────────────────────────────────────────────────

    /**
     * 建立購物車回應 DTO，subtotal 與 total 一律由伺服器計算（金額權威原則）。
     * 透過 CartItemRepository 查詢最新明細清單，確保反映最新狀態。
     *
     * @param cart 購物車 Entity
     * @return 含伺服器計算合計的 CartResponse
     */
    private CartResponse buildCartResponse(Cart cart) {
        // 從 DB 取得最新明細清單（確保資料一致性）
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return CartResponse.from(cart, items);
    }
}
