package com.skiresort.inventory.service;

import com.skiresort.inventory.event.*;
import com.skiresort.inventory.model.Equipment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 設備管理サービス
 */
@ApplicationScoped
public class EquipmentService {
    
    @PersistenceContext
    EntityManager entityManager;
    
    private static final Logger logger = Logger.getLogger(EquipmentService.class.getName());
    
    /**
     * 商品イベントから設備を作成
     */
    @Transactional
    public void createEquipmentFromProduct(ProductCreatedEvent event) {
        // 既存の設備をチェック（重複防止）
        Equipment existingEquipment = findByProductId(event.getProductId());
        if (existingEquipment != null) {
            logger.warning("Equipment already exists for product: " + event.getProductId() + ", updating instead");
            updateEquipmentCacheFromEvent(existingEquipment, event);
            return;
        }
        
        Equipment equipment = new Equipment();
        equipment.setProductId(event.getProductId());
        equipment.setDailyRate(Equipment.calculateDailyRate(event.getBasePrice(), event.getEquipmentType()));
        equipment.setRentalAvailable(event.isRentalAvailable());
        equipment.setActive(event.isActive());
        
        // キャッシュデータ設定
        updateCacheFromCreatedEvent(equipment, event);
        
        // 旧フィールドも一時的に設定（段階的移行用）
        updateLegacyFieldsFromEvent(equipment, event);
        
        entityManager.persist(equipment);
        
        logger.info("Created new equipment for product: " + event.getProductId() + " with daily rate: " + equipment.getDailyRate());
    }
    
    /**
     * 商品更新イベントから設備を更新
     */
    @Transactional
    public void updateEquipmentFromProduct(ProductUpdatedEvent event) {
        Equipment equipment = findByProductId(event.getProductId());
        if (equipment == null) {
            logger.warning("Equipment not found for product update: " + event.getProductId());
            return;
        }
        
        // 変更されたフィールドのみ更新
        if (event.getChangedFields().containsKey("name")) {
            equipment.setCachedName(event.getName());
            equipment.setName(event.getName()); // 旧フィールドも更新
        }
        if (event.getChangedFields().containsKey("description")) {
            equipment.setCachedDescription(event.getDescription());
            equipment.setDescription(event.getDescription()); // 旧フィールドも更新
        }
        if (event.getChangedFields().containsKey("basePrice")) {
            equipment.setCachedBasePrice(event.getBasePrice());
            // レンタル料金を再計算
            equipment.setDailyRate(Equipment.calculateDailyRate(event.getBasePrice(), equipment.getCachedEquipmentType()));
        }
        if (event.getChangedFields().containsKey("isActive")) {
            equipment.setActive(event.isActive());
        }
        if (event.getChangedFields().containsKey("categoryName")) {
            equipment.setCachedCategory(event.getCategoryName());
            equipment.setCategory(event.getCategoryName()); // 旧フィールドも更新
        }
        if (event.getChangedFields().containsKey("brandName")) {
            equipment.setCachedBrand(event.getBrandName());
            equipment.setBrand(event.getBrandName()); // 旧フィールドも更新
        }
        if (event.getChangedFields().containsKey("imageUrl")) {
            equipment.setCachedImageUrl(event.getImageUrl());
            equipment.setImageUrl(event.getImageUrl()); // 旧フィールドも更新
        }
        
        equipment.setCacheUpdatedAt(LocalDateTime.now());
        
        entityManager.merge(equipment);
        
        logger.info("Updated equipment cache for product: " + event.getProductId());
    }
    
    /**
     * 設備を無効化
     */
    @Transactional
    public void deactivateEquipment(UUID productId) {
        Equipment equipment = findByProductId(productId);
        if (equipment != null) {
            equipment.setActive(false);
            entityManager.merge(equipment);
            logger.info("Deactivated equipment for product: " + productId);
        } else {
            logger.warning("Equipment not found for deactivation: " + productId);
        }
    }
    
    /**
     * 設備を有効化
     */
    @Transactional
    public void activateEquipment(UUID productId) {
        Equipment equipment = findByProductId(productId);
        if (equipment != null) {
            equipment.setActive(true);
            entityManager.merge(equipment);
            logger.info("Activated equipment for product: " + productId);
        } else {
            logger.warning("Equipment not found for activation: " + productId);
        }
    }
    
    /**
     * 商品IDで設備を検索
     */
    public Equipment findByProductId(UUID productId) {
        try {
            return entityManager.createNamedQuery("Equipment.findByProductId", Equipment.class)
                    .setParameter("productId", productId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * SKUで設備を検索
     */
    public Equipment findBySku(String sku) {
        try {
            return entityManager.createNamedQuery("Equipment.findBySku", Equipment.class)
                    .setParameter("sku", sku)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * レンタル可能な設備一覧を取得
     */
    public List<Equipment> findRentalAvailable() {
        return entityManager.createNamedQuery("Equipment.findRentalAvailable", Equipment.class)
                .getResultList();
    }
    
    /**
     * 設備タイプ別一覧を取得
     */
    public List<Equipment> findByEquipmentType(String equipmentType) {
        return entityManager.createNamedQuery("Equipment.findByEquipmentType", Equipment.class)
                .setParameter("equipmentType", equipmentType)
                .getResultList();
    }
    
    /**
     * 全設備一覧を取得
     */
    public List<Equipment> findAll() {
        return entityManager.createQuery("SELECT e FROM Equipment e", Equipment.class)
                .getResultList();
    }
    
    // プライベートヘルパーメソッド
    
    private void updateCacheFromCreatedEvent(Equipment equipment, ProductCreatedEvent event) {
        equipment.setCachedSku(event.getSku());
        equipment.setCachedName(event.getName());
        equipment.setCachedCategory(event.getCategoryName());
        equipment.setCachedBrand(event.getBrandName());
        equipment.setCachedEquipmentType(event.getEquipmentType());
        equipment.setCachedSizeRange(event.getSizeRange());
        equipment.setCachedDifficultyLevel(event.getDifficultyLevel());
        equipment.setCachedBasePrice(event.getBasePrice());
        equipment.setCachedDescription(event.getDescription());
        equipment.setCachedImageUrl(event.getImageUrl());
        equipment.setCacheUpdatedAt(LocalDateTime.now());
    }
    
    private void updateEquipmentCacheFromEvent(Equipment equipment, ProductCreatedEvent event) {
        updateCacheFromCreatedEvent(equipment, event);
        updateLegacyFieldsFromEvent(equipment, event);
        equipment.setDailyRate(Equipment.calculateDailyRate(event.getBasePrice(), event.getEquipmentType()));
        equipment.setRentalAvailable(event.isRentalAvailable());
        equipment.setActive(event.isActive());
        entityManager.merge(equipment);
    }
    
    /**
     * 段階的移行用：旧フィールドも更新
     */
    private void updateLegacyFieldsFromEvent(Equipment equipment, ProductCreatedEvent event) {
        equipment.setSku(event.getSku());
        equipment.setName(event.getName());
        equipment.setCategory(event.getCategoryName());
        equipment.setBrand(event.getBrandName());
        equipment.setEquipmentType(event.getEquipmentType());
        equipment.setSizeRange(event.getSizeRange());
        equipment.setDifficultyLevel(event.getDifficultyLevel());
        equipment.setDescription(event.getDescription());
        equipment.setImageUrl(event.getImageUrl());
    }
}