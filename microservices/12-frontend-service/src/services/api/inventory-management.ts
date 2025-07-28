/**
 * Inventory Management Service API Client
 * 
 * Inventory Management Serviceへのアクセス用APIクライアント
 * 既存のproduct-catalogとshopping-cartサービスと同じパターンに従う
 */

import axios, { AxiosInstance } from 'axios';
import {
  InventoryItemResponse,
  StockReservationResponse,
  CreateInventoryItemRequest,
  ReserveInventoryRequest,
  UpdateStockRequest,
  InventoryErrorResponse,
  InventoryHealthResponse,
  BulkInventoryCheckRequest,
  BulkInventoryCheckResponse
} from '../../types/inventory';

// Inventory Management Serviceの直接URL
const INVENTORY_MANAGEMENT_BASE_URL = process.env.NEXT_PUBLIC_INVENTORY_MANAGEMENT_URL || 'http://localhost:8084';

/**
 * APIベースURLを取得
 * 環境変数NEXT_PUBLIC_USE_API_GATEWAYがtrueの場合はAPI Gateway経由、
 * それ以外はInventory Management Serviceに直接アクセス
 */
const getBaseUrl = (): string => {
  const useApiGateway = process.env.NEXT_PUBLIC_USE_API_GATEWAY === 'true';
  
  if (useApiGateway) {
    // API Gateway経由の場合
    return process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
  } else {
    // Inventory Management Serviceに直接アクセス
    return INVENTORY_MANAGEMENT_BASE_URL;
  }
};

/**
 * Inventory Management Service専用HTTPクライアント
 */
class InventoryManagementHttpClient {
  private readonly axiosInstance: AxiosInstance;

