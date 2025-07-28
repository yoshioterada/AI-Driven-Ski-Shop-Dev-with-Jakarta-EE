package com.skiresort.inventory.service;

import com.skiresort.inventory.model.Equipment;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * データ同期ヘルスチェック
 */
@ApplicationScoped
@Readiness
public class DataSyncHealthCheck implements HealthCheck {

    @PersistenceContext
    EntityManager entityManager;

    @Inject
    EquipmentService equipmentService;

    @Override
    public HealthCheckResponse call() {
        try {
            DataSyncStatus status = checkDataSync();
            
            if (status.isHealthy()) {
                return HealthCheckResponse.up("product-inventory-sync")
                        .withData("status", "healthy")
                        .withData("last_check", LocalDateTime.now().toString())
                        .withData("total_equipment", status.getTotalEquipment())
                        .withData("cache_issues", status.getCacheIssues())
                        .build();
            } else {
                return HealthCheckResponse.down("product-inventory-sync")
                        .withData("status", "unhealthy")
                        .withData("last_check", LocalDateTime.now().toString())
                        .withData("total_equipment", status.getTotalEquipment())
                        .withData("cache_issues", status.getCacheIssues())
                        .withData("stale_cache_count", status.getStaleCacheCount())
                        .build();
            }
        } catch (Exception e) {
            return HealthCheckResponse.down("product-inventory-sync")
                    .withData("status", "error")
                    .withData("error", e.getMessage())
                    .build();
        }
    }

    public DataSyncStatus checkDataSync() {
        List<Equipment> equipments = equipmentService.findAll();
        int totalEquipment = equipments.size();
        int cacheIssues = 0;
        int staleCacheCount = 0;

        LocalDateTime now = LocalDateTime.now();
        
        for (Equipment equipment : equipments) {
            // キャッシュの整合性チェック
            if (equipment.getCachedSku() == null || equipment.getCachedName() == null) {
                cacheIssues++;
            }
            
            // キャッシュの鮮度チェック（24時間以上更新されていない場合）
            if (equipment.getCacheUpdatedAt() != null && 
                ChronoUnit.HOURS.between(equipment.getCacheUpdatedAt(), now) > 24) {
                staleCacheCount++;
            }
        }

        boolean isHealthy = cacheIssues == 0 && staleCacheCount < (totalEquipment * 0.1); // 10%未満なら健康
        
        return new DataSyncStatus(isHealthy, totalEquipment, cacheIssues, staleCacheCount);
    }

    /**
     * データ同期ステータスを表すクラス
     */
    public static class DataSyncStatus {
        private final boolean healthy;
        private final int totalEquipment;
        private final int cacheIssues;
        private final int staleCacheCount;

        public DataSyncStatus(boolean healthy, int totalEquipment, int cacheIssues, int staleCacheCount) {
            this.healthy = healthy;
            this.totalEquipment = totalEquipment;
            this.cacheIssues = cacheIssues;
            this.staleCacheCount = staleCacheCount;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public int getTotalEquipment() {
            return totalEquipment;
        }

        public int getCacheIssues() {
            return cacheIssues;
        }

        public int getStaleCacheCount() {
            return staleCacheCount;
        }
    }
}