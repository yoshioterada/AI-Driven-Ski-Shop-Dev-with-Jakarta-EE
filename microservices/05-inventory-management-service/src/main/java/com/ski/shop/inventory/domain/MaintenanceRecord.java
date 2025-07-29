package com.ski.shop.inventory.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MaintenanceRecord entity for tracking equipment maintenance activities.
 * Supports preventive and corrective maintenance scheduling and tracking.
 */
@Entity
@Table(name = "maintenance_records", indexes = {
    @Index(name = "idx_maintenance_equipment_id", columnList = "equipment_id"),
    @Index(name = "idx_maintenance_status", columnList = "status"),
    @Index(name = "idx_maintenance_scheduled_date", columnList = "scheduled_date"),
    @Index(name = "idx_maintenance_type", columnList = "maintenance_type")
})
public class MaintenanceRecord extends PanacheEntity {

    @NotNull
    @Column(name = "maintenance_id", nullable = false, unique = true)
    public UUID maintenanceId = UUID.randomUUID();

    @NotNull
    @Column(name = "equipment_id", nullable = false)
    public Long equipmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false)
    public MaintenanceType maintenanceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public MaintenanceStatus status = MaintenanceStatus.SCHEDULED;

    @Column(name = "title", length = 255, nullable = false)
    public String title;

    @Column(name = "description", length = 1000)
    public String description;

    @Column(name = "scheduled_date", nullable = false)
    public LocalDateTime scheduledDate;

    @Column(name = "started_date")
    public LocalDateTime startedDate;

    @Column(name = "completed_date")
    public LocalDateTime completedDate;

    @Column(name = "technician_id", length = 100)
    public String technicianId;

    @Column(name = "estimated_duration_minutes")
    public Integer estimatedDurationMinutes;

    @Column(name = "actual_duration_minutes")
    public Integer actualDurationMinutes;

    @Column(name = "priority", nullable = false)
    public Integer priority = 3; // 1=High, 2=Medium, 3=Low

    @Column(name = "cost", precision = 10, scale = 2)
    public java.math.BigDecimal cost;

    @Column(name = "parts_used", length = 500)
    public String partsUsed;

    @Column(name = "notes", length = 2000)
    public String notes;

    @Column(name = "next_maintenance_date")
    public LocalDateTime nextMaintenanceDate;

    @Version
    @Column(name = "version_field")
    public Long versionField = 1L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    // Helper methods
    public boolean isOverdue() {
        return status == MaintenanceStatus.SCHEDULED && 
               scheduledDate.isBefore(LocalDateTime.now());
    }

    public boolean isInProgress() {
        return status == MaintenanceStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return status == MaintenanceStatus.COMPLETED;
    }

    public void startMaintenance(String technicianId) {
        this.status = MaintenanceStatus.IN_PROGRESS;
        this.startedDate = LocalDateTime.now();
        this.technicianId = technicianId;
    }

    public void completeMaintenance(String notes, java.math.BigDecimal cost, String partsUsed) {
        this.status = MaintenanceStatus.COMPLETED;
        this.completedDate = LocalDateTime.now();
        this.notes = notes;
        this.cost = cost;
        this.partsUsed = partsUsed;
        
        if (startedDate != null) {
            this.actualDurationMinutes = (int) java.time.Duration.between(startedDate, completedDate).toMinutes();
        }
    }

    public enum MaintenanceType {
        PREVENTIVE,
        CORRECTIVE,
        SAFETY_INSPECTION,
        CALIBRATION,
        CLEANING,
        REPAIR,
        REPLACEMENT
    }

    public enum MaintenanceStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        POSTPONED
    }
}