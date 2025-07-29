package com.skiresort.inventory.service;

import com.skiresort.inventory.dto.*;
import com.skiresort.inventory.exception.InsufficientStockException;
import com.skiresort.inventory.exception.InventoryItemNotFoundException;
import com.skiresort.inventory.model.*;
import com.skiresort.inventory.repository.InventoryItemRepository;
import com.skiresort.inventory.repository.StockReservationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 在庫管理サービス
 */
@ApplicationScoped
@Transactional
public class InventoryService {
    
    private static final Logger logger = Logger.getLogger(InventoryService.class.getName());
    
    @Inject
    private InventoryItemRepository inventoryRepository;
    
    @Inject
    private StockReservationRepository reservationRepository;
    
    @Inject
    private StockMovementService stockMovementService;
    
    /**
     * 在庫アイテムを作成する
     */
    public InventoryItemResponse createInventoryItem(CreateInventoryItemRequest request) {
        logger.info("在庫アイテムを作成中: SKU=" + request.sku());
        
        // SKUの重複チェック
        if (inventoryRepository.existsBySku(request.sku())) {
            throw new IllegalArgumentException("指定されたSKUは既に存在します: " + request.sku());
        }
        
        // 在庫アイテムを作成
        InventoryItem item = new InventoryItem(
            request.productId(),
            request.sku(),
            request.warehouseId(),
            request.availableQuantity(),
            request.minimumStockLevel(),
            request.maximumStockLevel(),
            request.reorderPoint(),
            request.reorderQuantity()
        );
        item.setStatus(request.status());
        
        // 保存
        InventoryItem savedItem = inventoryRepository.save(item);
        
        // 初期在庫移動履歴を記録
        if (request.availableQuantity() > 0) {
            stockMovementService.recordStockMovement(
                savedItem,
                MovementType.INBOUND,
                request.availableQuantity(),
                0,
                request.availableQuantity(),
                "初期在庫登録"
            );
        }
        
        logger.info("在庫アイテムを作成しました: SKU=" + savedItem.getSku() + ", ID=" + savedItem.getId());
        return InventoryItemResponse.from(savedItem);
    }
    
    /**
     * SKUで在庫アイテムを取得する
     */
    public InventoryItemResponse getInventoryItemBySku(String sku) {
        InventoryItem item = inventoryRepository.findBySku(sku)
            .orElseThrow(() -> InventoryItemNotFoundException.forSku(sku));
        return InventoryItemResponse.from(item);
    }
    
    /**
     * IDで在庫アイテムを取得する
     */
    public InventoryItemResponse getInventoryItemById(UUID id) {
        InventoryItem item = inventoryRepository.findById(id)
            .orElseThrow(() -> InventoryItemNotFoundException.forId(id.toString()));
        return InventoryItemResponse.from(item);
    }
    
    /**
     * 倉庫IDで在庫アイテムを取得する
     */
    public List<InventoryItemResponse> getInventoryItemsByWarehouse(UUID warehouseId) {
        List<InventoryItem> items = inventoryRepository.findByWarehouseId(warehouseId);
        return items.stream()
            .map(InventoryItemResponse::from)
            .toList();
    }
    
    /**
     * 全ての在庫アイテムを取得する
     */
    public List<InventoryItemResponse> getAllInventoryItems() {
        List<InventoryItem> items = inventoryRepository.findAll();
        return items.stream()
            .map(InventoryItemResponse::from)
            .toList();
    }
    
    /**
     * 低在庫アイテムを取得する
     */
    public List<InventoryItemResponse> getLowStockItems() {
        List<InventoryItem> items = inventoryRepository.findLowStockItems();
        return items.stream()
            .map(InventoryItemResponse::from)
            .toList();
    }
    
    /**
     * 在庫切れアイテムを取得する
     */
    public List<InventoryItemResponse> getOutOfStockItems() {
        List<InventoryItem> items = inventoryRepository.findOutOfStockItems();
        return items.stream()
            .map(InventoryItemResponse::from)
            .toList();
    }
    
    /**
     * 在庫を予約する
     */
    public StockReservationResponse reserveInventory(ReserveInventoryRequest request) {
        logger.info("在庫予約中: SKU=" + request.sku() + ", 数量=" + request.quantity());
        
        // 在庫アイテムを取得
        InventoryItem item = inventoryRepository.findBySku(request.sku())
            .orElseThrow(() -> InventoryItemNotFoundException.forSku(request.sku()));
        
        // 在庫チェック
        if (!item.canReserve(request.quantity())) {
            throw new InsufficientStockException(
                request.sku(), 
                request.quantity(), 
                item.getAvailableQuantity()
            );
        }
        
        // 在庫を予約
        item.reserveStock(request.quantity());
        inventoryRepository.save(item);
        
        // 予約レコードを作成
        StockReservation reservation = new StockReservation(
            item,
            request.orderId(),
            request.customerId(),
            request.quantity(),
            request.expiresAt()
        );
        StockReservation savedReservation = reservationRepository.save(reservation);
        
        // 在庫移動履歴を記録
        stockMovementService.recordReservation(item, request.quantity(), request.orderId());
        
        logger.info("在庫予約を完了しました: 予約ID=" + savedReservation.getId());
        return StockReservationResponse.from(savedReservation);
    }
    
    /**
     * 在庫予約を確定する
     */
    public StockReservationResponse confirmReservation(UUID reservationId) {
        logger.info("在庫予約確定中: 予約ID=" + reservationId);
        
        StockReservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("予約が見つかりません: " + reservationId));
        
