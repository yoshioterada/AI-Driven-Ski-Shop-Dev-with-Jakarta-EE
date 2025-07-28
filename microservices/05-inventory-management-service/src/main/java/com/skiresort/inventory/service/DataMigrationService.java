package com.skiresort.inventory.service;

import com.skiresort.inventory.model.Equipment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * データ移行サービス
 * 既存データをキャッシュフィールドに移行するためのサービス
 */
@ApplicationScoped
public class DataMigrationService {

    @PersistenceContext
    EntityManager entityManager;

    private static final Logger logger = Logger.getLogger(DataMigrationService.class.getName());

    /**
     * 既存の設備データをキャッシュフィールドに移行
     * 段階的移行時に実行
     */
    @Transactional
    public int migrateLegacyDataToCacheFields() {
        logger.info("Starting migration of legacy data to cache fields");

        @SuppressWarnings("unchecked")
        List<Equipment> equipments = entityManager.createQuery(
                "SELECT e FROM Equipment e WHERE " +
                "(e.cachedSku IS NULL OR e.cachedName IS NULL) AND " +
                "(e.sku IS NOT NULL AND e.name IS NOT NULL)"
        ).getResultList();

        int migratedCount = 0;

        for (Equipment equipment : equipments) {
            if (migrateSingleEquipment(equipment)) {
                migratedCount++;
            }
        }

        logger.info("Migration completed. Migrated " + migratedCount + " equipment records");
        return migratedCount;
    }

    /**
     * 単一設備の移行
     */
    private boolean migrateSingleEquipment(Equipment equipment) {
        try {
            // 旧フィールドからキャッシュフィールドにコピー
            if (equipment.getSku() != null && equipment.getCachedSku() == null) {
                equipment.setCachedSku(equipment.getSku());
            }
            if (equipment.getName() != null && equipment.getCachedName() == null) {
                equipment.setCachedName(equipment.getName());
            }
            if (equipment.getCategory() != null && equipment.getCachedCategory() == null) {
                equipment.setCachedCategory(equipment.getCategory());
            }
            if (equipment.getBrand() != null && equipment.getCachedBrand() == null) {
                equipment.setCachedBrand(equipment.getBrand());
            }
            if (equipment.getEquipmentType() != null && equipment.getCachedEquipmentType() == null) {
                equipment.setCachedEquipmentType(equipment.getEquipmentType());
            }
            if (equipment.getSizeRange() != null && equipment.getCachedSizeRange() == null) {
                equipment.setCachedSizeRange(equipment.getSizeRange());
            }
            if (equipment.getDifficultyLevel() != null && equipment.getCachedDifficultyLevel() == null) {
                equipment.setCachedDifficultyLevel(equipment.getDifficultyLevel());
            }
            if (equipment.getDescription() != null && equipment.getCachedDescription() == null) {
                equipment.setCachedDescription(equipment.getDescription());
            }
            if (equipment.getImageUrl() != null && equipment.getCachedImageUrl() == null) {
                equipment.setCachedImageUrl(equipment.getImageUrl());
            }

            equipment.setCacheUpdatedAt(LocalDateTime.now());
            entityManager.merge(equipment);

            logger.fine("Migrated equipment: " + equipment.getId() + " (" + equipment.getSku() + ")");
            return true;

        } catch (Exception e) {
            logger.severe("Failed to migrate equipment: " + equipment.getId() + ", error: " + e.getMessage());
            return false;
        }
    }

    /**
     * キャッシュデータの整合性チェックと修復
     */
    @Transactional
    public int validateAndRepairCacheData() {
        logger.info("Starting cache data validation and repair");

        @SuppressWarnings("unchecked")
        List<Equipment> equipments = entityManager.createQuery(
                "SELECT e FROM Equipment e WHERE e.cachedSku IS NOT NULL"
        ).getResultList();

        int repairedCount = 0;

        for (Equipment equipment : equipments) {
            if (repairCacheDataIfNeeded(equipment)) {
                repairedCount++;
            }
        }

        logger.info("Cache validation completed. Repaired " + repairedCount + " equipment records");
        return repairedCount;
    }

    /**
     * 必要に応じてキャッシュデータを修復
     */
    private boolean repairCacheDataIfNeeded(Equipment equipment) {
        boolean needsRepair = false;

        // 必須フィールドのチェック
        if (equipment.getCachedSku() == null || equipment.getCachedSku().trim().isEmpty()) {
            if (equipment.getSku() != null) {
                equipment.setCachedSku(equipment.getSku());
                needsRepair = true;
            }
        }

        if (equipment.getCachedName() == null || equipment.getCachedName().trim().isEmpty()) {
            if (equipment.getName() != null) {
                equipment.setCachedName(equipment.getName());
                needsRepair = true;
            }
        }

        // レンタル料金の再計算（base_priceがあり、equipment_typeが設定されている場合）
        if (equipment.getCachedBasePrice() != null && 
            equipment.getCachedEquipmentType() != null &&
            equipment.getDailyRate() != null) {
            
            var recalculatedRate = Equipment.calculateDailyRate(
                equipment.getCachedBasePrice(), 
                equipment.getCachedEquipmentType()
            );
            
            // 料金に大きな差がある場合は修正
            if (equipment.getDailyRate().subtract(recalculatedRate).abs().doubleValue() > 1.0) {
                equipment.setDailyRate(recalculatedRate);
                needsRepair = true;
            }
        }

        if (needsRepair) {
            equipment.setCacheUpdatedAt(LocalDateTime.now());
            entityManager.merge(equipment);
            logger.fine("Repaired cache data for equipment: " + equipment.getId());
        }

        return needsRepair;
    }

    /**
     * 移行状況の統計情報を取得
     */
    public MigrationStats getMigrationStats() {
        // 全設備数
        Long totalEquipment = entityManager.createQuery(
                "SELECT COUNT(e) FROM Equipment e", Long.class
        ).getSingleResult();

        // キャッシュデータがある設備数
        Long withCacheData = entityManager.createQuery(
                "SELECT COUNT(e) FROM Equipment e WHERE e.cachedSku IS NOT NULL", Long.class
        ).getSingleResult();

        // 旧データのみの設備数
        Long legacyDataOnly = entityManager.createQuery(
                "SELECT COUNT(e) FROM Equipment e WHERE e.cachedSku IS NULL AND e.sku IS NOT NULL", Long.class
        ).getSingleResult();

        return new MigrationStats(
                totalEquipment.intValue(),
                withCacheData.intValue(),
                legacyDataOnly.intValue()
        );
    }

    /**
     * 移行統計情報を表すクラス
     */
    public static class MigrationStats {
        private final int totalEquipment;
        private final int withCacheData;
        private final int legacyDataOnly;

        public MigrationStats(int totalEquipment, int withCacheData, int legacyDataOnly) {
            this.totalEquipment = totalEquipment;
            this.withCacheData = withCacheData;
            this.legacyDataOnly = legacyDataOnly;
        }

        public int getTotalEquipment() {
            return totalEquipment;
        }

        public int getWithCacheData() {
            return withCacheData;
        }

        public int getLegacyDataOnly() {
            return legacyDataOnly;
        }

        public double getMigrationProgress() {
            return totalEquipment > 0 ? (double) withCacheData / totalEquipment * 100.0 : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "MigrationStats{total=%d, cached=%d, legacy=%d, progress=%.1f%%}",
                    totalEquipment, withCacheData, legacyDataOnly, getMigrationProgress()
            );
        }
    }
}