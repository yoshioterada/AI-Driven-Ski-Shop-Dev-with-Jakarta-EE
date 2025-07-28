/**
 * Inventory Management Service Types
 * 
 * TypeScript types matching the inventory management service DTOs
 */

// Inventory status enum matching the Java enum
export type InventoryStatus = 'ACTIVE' | 'INACTIVE' | 'DISCONTINUED' | 'PENDING';

// Reservation status enum matching the Java enum  
export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED';

// Stock movement type for inventory updates
export type StockMovementType = 'INBOUND' | 'OUTBOUND' | 'ADJUSTMENT' | 'RETURN' | 'DAMAGE' | 'THEFT';

/**
 * Inventory Item Response - matches InventoryItemResponse.java
 */
export interface InventoryItemResponse {
  id: string;
  productId: string;
  sku: string;
  warehouseId: string;
  availableQuantity: number;
  reservedQuantity: number;
  incomingQuantity: number;
  totalQuantity: number;
  minimumStockLevel: number;
  maximumStockLevel: number;
  reorderPoint: number;
  reorderQuantity: number;
  status: InventoryStatus;
  isLowStock: boolean;
  isOutOfStock: boolean;
  lastUpdatedAt: string;
  createdAt: string;
}

/**
 * Stock Reservation Response - matches StockReservationResponse.java
 */
export interface StockReservationResponse {
  id: string;
  inventoryItemId: string;
  sku: string;
  orderId: string;
  customerId: string;
  reservedQuantity: number;
  status: ReservationStatus;
  expiresAt: string;
  createdAt: string;
  confirmedAt?: string;
  cancelledAt?: string;
  cancellationReason?: string;
  isExpired: boolean;
  canConfirm: boolean;
  canCancel: boolean;
}

/**
 * Create Inventory Item Request - matches CreateInventoryItemRequest.java
 */
export interface CreateInventoryItemRequest {
  productId: string;
  sku: string;
  warehouseId: string;
  initialQuantity: number;
  minimumStockLevel: number;
  maximumStockLevel: number;
  reorderPoint: number;
  reorderQuantity: number;
}

/**
 * Reserve Inventory Request - matches ReserveInventoryRequest.java
 */
export interface ReserveInventoryRequest {
  sku: string;
  quantity: number;
  orderId: string;
  customerId: string;
  expirationHours?: number;
}

/**
 * Update Stock Request - matches UpdateStockRequest.java
 */
export interface UpdateStockRequest {
  sku: string;
  quantity: number;
  movementType: StockMovementType;
  reason?: string;
  reference?: string;
}

/**
 * Error Response for API errors
 */
export interface InventoryErrorResponse {
  code: string;
  message: string;
}

/**
 * Health Check Response
 */
export interface InventoryHealthResponse {
  status: string;
  checks: Array<{
    name: string;
    status: string;
    data?: Record<string, unknown>;
  }>;
}

/**
 * Bulk inventory check request for multiple SKUs
 */
export interface BulkInventoryCheckRequest {
  skus: string[];
}

/**
 * Bulk inventory check response
 */
export interface BulkInventoryCheckResponse {
  inventoryItems: InventoryItemResponse[];
  notFound: string[];
}

/**
 * Inventory summary for dashboard/analytics
 */
export interface InventorySummary {
  totalItems: number;
  lowStockItems: number;
  outOfStockItems: number;
  totalValue: number;
  lastUpdated: string;
}