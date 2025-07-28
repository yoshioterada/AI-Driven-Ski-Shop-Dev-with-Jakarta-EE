package com.skiresort.inventory.resource;

import com.skiresort.inventory.service.DataMigrationService;
import com.skiresort.inventory.service.DataSyncHealthCheck;
import com.skiresort.inventory.service.EquipmentService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * 管理用リソース（データ移行・同期状況確認）
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    DataMigrationService migrationService;

    @Inject
    DataSyncHealthCheck healthCheck;

    @Inject
    EquipmentService equipmentService;

    /**
     * データ移行実行
     */
    @POST
    @Path("/migrate")
    public Response migrateData() {
        try {
            int migratedCount = migrationService.migrateLegacyDataToCacheFields();
            return Response.ok(Map.of(
                    "status", "success",
                    "message", "Migration completed",
                    "migratedCount", migratedCount
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", "Migration failed: " + e.getMessage()
                    )).build();
        }
    }

    /**
     * キャッシュデータ修復
     */
    @POST
    @Path("/repair")
    public Response repairCacheData() {
        try {
            int repairedCount = migrationService.validateAndRepairCacheData();
            return Response.ok(Map.of(
                    "status", "success",
                    "message", "Cache repair completed",
                    "repairedCount", repairedCount
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", "Cache repair failed: " + e.getMessage()
                    )).build();
        }
    }

    /**
     * 移行状況確認
     */
    @GET
    @Path("/migration-status")
    public Response getMigrationStatus() {
        try {
            DataMigrationService.MigrationStats stats = migrationService.getMigrationStats();
            return Response.ok(Map.of(
                    "totalEquipment", stats.getTotalEquipment(),
                    "withCacheData", stats.getWithCacheData(),
                    "legacyDataOnly", stats.getLegacyDataOnly(),
                    "migrationProgress", stats.getMigrationProgress(),
                    "status", stats.getMigrationProgress() >= 100.0 ? "completed" : "in_progress"
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", "Failed to get migration status: " + e.getMessage()
                    )).build();
        }
    }

    /**
     * データ同期ヘルスチェック
     */
    @GET
    @Path("/sync-health")
    public Response getSyncHealth() {
        try {
            DataSyncHealthCheck.DataSyncStatus status = healthCheck.checkDataSync();
            return Response.ok(Map.of(
                    "healthy", status.isHealthy(),
                    "totalEquipment", status.getTotalEquipment(),
                    "cacheIssues", status.getCacheIssues(),
                    "staleCacheCount", status.getStaleCacheCount(),
                    "status", status.isHealthy() ? "healthy" : "unhealthy"
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", "Failed to check sync health: " + e.getMessage()
                    )).build();
        }
    }

    /**
     * 設備一覧（簡易）
     */
    @GET
    @Path("/equipment")
    public Response getEquipmentList(@QueryParam("limit") @DefaultValue("10") int limit) {
        try {
            var equipments = equipmentService.findAll();
            var limitedList = equipments.stream()
                    .limit(limit)
                    .map(e -> Map.of(
                            "id", e.getId(),
                            "productId", e.getProductId(),
                            "cachedSku", e.getCachedSku() != null ? e.getCachedSku() : e.getSku(),
                            "cachedName", e.getCachedName() != null ? e.getCachedName() : e.getName(),
                            "dailyRate", e.getDailyRate(),
                            "isActive", e.isActive(),
                            "cacheUpdatedAt", e.getCacheUpdatedAt()
                    ))
                    .toList();

            return Response.ok(Map.of(
                    "equipments", limitedList,
                    "total", equipments.size(),
                    "showing", Math.min(limit, equipments.size())
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", "Failed to get equipment list: " + e.getMessage()
                    )).build();
        }
    }
}