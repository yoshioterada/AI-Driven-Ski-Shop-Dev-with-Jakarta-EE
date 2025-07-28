/**
 * Product with Inventory Integration Service
 * 
 * 商品カタログサービスと在庫管理サービスのデータを統合して、
 * 商品データにリアルタイムの在庫情報を追加するサービス
 */

import { Product, ProductSearchParams, productCatalogApi } from './product-catalog';
import { inventoryManagementApi, InventoryItemResponse } from './inventory-management';

/**
 * 商品データに在庫情報を追加した拡張型
 */
export interface ProductWithInventory extends Product {
  realTimeInventory?: InventoryItemResponse;
  inventoryLoaded: boolean;
  inventoryError?: string;
}

/**
 * 商品と在庫データ統合API
 */
export class ProductInventoryIntegrationApi {
  
  /**
   * 商品一覧を取得し、在庫情報を統合
   */
  async getProductsWithInventory(params?: ProductSearchParams): Promise<ProductWithInventory[]> {
    try {
      // 1. まず商品データを取得
      console.log('[ProductInventoryIntegration] 商品データを取得中...');
      const products = await productCatalogApi.getProducts(params);
      
      if (products.length === 0) {
        return [];
      }
      
      // 2. 商品のSKUリストを抽出
      const skus = products.map(product => product.sku).filter(Boolean);
      
      if (skus.length === 0) {
        console.warn('[ProductInventoryIntegration] SKUが見つかりません');
        return products.map(product => ({
          ...product,
          inventoryLoaded: false,
          inventoryError: 'SKU情報がありません'
        }));
      }
      
      // 3. 在庫データを並行取得
      console.log('[ProductInventoryIntegration] 在庫データを取得中...', skus);
      const inventoryResults = await this.fetchInventoryDataBatch(skus);
      
      // 4. 商品データと在庫データを統合
      const productsWithInventory = products.map(product => {
        const inventoryData = inventoryResults.get(product.sku);
        
        if (inventoryData && !inventoryData.error) {
          // 在庫データが正常に取得できた場合、商品の在庫情報を更新
          const updatedProduct: ProductWithInventory = {
            ...product,
            inventory: {
              quantity: inventoryData.data.totalQuantity,
              reservedQuantity: inventoryData.data.reservedQuantity,
              availableQuantity: inventoryData.data.availableQuantity,
              lowStockThreshold: inventoryData.data.minimumStockLevel,
              status: this.mapInventoryStatus(inventoryData.data.status, inventoryData.data.isOutOfStock, inventoryData.data.isLowStock)
            },
            realTimeInventory: inventoryData.data,
            inventoryLoaded: true
          };
          
          return updatedProduct;
        } else {
          // 在庫データが取得できなかった場合は元の商品データを保持
          console.warn(`[ProductInventoryIntegration] SKU: ${product.sku} の在庫データ取得失敗`, inventoryData?.error);
          
          return {
            ...product,
            inventoryLoaded: false,
            inventoryError: inventoryData?.error || '在庫データが見つかりません'
          };
        }
      });
      
      console.log('[ProductInventoryIntegration] 統合完了:', productsWithInventory.length, '商品');
      return productsWithInventory;
      
    } catch (error) {
      console.error('[ProductInventoryIntegration] 商品・在庫データ統合エラー:', error);
      
      // エラーが発生した場合は、最低限商品データだけでも返す
      try {
        const products = await productCatalogApi.getProducts(params);
        return products.map(product => ({
          ...product,
          inventoryLoaded: false,
          inventoryError: error instanceof Error ? error.message : '在庫データの取得に失敗しました'
        }));
      } catch (productError) {
        console.error('[ProductInventoryIntegration] 商品データ取得もエラー:', productError);
        throw new Error('商品データと在庫データの取得に失敗しました');
      }
    }
  }
  