  constructor(baseURL: string) {
    this.axiosInstance = axios.create({
      baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    // リクエストインターセプター
    this.axiosInstance.interceptors.request.use(
      (config) => {
        console.log(`[InventoryManagement] ${config.method?.toUpperCase()} ${config.url}`);
        return config;
      },
      (error) => {
        console.error('[InventoryManagement] Request error:', error);
        return Promise.reject(new Error('Request failed'));
      }
    );

    // レスポンスインターセプター
    this.axiosInstance.interceptors.response.use(
      (response) => {
        console.log(`[InventoryManagement] ${response.status} ${response.config.url}`);
        return response;
      },
      (error) => {
        console.error('[InventoryManagement] Response error:', error);
        return Promise.reject(new Error('Response failed'));
      }
    );
  }

  public updateBaseURL(baseURL: string): void {
    this.axiosInstance.defaults.baseURL = baseURL;
  }

  public getAxiosInstance(): AxiosInstance {
    return this.axiosInstance;
  }
}

/**
 * Inventory Management Service API Client
 */
export class InventoryManagementApi {
  private readonly httpClient: InventoryManagementHttpClient;

  constructor() {
    this.httpClient = new InventoryManagementHttpClient(getBaseUrl());
  }

  /**
   * API Gateway経由に切り替える
   */
  public switchToApiGateway(): void {
    const gatewayUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
    this.httpClient.updateBaseURL(gatewayUrl);
  }

  /**
   * Inventory Management Service直接アクセスに切り替える
   */
  public switchToDirectAccess(): void {
    this.httpClient.updateBaseURL(INVENTORY_MANAGEMENT_BASE_URL);
  }

  // =================
  // 在庫アイテム管理API
  // =================

  /**
   * SKUで在庫アイテムを取得
   */
  async getInventoryItemBySku(sku: string): Promise<InventoryItemResponse> {
    const response = await this.httpClient.getAxiosInstance().get<InventoryItemResponse>(
      `/api/v1/inventory/items/sku/${encodeURIComponent(sku)}`
    );
    return response.data;
  }

  /**
   * IDで在庫アイテムを取得
   */
  async getInventoryItemById(id: string): Promise<InventoryItemResponse> {
    const response = await this.httpClient.getAxiosInstance().get<InventoryItemResponse>(
      `/api/v1/inventory/items/${id}`
    );
    return response.data;
  }

  /**
   * 全ての在庫アイテムを取得
   */
  async getAllInventoryItems(): Promise<InventoryItemResponse[]> {
    const response = await this.httpClient.getAxiosInstance().get<InventoryItemResponse[]>(
      '/api/v1/inventory/items'
    );
    return response.data;
  }

  /**
   * 倉庫IDで在庫アイテム一覧を取得
   */
  async getInventoryItemsByWarehouse(warehouseId: string): Promise<InventoryItemResponse[]> {
    const response = await this.httpClient.getAxiosInstance().get<InventoryItemResponse[]>(
      `/api/v1/inventory/items/warehouse/${warehouseId}`
    );
    return response.data;
  }

  /**
   * 低在庫アイテムを取得
   */
  async getLowStockItems(): Promise<InventoryItemResponse[]> {
    const response = await this.httpClient.getAxiosInstance().get<InventoryItemResponse[]>(
      '/api/v1/inventory/items/low-stock'
    );
    return response.data;
  }

  /**
   * 在庫切れアイテムを取得
   */
  async getOutOfStockItems(): Promise<InventoryItemResponse[]> {
    const response = await this.httpClient.getAxiosInstance().get<InventoryItemResponse[]>(
      '/api/v1/inventory/items/out-of-stock'
    );
    return response.data;
  }

  /**
   * 複数SKUの在庫情報を一括取得（パフォーマンス最適化）
   */
  async getBulkInventoryBySkus(skus: string[]): Promise<BulkInventoryCheckResponse> {
    // 複数のSKUを並行して取得
    try {
      const promises = skus.map(sku => 
        this.getInventoryItemBySku(sku).catch(() => null)
      );
      
      const results = await Promise.all(promises);
      
      const inventoryItems: InventoryItemResponse[] = [];
      const notFound: string[] = [];
      
      results.forEach((result, index) => {
        if (result) {
          inventoryItems.push(result);
        } else {
          notFound.push(skus[index]);
        }
      });
      
      return { inventoryItems, notFound };
    } catch (error) {
      console.error('Bulk inventory check failed:', error);
      throw error;
    }
  }

  /**
   * 在庫アイテムを作成
   */
  async createInventoryItem(request: CreateInventoryItemRequest): Promise<InventoryItemResponse> {
    const response = await this.httpClient.getAxiosInstance().post<InventoryItemResponse>(
      '/api/v1/inventory/items',
      request
    );
    return response.data;
  }

  /**
   * 在庫を更新
   */
  async updateStock(request: UpdateStockRequest): Promise<InventoryItemResponse> {
    const response = await this.httpClient.getAxiosInstance().put<InventoryItemResponse>(
      '/api/v1/inventory/items/update-stock',
      request
    );
    return response.data;
  }

  // =================
  // 在庫予約管理API
  // =================

  /**
   * 在庫を予約
   */
  async reserveInventory(request: ReserveInventoryRequest): Promise<StockReservationResponse> {
    const response = await this.httpClient.getAxiosInstance().post<StockReservationResponse>(
      '/api/v1/inventory/reservations',
      request
    );
    return response.data;
  }

  /**
   * 在庫予約を確定
   */
  async confirmReservation(reservationId: string): Promise<StockReservationResponse> {
    const response = await this.httpClient.getAxiosInstance().put<StockReservationResponse>(
      `/api/v1/inventory/reservations/${reservationId}/confirm`
    );
    return response.data;
  }

  /**
   * 在庫予約をキャンセル
   */
  async cancelReservation(reservationId: string, reason?: string): Promise<StockReservationResponse> {
    const url = reason 
      ? `/api/v1/inventory/reservations/${reservationId}/cancel?reason=${encodeURIComponent(reason)}`
      : `/api/v1/inventory/reservations/${reservationId}/cancel`;
      
    const response = await this.httpClient.getAxiosInstance().put<StockReservationResponse>(url);
    return response.data;
  }

  /**
   * 注文IDで予約を取得
   */
  async getReservationsByOrderId(orderId: string): Promise<StockReservationResponse[]> {
    const response = await this.httpClient.getAxiosInstance().get<StockReservationResponse[]>(
      `/api/v1/inventory/reservations/order/${orderId}`
    );
    return response.data;
  }

  /**
   * 顧客IDで予約を取得
   */
  async getReservationsByCustomerId(customerId: string): Promise<StockReservationResponse[]> {
    const response = await this.httpClient.getAxiosInstance().get<StockReservationResponse[]>(
      `/api/v1/inventory/reservations/customer/${customerId}`
    );
    return response.data;
  }

  /**
   * 期限切れ予約を処理
   */
  async processExpiredReservations(): Promise<{ processedCount: number; message: string }> {
    const response = await this.httpClient.getAxiosInstance().post<{ processedCount: number; message: string }>(
      '/api/v1/inventory/reservations/process-expired'
    );
    return response.data;
  }

  // =================
  // システム関連API
  // =================

  /**
   * ヘルスチェック
   */
  async getHealth(): Promise<InventoryHealthResponse> {
    const response = await this.httpClient.getAxiosInstance().get<InventoryHealthResponse>('/q/health');
    return response.data;
  }

  // =================
  // 便利なヘルパーメソッド
  // =================

  /**
   * 商品が在庫切れかどうかをチェック
   */
  async isOutOfStock(sku: string): Promise<boolean> {
    try {
      const inventory = await this.getInventoryItemBySku(sku);
      return inventory.isOutOfStock;
    } catch (error) {
      console.error(`Failed to check stock for SKU: ${sku}`, error);
      return true; // エラーの場合は在庫切れとして扱う
    }
  }

  /**
   * 商品が低在庫かどうかをチェック
   */
  async isLowStock(sku: string): Promise<boolean> {
    try {
      const inventory = await this.getInventoryItemBySku(sku);
      return inventory.isLowStock;
    } catch (error) {
      console.error(`Failed to check low stock for SKU: ${sku}`, error);
      return false;
    }
  }

  /**
   * 商品の利用可能数量を取得
   */
  async getAvailableQuantity(sku: string): Promise<number> {
    try {
      const inventory = await this.getInventoryItemBySku(sku);
      return inventory.availableQuantity;
    } catch (error) {
      console.error(`Failed to get available quantity for SKU: ${sku}`, error);
      return 0;
    }
  }
}

// デフォルトインスタンス
export const inventoryManagementApi = new InventoryManagementApi();

// 便利関数
export const switchInventoryApiToGateway = () => {
  inventoryManagementApi.switchToApiGateway();
};

export const switchInventoryApiToDirectAccess = () => {
  inventoryManagementApi.switchToDirectAccess();
};