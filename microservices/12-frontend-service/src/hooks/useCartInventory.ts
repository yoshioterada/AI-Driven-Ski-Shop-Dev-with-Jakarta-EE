/**
 * Cart Inventory Hook
 * 
 * カートアイテムの在庫情報を取得し、リアルタイムで更新するカスタムフック
 */

import { useState, useEffect } from 'react';
import { CartItemResponse } from '../types/cart';
import { InventoryItemResponse } from '../types/inventory';
import { inventoryManagementApi } from '../services/api/inventory-management';

export interface CartItemWithInventory extends CartItemResponse {
  inventory?: InventoryItemResponse;
  inventoryLoaded: boolean;
  inventoryError?: string;
}

/**
 * カートアイテムの在庫情報を取得するフック
 */
export const useCartInventory = (cartItems: CartItemResponse[]) => {
  const [itemsWithInventory, setItemsWithInventory] = useState<CartItemWithInventory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchInventoryData = async () => {
      if (cartItems.length === 0) {
        setItemsWithInventory([]);
        return;
      }

      setLoading(true);
      setError(null);

      try {
        // SKUリストを抽出
        const skus = cartItems.map(item => item.sku).filter(Boolean);
        
        if (skus.length === 0) {
          // SKUがない場合は在庫情報なしで返す
          const itemsWithoutInventory = cartItems.map(item => ({
            ...item,
            inventoryLoaded: false,
            inventoryError: 'SKU情報がありません'
          }));
          setItemsWithInventory(itemsWithoutInventory);
          return;
        }

        // 並行して在庫データを取得
        const inventoryPromises = skus.map(async (sku) => {
          try {
            const inventory = await inventoryManagementApi.getInventoryItemBySku(sku);
            return { sku, inventory, error: null };
          } catch (error) {
            console.warn(`[useCartInventory] SKU: ${sku} の在庫取得失敗:`, error);
            return { 
              sku, 
              inventory: null, 
              error: error instanceof Error ? error.message : '在庫データ取得失敗' 
            };
          }
        });

        const inventoryResults = await Promise.all(inventoryPromises);
        
        // カートアイテムと在庫データを統合
        const enhancedItems = cartItems.map(item => {
          const inventoryResult = inventoryResults.find(result => result.sku === item.sku);
          
          if (inventoryResult?.inventory) {
            return {
              ...item,
              inventory: inventoryResult.inventory,
              inventoryLoaded: true
            };
          } else {
            return {
              ...item,
              inventoryLoaded: false,
              inventoryError: inventoryResult?.error || '在庫データが見つかりません'
            };
          }
        });

        setItemsWithInventory(enhancedItems);
        
      } catch (error) {
        console.error('[useCartInventory] 在庫データ取得エラー:', error);
        setError(error instanceof Error ? error.message : '在庫データの取得に失敗しました');
        
        // エラーの場合は在庫情報なしで返す
        const itemsWithError = cartItems.map(item => ({
          ...item,
          inventoryLoaded: false,
          inventoryError: '在庫データの取得に失敗しました'
        }));
        setItemsWithInventory(itemsWithError);
        
      } finally {
        setLoading(false);
      }
    };

    fetchInventoryData();
  }, [cartItems]);

  return {
    itemsWithInventory,
    loading,
    error
  };
};

/**
 * 在庫ステータスのヘルパー関数
 */
export const getInventoryStatusDisplay = (item: CartItemWithInventory) => {
  if (!item.inventoryLoaded) {
    return {
      text: '在庫確認中...',
      className: 'text-gray-500',
      color: 'gray'
    };
  }

  if (!item.inventory) {
    return {
      text: '在庫不明',
      className: 'text-amber-600',
      color: 'amber'
    };
  }

  if (item.inventory.isOutOfStock) {
    return {
      text: '在庫切れ',
      className: 'text-red-600',
      color: 'red'
    };
  }

  if (item.inventory.isLowStock) {
    return {
      text: `残り${item.inventory.availableQuantity}個`,
      className: 'text-yellow-600',
      color: 'yellow'
    };
  }

  return {
    text: '在庫あり',
    className: 'text-green-600',
    color: 'green'
  };
};

/**
 * カートアイテムが購入可能かどうかを判定
 */
export const isCartItemAvailable = (item: CartItemWithInventory): boolean => {
  if (!item.inventoryLoaded || !item.inventory) {
    return true; // 在庫情報が不明な場合は購入可能とみなす
  }

  return item.inventory.availableQuantity >= item.quantity;
};

/**
 * カートアイテムの最大選択可能数量を取得
 */
export const getMaxAvailableQuantity = (item: CartItemWithInventory): number => {
  if (!item.inventoryLoaded || !item.inventory) {
    return 10; // デフォルト最大値
  }

  return Math.min(item.inventory.availableQuantity, 10);
};