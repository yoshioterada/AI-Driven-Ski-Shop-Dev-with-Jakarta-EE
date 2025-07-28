package com.skiresort.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 設備マスタエンティティ（商品カタログサービスと連携）
 */
@Entity
@Table(name = "equipment")
@NamedQueries({
    @NamedQuery(
        name = "Equipment.findByProductId",
        query = "SELECT e FROM Equipment e WHERE e.productId = :productId"
    ),
    @NamedQuery(
        name = "Equipment.findBySku",
        query = "SELECT e FROM Equipment e WHERE e.cachedSku = :sku"
    ),
    @NamedQuery(
        name = "Equipment.findByEquipmentType",
        query = "SELECT e FROM Equipment e WHERE e.cachedEquipmentType = :equipmentType AND e.isActive = true"
    ),
    @NamedQuery(
        name = "Equipment.findRentalAvailable",
        query = "SELECT e FROM Equipment e WHERE e.isRentalAvailable = true AND e.isActive = true"
    )
})
public class Equipment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "equipment_seq")
    @SequenceGenerator(name = "equipment_seq", sequenceName = "equipment_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;
    
    @NotNull
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    // 旧フィールド（段階的移行のため一時的に保持）
    @Column(name = "sku", length = 100)
    private String sku;
    
    @Column(name = "name", length = 200)
    private String name;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "brand", length = 100)
    private String brand;
    
    @Column(name = "equipment_type", length = 50)
    private String equipmentType;
    
    @Column(name = "size_range", length = 50)
    private String sizeRange;
    
    @Column(name = "difficulty_level", length = 20)
    private String difficultyLevel;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    // ビジネス固有フィールド
    @NotNull
    @Column(name = "daily_rate", precision = 10, scale = 2, nullable = false)
    private BigDecimal dailyRate;
    
    @Column(name = "is_rental_available", nullable = false)
    private boolean isRentalAvailable = true;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // キャッシュフィールド（商品カタログサービスから同期）
    @Column(name = "cached_sku", length = 100)
    private String cachedSku;
    
    @Column(name = "cached_name", length = 200)
    private String cachedName;
    
    @Column(name = "cached_category", length = 100)
    private String cachedCategory;
    
    @Column(name = "cached_brand", length = 100)
    private String cachedBrand;
    
    @Column(name = "cached_equipment_type", length = 50)
    private String cachedEquipmentType;
    
    @Column(name = "cached_size_range", length = 50)
    private String cachedSizeRange;
    
    @Column(name = "cached_difficulty_level", length = 20)
    private String cachedDifficultyLevel;
    
    @Column(name = "cached_base_price", precision = 10, scale = 2)
    private BigDecimal cachedBasePrice;
    
    @Column(name = "cached_description", columnDefinition = "TEXT")
    private String cachedDescription;
    
    @Column(name = "cached_image_url", length = 500)
    private String cachedImageUrl;
    
    @Column(name = "cache_updated_at", nullable = false)
    private LocalDateTime cacheUpdatedAt;
    
    // コンストラクタ
    public Equipment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.cacheUpdatedAt = LocalDateTime.now();
    }
    
    public Equipment(UUID productId, BigDecimal dailyRate) {
        this();
        this.productId = productId;
        this.dailyRate = dailyRate;
    }
    
    // ライフサイクルコールバック
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // ビジネスメソッド
    
    /**
     * レンタル料金を計算（商品タイプに応じた計算）
     */
    public static BigDecimal calculateDailyRate(BigDecimal basePrice, String equipmentType) {
        if (basePrice == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal rate = basePrice.multiply(BigDecimal.valueOf(0.1)); // 基本10%
        
        return switch (equipmentType) {
            case "SKI_BOARD" -> rate.multiply(BigDecimal.valueOf(1.2)); // 20%増し
            case "BOOT" -> rate.multiply(BigDecimal.valueOf(1.1)); // 10%増し
            case "HELMET" -> rate.multiply(BigDecimal.valueOf(0.8)); // 20%減
            case "POLE" -> rate.multiply(BigDecimal.valueOf(0.6)); // 40%減
            case "GOGGLE" -> rate.multiply(BigDecimal.valueOf(0.5)); // 50%減
            case "GLOVE" -> rate.multiply(BigDecimal.valueOf(0.4)); // 60%減
            default -> rate;
        };
    }
    
    /**
     * レンタル対象商品かどうか判定
     */
    public static boolean isRentalEligible(String equipmentType) {
        return !"WAX".equals(equipmentType) && !"TUNING".equals(equipmentType);
    }
    
    // Getter and Setter methods
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public UUID getProductId() {
        return productId;
    }
    
    public void setProductId(UUID productId) {
        this.productId = productId;
    }
    
    // 旧フィールドのGetters/Setters（段階的移行用）
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
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
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
    
    // ビジネス固有フィールドのGetters/Setters
    public BigDecimal getDailyRate() {
        return dailyRate;
    }
    
    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // キャッシュフィールドのGetters/Setters
    public String getCachedSku() {
        return cachedSku;
    }
    
    public void setCachedSku(String cachedSku) {
        this.cachedSku = cachedSku;
    }
    
    public String getCachedName() {
        return cachedName;
    }
    
    public void setCachedName(String cachedName) {
        this.cachedName = cachedName;
    }
    
    public String getCachedCategory() {
        return cachedCategory;
    }
    
    public void setCachedCategory(String cachedCategory) {
        this.cachedCategory = cachedCategory;
    }
    
    public String getCachedBrand() {
        return cachedBrand;
    }
    
    public void setCachedBrand(String cachedBrand) {
        this.cachedBrand = cachedBrand;
    }
    
    public String getCachedEquipmentType() {
        return cachedEquipmentType;
    }
    
    public void setCachedEquipmentType(String cachedEquipmentType) {
        this.cachedEquipmentType = cachedEquipmentType;
    }
    
    public String getCachedSizeRange() {
        return cachedSizeRange;
    }
    
    public void setCachedSizeRange(String cachedSizeRange) {
        this.cachedSizeRange = cachedSizeRange;
    }
    
    public String getCachedDifficultyLevel() {
        return cachedDifficultyLevel;
    }
    
    public void setCachedDifficultyLevel(String cachedDifficultyLevel) {
        this.cachedDifficultyLevel = cachedDifficultyLevel;
    }
    
    public BigDecimal getCachedBasePrice() {
        return cachedBasePrice;
    }
    
    public void setCachedBasePrice(BigDecimal cachedBasePrice) {
        this.cachedBasePrice = cachedBasePrice;
    }
    
    public String getCachedDescription() {
        return cachedDescription;
    }
    
    public void setCachedDescription(String cachedDescription) {
        this.cachedDescription = cachedDescription;
    }
    
    public String getCachedImageUrl() {
        return cachedImageUrl;
    }
    
    public void setCachedImageUrl(String cachedImageUrl) {
        this.cachedImageUrl = cachedImageUrl;
    }
    
    public LocalDateTime getCacheUpdatedAt() {
        return cacheUpdatedAt;
    }
    
    public void setCacheUpdatedAt(LocalDateTime cacheUpdatedAt) {
        this.cacheUpdatedAt = cacheUpdatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Equipment equipment)) return false;
        return id != null && id.equals(equipment.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Equipment{" +
                "id=" + id +
                ", productId=" + productId +
                ", cachedSku='" + cachedSku + '\'' +
                ", cachedName='" + cachedName + '\'' +
                ", dailyRate=" + dailyRate +
                ", isRentalAvailable=" + isRentalAvailable +
                ", isActive=" + isActive +
                '}';
    }
}