  /**
   * 単一商品を取得し、在庫情報を統合
   */
  async getProductWithInventoryById(productId: string): Promise<ProductWithInventory> {
    try {
      // 1. 商品データを取得
      console.log('[ProductInventoryIntegration] 商品詳細データを取得中...', productId);
      const product = await productCatalogApi.getProductById(productId);
      
      if (!product.sku) {
        console.warn('[ProductInventoryIntegration] 商品にSKUがありません:', productId);
        return {
          ...product,
          inventoryLoaded: false,
          inventoryError: 'SKU情報がありません'
        };
      }
      
      // 2. 在庫データを取得
      try {
        console.log('[ProductInventoryIntegration] 在庫データを取得中...', product.sku);
        const inventoryData = await inventoryManagementApi.getInventoryItemBySku(product.sku);
        
        // 3. 商品データと在庫データを統合
        const productWithInventory: ProductWithInventory = {
          ...product,
          inventory: {
            quantity: inventoryData.totalQuantity,
            reservedQuantity: inventoryData.reservedQuantity,
            availableQuantity: inventoryData.availableQuantity,
            lowStockThreshold: inventoryData.minimumStockLevel,
            status: this.mapInventoryStatus(inventoryData.status, inventoryData.isOutOfStock, inventoryData.isLowStock)
          },
          realTimeInventory: inventoryData,
          inventoryLoaded: true
        };
        
        console.log('[ProductInventoryIntegration] 商品詳細統合完了:', productId);
        return productWithInventory;
        
      } catch (inventoryError) {
        console.warn('[ProductInventoryIntegration] 在庫データ取得失敗:', product.sku, inventoryError);
        
        return {
          ...product,
          inventoryLoaded: false,
          inventoryError: inventoryError instanceof Error ? inventoryError.message : '在庫データの取得に失敗しました'
        };
      }
      
    } catch (error) {
      console.error('[ProductInventoryIntegration] 商品詳細取得エラー:', productId, error);
      throw error;
    }
  }
  
  /**
   * 在庫データを一括取得（パフォーマンス最適化）
   */
  private async fetchInventoryDataBatch(skus: string[]): Promise<Map<string, { data: InventoryItemResponse; error?: string }>> {
    const results = new Map<string, { data: InventoryItemResponse; error?: string }>();
    
    // 並行処理で在庫データを取得（最大同時リクエスト数を制限）
    const BATCH_SIZE = 10; // 同時リクエスト数の制限
    
    for (let i = 0; i < skus.length; i += BATCH_SIZE) {
      const batch = skus.slice(i, i + BATCH_SIZE);
      
      const batchPromises = batch.map(async (sku) => {
        try {
          const inventoryData = await inventoryManagementApi.getInventoryItemBySku(sku);
          results.set(sku, { data: inventoryData });
        } catch (error) {
          console.warn(`[ProductInventoryIntegration] SKU: ${sku} の在庫取得失敗:`, error);
          results.set(sku, { 
            data: this.createFallbackInventoryData(sku),
            error: error instanceof Error ? error.message : '在庫データ取得失敗'
          });
        }
      });
      
      await Promise.all(batchPromises);
    }
    
    return results;
  }
  
  /**
   * 在庫ステータスをマッピング
   */
  private mapInventoryStatus(
    inventoryStatus: string, 
    isOutOfStock: boolean, 
    isLowStock: boolean
  ): 'in_stock' | 'low_stock' | 'out_of_stock' | 'discontinued' {
    if (inventoryStatus === 'DISCONTINUED') {
      return 'discontinued';
    }
    
    if (isOutOfStock) {
      return 'out_of_stock';
    }
    
    if (isLowStock) {
      return 'low_stock';
    }
    
    return 'in_stock';
  }
  
  /**
   * 在庫データが取得できない場合のフォールバックデータを作成
   */
  private createFallbackInventoryData(sku: string): InventoryItemResponse {
    return {
      id: 'unknown',
      productId: 'unknown',
      sku: sku,
      warehouseId: 'unknown',
      availableQuantity: 0,
      reservedQuantity: 0,
      incomingQuantity: 0,
      totalQuantity: 0,
      minimumStockLevel: 0,
      maximumStockLevel: 0,
      reorderPoint: 0,
      reorderQuantity: 0,
      status: 'INACTIVE',
      isLowStock: false,
      isOutOfStock: true,
      lastUpdatedAt: new Date().toISOString(),
      createdAt: new Date().toISOString()
    };
  }
}

// デフォルトインスタンス
export const productInventoryIntegrationApi = new ProductInventoryIntegrationApi();

// 便利な関数のエクスポート
export const getProductsWithInventory = (params?: ProductSearchParams) => 
  productInventoryIntegrationApi.getProductsWithInventory(params);

export const getProductWithInventoryById = (productId: string) => 
  productInventoryIntegrationApi.getProductWithInventoryById(productId);