package com.skiresort.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 在庫移動履歴エンティティ
 */
@Entity
@Table(name = "stock_movements")
@NamedQueries({
    @NamedQuery(
        name = "StockMovement.findByInventoryItem",
        query = "SELECT m FROM StockMovement m WHERE m.inventoryItem = :inventoryItem ORDER BY m.createdAt DESC"
    ),
    @NamedQuery(
        name = "StockMovement.findByDateRange",
        query = "SELECT m FROM StockMovement m WHERE m.createdAt BETWEEN :startDate AND :endDate ORDER BY m.createdAt DESC"
    ),
    @NamedQuery(
        name = "StockMovement.findByMovementType",
        query = "SELECT m FROM StockMovement m WHERE m.movementType = :movementType ORDER BY m.createdAt DESC"
    )
})
public class StockMovement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;
    
    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "previous_quantity")
    private Integer previousQuantity;
    
    @Column(name = "new_quantity")
    private Integer newQuantity;
    
    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;
    
    @Column(name = "total_cost", precision = 12, scale = 2)
    private BigDecimal totalCost;
    
    @Size(max = 500)
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Size(max = 100)
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;
    
    @Column(name = "order_id")
    private UUID orderId;
    
    @Column(name = "supplier_id")
    private UUID supplierId;
    
    @Size(max = 100)
    @Column(name = "performed_by", length = 100)
    private String performedBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Size(max = 1000)
    @Column(name = "notes", length = 1000)
    private String notes;
    
    // コンストラクタ
    public StockMovement() {
        this.createdAt = LocalDateTime.now();
    }
    
    public StockMovement(InventoryItem inventoryItem, MovementType movementType, 
                        Integer quantity, Integer previousQuantity, Integer newQuantity, 
                        String reason) {
        this();
        this.inventoryItem = inventoryItem;
        this.movementType = movementType;
        this.quantity = quantity;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.reason = reason;
    }
    
    // ビジネスロジック
    public boolean isInbound() {
        return movementType == MovementType.INBOUND || 
               movementType == MovementType.RETURN || 
               movementType == MovementType.ADJUSTMENT && quantity > 0;
    }
    
    public boolean isOutbound() {
        return movementType == MovementType.OUTBOUND || 
               movementType == MovementType.DAMAGE || 
               movementType == MovementType.THEFT ||
               movementType == MovementType.ADJUSTMENT && quantity < 0;
    }
    
    public Integer getNetQuantityChange() {
        switch (movementType) {
            case INBOUND: 
            case RETURN: return quantity;
            case OUTBOUND: 
            case DAMAGE: 
            case THEFT: return -quantity;
            case ADJUSTMENT: return newQuantity != null && previousQuantity != null ? 
                newQuantity - previousQuantity : 0;
            case TRANSFER: return 0; // Transfer handled separately
            default: return 0;
        }
    }
    
    // Getter and Setter methods
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }
    
    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }
    
    public MovementType getMovementType() {
        return movementType;
    }
    
    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Integer getPreviousQuantity() {
        return previousQuantity;
    }
    
    public void setPreviousQuantity(Integer previousQuantity) {
        this.previousQuantity = previousQuantity;
    }
    
    public Integer getNewQuantity() {
        return newQuantity;
    }
    
    public void setNewQuantity(Integer newQuantity) {
        this.newQuantity = newQuantity;
    }
    
    public BigDecimal getUnitCost() {
        return unitCost;
    }
    
    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }
    
    public BigDecimal getTotalCost() {
        return totalCost;
    }
    
    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getReferenceNumber() {
        return referenceNumber;
    }
    
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
    
    public UUID getSupplierId() {
        return supplierId;
    }
    
    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }
    
    public String getPerformedBy() {
        return performedBy;
    }
    
    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockMovement that)) return false;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "StockMovement{" +
                "id=" + id +
                ", movementType=" + movementType +
                ", quantity=" + quantity +
                ", reason='" + reason + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