        if (!reservation.canConfirm()) {
            throw new IllegalStateException("予約を確定できません。ステータス: " + reservation.getStatus() + 
                ", 期限切れ: " + reservation.isExpired());
        }
        
        // 予約を確定
        reservation.confirm();
        InventoryItem item = reservation.getInventoryItem();
        item.confirmReservation(reservation.getReservedQuantity());
        
        // 保存
        reservationRepository.save(reservation);
        inventoryRepository.save(item);
        
        // 在庫移動履歴を記録
        stockMovementService.recordConfirmation(item, reservation.getReservedQuantity(), 
            reservation.getOrderId());
        
        logger.info("在庫予約を確定しました: 予約ID=" + reservationId);
        return StockReservationResponse.from(reservation);
    }
    
    /**
     * 在庫予約をキャンセルする
     */
    public StockReservationResponse cancelReservation(UUID reservationId, String reason) {
        logger.info("在庫予約キャンセル中: 予約ID=" + reservationId);
        
        StockReservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("予約が見つかりません: " + reservationId));
        
        if (!reservation.canCancel()) {
            throw new IllegalStateException("予約をキャンセルできません。ステータス: " + reservation.getStatus());
        }
        
        // 在庫を解放
        InventoryItem item = reservation.getInventoryItem();
        item.releaseReservation(reservation.getReservedQuantity());
        
        // 予約をキャンセル
        reservation.cancel(reason);
        
        // 保存
        inventoryRepository.save(item);
        reservationRepository.save(reservation);
        
        // 在庫移動履歴を記録
        stockMovementService.recordCancellation(item, reservation.getReservedQuantity(), 
            reservation.getOrderId(), reason);
        
        logger.info("在庫予約をキャンセルしました: 予約ID=" + reservationId);
        return StockReservationResponse.from(reservation);
    }
    
    /**
     * 在庫を更新する
     */
    public InventoryItemResponse updateStock(UpdateStockRequest request) {
        logger.info("在庫更新中: SKU=" + request.sku() + ", タイプ=" + request.movementType());
        
        // 在庫アイテムを取得
        InventoryItem item = inventoryRepository.findBySku(request.sku())
            .orElseThrow(() -> InventoryItemNotFoundException.forSku(request.sku()));
        
        Integer previousQuantity = item.getAvailableQuantity();
        
        // 移動タイプに応じて在庫を更新
        switch (request.movementType()) {
            case INBOUND:
                item.setAvailableQuantity(item.getAvailableQuantity() + request.quantity());
                break;
            case OUTBOUND:
                if (item.getAvailableQuantity() < request.quantity()) {
                    throw new InsufficientStockException(
                        request.sku(), 
                        request.quantity(), 
                        item.getAvailableQuantity()
                    );
                }
                item.setAvailableQuantity(item.getAvailableQuantity() - request.quantity());
                break;
            case ADJUSTMENT:
                item.setAvailableQuantity(request.quantity());
                break;
            default:
                throw new UnsupportedOperationException("未対応の移動タイプ: " + request.movementType());
        }
        
        // 保存
        InventoryItem savedItem = inventoryRepository.save(item);
        
        // 在庫移動履歴を記録
        stockMovementService.recordStockMovement(
            savedItem,
            request.movementType(),
            request.quantity(),
            previousQuantity,
            savedItem.getAvailableQuantity(),
            request.reason(),
            request.referenceNumber(),
            request.orderId(),
            request.supplierId(),
            request.unitCost(),
            request.performedBy(),
            request.notes()
        );
        
        logger.info("在庫を更新しました: SKU=" + savedItem.getSku() + 
            ", 前回数量=" + previousQuantity + ", 新数量=" + savedItem.getAvailableQuantity());
        
        return InventoryItemResponse.from(savedItem);
    }
    
    /**
     * 期限切れ予約を処理する
     */
    public int processExpiredReservations() {
        logger.info("期限切れ予約の処理を開始");
        
        List<StockReservation> expiredReservations = reservationRepository.findExpiredReservations();
        int processedCount = 0;
        
        for (StockReservation reservation : expiredReservations) {
            try {
                // 在庫を解放
                InventoryItem item = reservation.getInventoryItem();
                item.releaseReservation(reservation.getReservedQuantity());
                
                // 予約を期限切れに更新
                reservation.expire();
                
                // 保存
                inventoryRepository.save(item);
                reservationRepository.save(reservation);
                
                // 在庫移動履歴を記録
                stockMovementService.recordExpiration(item, reservation.getReservedQuantity(), 
                    reservation.getOrderId());
                
                processedCount++;
                
            } catch (Exception e) {
                logger.warning("期限切れ予約の処理でエラーが発生しました: 予約ID=" + 
                    reservation.getId() + ", エラー=" + e.getMessage());
            }
        }
        
        logger.info("期限切れ予約の処理を完了しました: 処理件数=" + processedCount);
        return processedCount;
    }
    
    /**
     * 注文IDで予約を取得する
     */
    public List<StockReservationResponse> getReservationsByOrderId(UUID orderId) {
        List<StockReservation> reservations = reservationRepository.findByOrderId(orderId);
        return reservations.stream()
            .map(StockReservationResponse::from)
            .toList();
    }
    
    /**
     * 顧客IDで予約を取得する
     */
    public List<StockReservationResponse> getReservationsByCustomerId(UUID customerId) {
        List<StockReservation> reservations = reservationRepository.findByCustomerId(customerId);
        return reservations.stream()
            .map(StockReservationResponse::from)
            .toList();
    }
}
