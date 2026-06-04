package com.shoppingcart.backend.repository;

import com.shoppingcart.backend.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 商品資料存取層，繼承 JpaRepository 以取得基本 CRUD 操作。
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * 以悲觀寫入鎖定（SELECT ... FOR UPDATE）方式查詢商品。
     * 供結帳流程在扣減庫存前鎖定該列，防止並發超賣。
     *
     * @param id 商品 UUID
     * @return 鎖定中的商品（若存在）
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findWithLockById(@Param("id") UUID id);
}
