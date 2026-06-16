package com.shoppingcart.backend.service;

import com.shoppingcart.backend.entity.Cart;
import com.shoppingcart.backend.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 廢棄購物車排程清理服務。
 * 每天凌晨 3 點自動刪除超過 30 天未結帳的購物車，
 * 避免 cart 與 cart_item 資料表長期累積孤兒紀錄。
 *
 * <p>Cart entity 設有 cascade=ALL、orphanRemoval=true，
 * 刪除 Cart 時其所屬 CartItem 會一併由 Hibernate 連帶刪除。</p>
 */
@Service
public class CartCleanupService {

    private static final Logger log = LoggerFactory.getLogger(CartCleanupService.class);

    /** 廢棄判定天數：超過此天數且尚未結帳，視為廢棄購物車 */
    static final long ABANDONED_DAYS = 30;

    private final CartRepository cartRepository;

    public CartCleanupService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    /**
     * 定期清除廢棄購物車（每天凌晨 3 點執行）。
     * 條件：建立時間早於 30 天前，且 checked_out_at 為 NULL。
     * Cart 的 cascade=ALL 設定確保 CartItem 一併刪除。
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeAbandonedCarts() {
        Instant cutoff = Instant.now().minus(ABANDONED_DAYS, ChronoUnit.DAYS);
        List<Cart> abandoned = cartRepository.findByCheckedOutAtIsNullAndCreatedAtBefore(cutoff);

        if (abandoned.isEmpty()) {
            log.info("[CartCleanup] 無廢棄購物車需清理");
            return;
        }

        cartRepository.deleteAll(abandoned);
        log.info("[CartCleanup] 已清除 {} 筆廢棄購物車（超過 {} 天未結帳）",
                abandoned.size(), ABANDONED_DAYS);
    }
}
