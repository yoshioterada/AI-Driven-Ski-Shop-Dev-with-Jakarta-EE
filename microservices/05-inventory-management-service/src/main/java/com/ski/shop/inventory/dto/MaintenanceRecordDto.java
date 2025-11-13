package com.ski.shop.inventory.dto;

import com.ski.shop.inventory.domain.MaintenanceRecord;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for MaintenanceRecord entity operations
 */
public class MaintenanceRecordDto {

    public UUID maintenanceId;
    public Long equipmentId;
    public MaintenanceRecord.MaintenanceType maintenanceType;
    public MaintenanceRecord.MaintenanceStatus status;
    public String title;
    public String description;
    public LocalDateTime scheduledDate;
    public LocalDateTime startedDate;
    public LocalDateTime completedDate;
    public String technicianId;
    public Integer estimatedDurationMinutes;
    public Integer actualDurationMinutes;
    public Integer priority;
    public BigDecimal cost;
    public String partsUsed;
    public String notes;
    public LocalDateTime nextMaintenanceDate;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    // Helper properties
    public Boolean isOverdue;
    public Boolean isInProgress;
    public Boolean isCompleted;

    public MaintenanceRecordDto() {}

    public MaintenanceRecordDto(MaintenanceRecord record) {
        this.maintenanceId = record.maintenanceId;
        this.equipmentId = record.equipmentId;
        this.maintenanceType = record.maintenanceType;
        this.status = record.status;
        this.title = record.title;
        this.description = record.description;
        this.scheduledDate = record.scheduledDate;
        this.startedDate = record.startedDate;
        this.completedDate = record.completedDate;
        this.technicianId = record.technicianId;
        this.estimatedDurationMinutes = record.estimatedDurationMinutes;
        this.actualDurationMinutes = record.actualDurationMinutes;
        this.priority = record.priority;
        this.cost = record.cost;
        this.partsUsed = record.partsUsed;
        this.notes = record.notes;
        this.nextMaintenanceDate = record.nextMaintenanceDate;
        this.createdAt = record.createdAt;
        this.updatedAt = record.updatedAt;
        
        // Set helper properties
        this.isOverdue = record.isOverdue();
        this.isInProgress = record.isInProgress();
        this.isCompleted = record.isCompleted();
    }

    /**
     * Request DTO for creating maintenance records
     */
    public static class CreateMaintenanceRequest {
        @NotNull
        public Long equipmentId;
        
        @NotNull
        public MaintenanceRecord.MaintenanceType maintenanceType;
        
        @NotNull
        public String title;
        
        public String description;
        
        @NotNull
        public LocalDateTime scheduledDate;
        
        public String technicianId;
        
        @Positive
        public Integer estimatedDurationMinutes;
        
        public Integer priority = 3;
        
        public BigDecimal cost;
        
        public LocalDateTime nextMaintenanceDate;
    }

    /**
     * Request DTO for updating maintenance records
     */
    public static class UpdateMaintenanceRequest {
        public MaintenanceRecord.MaintenanceStatus status;
        public String description;
        public LocalDateTime scheduledDate;
        public String technicianId;
        public Integer estimatedDurationMinutes;
        public Integer priority;
        public BigDecimal cost;
        public String partsUsed;
        public String notes;
        public LocalDateTime nextMaintenanceDate;
    }

    /**
     * Request DTO for starting maintenance
     */
    public static class StartMaintenanceRequest {
        @NotNull
        public String technicianId;
    }

    /**
     * Request DTO for completing maintenance
     */
    public static class CompleteMaintenanceRequest {
        public String notes;
        public BigDecimal cost;
        public String partsUsed;
        public LocalDateTime nextMaintenanceDate;
    }
}