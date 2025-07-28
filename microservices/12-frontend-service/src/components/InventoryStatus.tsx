/**
 * Inventory Status Component
 * 
 * 商品の在庫状況をリアルタイムで表示するコンポーネント
 * 既存のコンポーネントに簡単に統合できるよう設計
 */

'use client';

import React, { useState, useEffect } from 'react';
import { inventoryManagementApi } from '../services/api/inventory-management';
import { InventoryItemResponse } from '../types/inventory';

interface InventoryStatusProps {
  sku: string;
  showDetails?: boolean;
  className?: string;
  size?: 'sm' | 'md' | 'lg';
}

export const InventoryStatus: React.FC<InventoryStatusProps> = ({
  sku,
  showDetails = false,
  className = '',
  size = 'md'
}) => {
  const [inventory, setInventory] = useState<InventoryItemResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchInventory = async () => {
      if (!sku) {
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        const inventoryData = await inventoryManagementApi.getInventoryItemBySku(sku);
        setInventory(inventoryData);
      } catch (err) {
        console.warn(`[InventoryStatus] 在庫データ取得失敗 SKU: ${sku}`, err);
        setError(err instanceof Error ? err.message : '在庫データの取得に失敗しました');
      } finally {
        setLoading(false);
      }
    };

    fetchInventory();
  }, [sku]);

  if (loading) {
    return (
      <div className={`flex items-center space-x-2 ${className}`}>
        <div className="animate-pulse bg-gray-300 w-3 h-3 rounded-full"></div>
        <span className="text-gray-500 text-sm">在庫確認中...</span>
      </div>
    );
  }

  if (error || !inventory) {
    return (
      <div className={`flex items-center space-x-2 ${className}`}>
        <span className="w-3 h-3 bg-gray-400 rounded-full"></span>
        <span className="text-gray-500 text-sm">在庫不明</span>
        {showDetails && (
          <details className="text-xs text-gray-400">
            <summary className="cursor-pointer">詳細</summary>
            <p className="mt-1">{error || '在庫データが見つかりません'}</p>
          </details>
        )}
      </div>
    );
  }

  const getStatusInfo = () => {
    if (inventory.isOutOfStock) {
      return {
        text: '在庫切れ',
        color: 'red',
        bgColor: 'bg-red-400',
        textColor: 'text-red-600'
      };
    }

    if (inventory.isLowStock) {
      return {
        text: `残り${inventory.availableQuantity}個`,
        color: 'yellow',
        bgColor: 'bg-yellow-400',
        textColor: 'text-yellow-600'
      };
    }

    return {
      text: '在庫あり',
      color: 'green',
      bgColor: 'bg-green-400',
      textColor: 'text-green-600'
    };
  };

  const statusInfo = getStatusInfo();
  
  const sizeClasses = {
    sm: 'text-xs',
    md: 'text-sm',
    lg: 'text-base'
  };

  const dotSizeClasses = {
    sm: 'w-2 h-2',
    md: 'w-3 h-3',
    lg: 'w-4 h-4'
  };

  return (
    <div className={`${className}`}>
      <div className="flex items-center space-x-2">
        <span className={`inline-block ${dotSizeClasses[size]} rounded-full ${statusInfo.bgColor}`}></span>
        <span className={`font-medium ${statusInfo.textColor} ${sizeClasses[size]}`}>
          {statusInfo.text}
        </span>
      </div>
      
      {showDetails && inventory && (
        <div className={`mt-2 space-y-1 ${sizeClasses[size]}`}>
          <div className="text-gray-600">
            <p>利用可能: {inventory.availableQuantity}個</p>
            {inventory.reservedQuantity > 0 && (
              <p>予約済み: {inventory.reservedQuantity}個</p>
            )}
            {inventory.incomingQuantity > 0 && (
              <p>入荷予定: {inventory.incomingQuantity}個</p>
            )}
            {inventory.minimumStockLevel > 0 && (
              <p>最小在庫レベル: {inventory.minimumStockLevel}個</p>
            )}
          </div>
          <p className="text-xs text-gray-500">
            最終更新: {new Date(inventory.lastUpdatedAt).toLocaleString('ja-JP')}
          </p>
        </div>
      )}
    </div>
  );
};

export default InventoryStatus;