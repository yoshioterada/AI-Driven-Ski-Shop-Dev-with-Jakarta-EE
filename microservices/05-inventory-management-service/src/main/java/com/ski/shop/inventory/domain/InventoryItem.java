package com.ski.shop.inventory.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * InventoryItem entity representing individual inventory items.
 * Each equipment can have multiple inventory items with serial numbers.
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem extends PanacheEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    public Equipment equipment;

    @NotNull
    @Column(name = "serial_number", length = 100, nullable = false, unique = true)
    public String serialNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    public ItemStatus status = ItemStatus.AVAILABLE;

    @NotNull
    @Column(name = "location", length = 100, nullable = false)
    public String location;

    @Column(name = "size", length = 20)
    public String size;

    @Column(name = "condition_rating")
    public Integer conditionRating = 10; // 1-10 scale, 10 being new

    @Column(name = "purchase_date")
    public LocalDate purchaseDate;

    @Column(name = "last_maintenance_date")
    public LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date")
    public LocalDate nextMaintenanceDate;

    @NotNull
    @Column(name = "total_rental_count", nullable = false)
    public Integer totalRentalCount = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    public String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    // Constructors
    public InventoryItem() {}

    public InventoryItem(Equipment equipment, String serialNumber, String location, String size) {
        this.equipment = equipment;
        this.serialNumber = serialNumber;
        this.location = location;
        this.size = size;
        this.status = ItemStatus.AVAILABLE;
        this.conditionRating = 10;
        this.totalRentalCount = 0;
    }

    // Business methods
    public boolean isAvailable() {
        return status == ItemStatus.AVAILABLE && conditionRating >= 6;
    }

    public boolean needsMaintenance() {
        return totalRentalCount > 0 && (totalRentalCount % 20 == 0) || 
               conditionRating < 6 ||
               (nextMaintenanceDate != null && nextMaintenanceDate.isBefore(LocalDate.now()));
    }

    public void rent() {
        if (isAvailable()) {
            this.status = ItemStatus.RENTED;
            this.totalRentalCount++;
            
            // Update condition rating based on usage
            if (totalRentalCount % 5 == 0 && conditionRating > 1) {
                this.conditionRating = Math.max(1, conditionRating - 1);
            }
        }
    }

    public void returnItem() {
        if (status == ItemStatus.RENTED) {
            this.status = ItemStatus.AVAILABLE;
        }
    }

    public void sendToMaintenance() {
        this.status = ItemStatus.MAINTENANCE;
        this.lastMaintenanceDate = LocalDate.now();
    }

    public void completeMaintenanceAndReturn(Integer newConditionRating) {
        this.status = ItemStatus.AVAILABLE;
        this.conditionRating = Math.min(10, newConditionRating);
        this.nextMaintenanceDate = LocalDate.now().plusMonths(6);
    }

    public void retire() {
        this.status = ItemStatus.RETIRED;
    }

    // Static finder methods
    public static InventoryItem findBySerialNumber(String serialNumber) {
        return find("serialNumber", serialNumber).firstResult();
    }

    @Override
    public String toString() {
        return "InventoryItem{" +
                "id=" + id +
                ", serialNumber='" + serialNumber + '\'' +
                ", status=" + status +
                ", location='" + location + '\'' +
                ", size='" + size + '\'' +
                ", conditionRating=" + conditionRating +
                ", totalRentalCount=" + totalRentalCount +
                '}';
    }

    // ItemStatus enum
    public enum ItemStatus {
        AVAILABLE,
        RENTED,
        RESERVED,
        MAINTENANCE,
        RETIRED,
        LOST_DAMAGED
    }
}