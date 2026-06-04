package com.shoppingcart.backend.controller;

import com.shoppingcart.backend.dto.ProductResponse;
import com.shoppingcart.backend.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 商品 REST Controller，提供商品查詢相關 API 端點。
 * 基底路徑：{@code /api/products}。
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    /** 商品業務邏輯層，透過建構子注入 */
    private final ProductService productService;

    /**
     * 建構子注入 ProductService。
     *
     * @param productService 商品服務層
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 取得所有商品列表。
     * 對應 api.md §3.1：{@code GET /api/products}，成功回傳 HTTP 200。
     *
     * @return 包含所有商品 DTO 列表的 200 回應
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * 依 UUID 取得單一商品詳細資訊。
     * 對應 api.md §3.2：{@code GET /api/products/{id}}，
     * 成功回傳 HTTP 200；商品不存在時由 GlobalExceptionHandler 轉為 HTTP 404。
     *
     * @param id 商品 UUID（路徑參數）
     * @return 包含商品 DTO 的 200 回應
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }
}
