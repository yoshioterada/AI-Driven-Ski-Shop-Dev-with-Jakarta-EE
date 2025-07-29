package com.ski.shop.catalog.event;

import com.ski.shop.catalog.domain.Product;
import com.ski.shop.catalog.domain.ProductImage;
import com.ski.shop.catalog.domain.SkiType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 商品作成イベント
 */
public class ProductCreatedEvent extends ProductEvent {
    
    public UUID productId;
    public String sku;
    public String name;
    public String categoryName;
    public String brandName;
    public String equipmentType; // SKI_BOARD, BOOT, HELMET, etc.
    public String sizeRange;
    public String difficultyLevel;
    public BigDecimal basePrice;
    public String description;
    public String imageUrl;
    public boolean isRentalAvailable;
    public boolean isActive;
    
    public ProductCreatedEvent() {
        super();
    }
    
    public ProductCreatedEvent(Product product) {
        super("PRODUCT_CREATED", product.id.toString(), 1);
        this.productId = product.id;
        this.sku = product.sku;
        this.name = product.name;
        this.categoryName = product.category.name;
        this.brandName = product.brand.name;
        this.equipmentType = mapToEquipmentType(product.skiType);
        this.sizeRange = buildSizeRange(product);
        this.difficultyLevel = product.difficultyLevel.name();
        this.basePrice = product.basePrice;
        this.description = product.description;
        this.imageUrl = extractMainImageUrl(product);
        this.isRentalAvailable = determineRentalAvailability(product);
        this.isActive = product.isActive;
    }
    
    // Helper methods
    private String mapToEquipmentType(SkiType skiType) {
        // Product Catalog の SkiType を Inventory の EquipmentType にマッピング
        switch (skiType) {
            case SKI_BOARD: return "SKI_BOARD";
            case BINDING: return "BINDING";
            case POLE: return "POLE";
            case BOOT: return "BOOT";
            case HELMET: return "HELMET";
            case PROTECTOR: return "PROTECTOR";
            case WEAR: return "WEAR";
            case GOGGLE: return "GOGGLE";
            case GLOVE: return "GLOVE";
            case BAG: return "BAG";
            case WAX: return "WAX";
            case TUNING: return "TUNING";
            default: return "OTHER";
        }
    }
    
    private String buildSizeRange(Product product) {
        // 商品の仕様から適切なサイズ範囲を構築
        if (product.length != null && !product.length.isEmpty()) {
            return product.length;
        }
        // カテゴリに基づいてデフォルトサイズ範囲を設定
        return switch (product.category.path) {
            case "/ski-board" -> "150-190cm";
            case "/boot" -> "22.0-30.0cm";
            case "/helmet", "/goggle", "/glove", "/wear" -> "S-XL";
            case "/pole" -> "100-140cm";
            default -> "ONE_SIZE";
        };
    }
    
    private String extractMainImageUrl(Product product) {
        // 商品画像からメイン画像のURLを取得
        if (product.images != null && !product.images.isEmpty()) {
            return product.images.stream()
                    .filter(img -> img.isPrimary)
                    .findFirst()
                    .map(img -> img.imageUrl)
                    .orElse(product.images.get(0).imageUrl);
        }
        return "/images/default-product.jpg";
    }
    
    private boolean determineRentalAvailability(Product product) {
        // ワックスやチューンナップ用品はレンタル対象外
        return !product.category.path.contains("/wax") && 
               !product.category.path.contains("/tuning");
    }
    
    // Getters and setters
    public UUID getProductId() {
        return productId;
    }
    
    public void setProductId(UUID productId) {
        this.productId = productId;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public String getBrandName() {
        return brandName;
    }
    
    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }
    
    public String getEquipmentType() {
        return equipmentType;
    }
    
    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }
    
    public String getSizeRange() {
        return sizeRange;
    }
    
    public void setSizeRange(String sizeRange) {
        this.sizeRange = sizeRange;
    }
    
    public String getDifficultyLevel() {
        return difficultyLevel;
    }
    
    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }
    
    public BigDecimal getBasePrice() {
        return basePrice;
    }
    
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public boolean isRentalAvailable() {
        return isRentalAvailable;
    }
    
    public void setRentalAvailable(boolean rentalAvailable) {
        isRentalAvailable = rentalAvailable;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
}