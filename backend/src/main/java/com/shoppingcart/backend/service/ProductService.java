package com.shoppingcart.backend.service;

import com.shoppingcart.backend.dto.ProductResponse;
import com.shoppingcart.backend.exception.NotFoundException;
import com.shoppingcart.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 商品業務邏輯層，提供查詢商品列表與單一商品的服務方法。
 */
@Service
public class ProductService {

    /** 商品資料存取層，透過建構子注入 */
    private final ProductRepository productRepository;

    /**
     * 建構子注入 ProductRepository。
     *
     * @param productRepository 商品 JPA Repository
     */
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 取得所有商品列表，將全部 Product Entity 轉換為回應 DTO 後回傳。
     *
     * @return 所有商品的 ProductResponse 列表
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    /**
     * 依 UUID 查詢單一商品，若商品不存在則拋出 {@link NotFoundException}。
     *
     * @param id 商品 UUID
     * @return 對應商品的 ProductResponse
     * @throws NotFoundException 當指定 ID 的商品不存在時
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
    }
}
