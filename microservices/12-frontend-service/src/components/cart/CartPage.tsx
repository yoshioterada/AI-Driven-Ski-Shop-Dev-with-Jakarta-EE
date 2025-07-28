/**
 * ショッピングカートページ
 * カート内容の詳細表示と操作を行うページ
 */

'use client';

import React, { useEffect } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { 
  TrashIcon, 
  PlusIcon, 
  MinusIcon, 
  ArrowLeftIcon,
  ShoppingCartIcon 
} from '@heroicons/react/24/outline';
import { MainLayout } from '../layout/MainLayout';
import { useCartStore } from '../../stores/cartStore';
import { CartItemResponse } from '../../types/cart';
import InventoryStatus from '../InventoryStatus';

// カートアイテムコンポーネント
const CartItemRow = ({ 
  item, 
  onUpdateQuantity, 
  onRemove, 
  loading 
}: { 
  item: CartItemResponse;
  onUpdateQuantity: (sku: string, quantity: number) => void;
  onRemove: (sku: string) => void;
  loading: boolean;
}) => (
  <div className="p-6">
    <div className="flex items-center space-x-4">
      {/* チェックボックス */}
      <div className="flex-shrink-0">
        <input type="checkbox" defaultChecked className="rounded" />
      </div>

      {/* 商品画像 */}
      <div className="flex-shrink-0 w-20 h-20 bg-gray-100 rounded-lg overflow-hidden">
        {item.productImageUrl ? (
          <Image
            src={item.productImageUrl}
            alt={item.productName}
            width={80}
            height={80}
            className="w-full h-full object-cover"
            onError={(e) => {
              const target = e.target as HTMLImageElement;
              target.style.display = 'none';
              const parent = target.parentElement;
              if (parent) {
                parent.innerHTML = `
                  <div class="w-full h-full flex items-center justify-center text-gray-400 bg-gray-100">
                    <div class="text-center">
                      <div class="text-lg mb-1">📷</div>
                      <p class="text-xs">No Image</p>
                    </div>
                  </div>
                `;
              }
            }}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-400 bg-gray-100">
            <div className="text-center">
              <div className="text-lg mb-1">📷</div>
              <p className="text-xs">No Image</p>
            </div>
          </div>
        )}
      </div>

      {/* 商品情報 */}
      <div className="flex-1 min-w-0">
        <div className="flex justify-between items-start">
          <div className="flex-1">
            <Link
              href={`/products/${item.productId}`}
              className="text-blue-600 hover:text-blue-800 font-medium block truncate"
            >
              {item.productName}
            </Link>
            <p className="text-sm text-gray-500 mt-1">
              SKU: {item.sku}
            </p>
            <div className="mt-1">
              <InventoryStatus 
                sku={item.sku} 
                size="sm"
              />
            </div>
            
            {/* 数量変更コントロール */}
            <div className="flex items-center space-x-2 mt-3">
              <button
                onClick={() => onUpdateQuantity(item.sku, item.quantity - 1)}
                disabled={loading || item.quantity <= 1}
                className="w-8 h-8 flex items-center justify-center border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                aria-label="数量を減らす"
              >
                <MinusIcon className="w-3 h-3" />
              </button>
              <span className="min-w-[2rem] text-center font-medium">
                {loading ? '...' : item.quantity}
              </span>
              <button
                onClick={() => onUpdateQuantity(item.sku, item.quantity + 1)}
                disabled={loading}
                className="w-8 h-8 flex items-center justify-center border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                aria-label="数量を増やす"
              >
                <PlusIcon className="w-3 h-3" />
              </button>
              <span className="text-sm text-gray-500 ml-2">|</span>
              <button
                onClick={() => onRemove(item.sku)}
                disabled={loading}
                className="text-red-600 hover:text-red-800 text-sm disabled:opacity-50"
                aria-label="商品を削除"
              >
                削除
              </button>
            </div>
          </div>

          {/* 価格表示 */}
          <div className="text-right ml-4">
            <div className="text-lg font-bold text-gray-900">
              ¥{item.totalPrice.toLocaleString()}
            </div>
            <div className="text-sm text-gray-500">
              ¥{item.unitPrice.toLocaleString()} × {item.quantity}
            </div>
            <div className="text-sm text-green-600 mt-1">
              ポイント: {Math.floor(item.totalPrice * 0.01)}pt (1%)
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
);

export const CartPage: React.FC = () => {
  const {
    cart,
    loading,
    error,
    initializeCart,
    removeItem,
    updateQuantity,
    clearCart,
    clearError
  } = useCartStore();

  // コンポーネントマウント時にカートを初期化
  useEffect(() => {
    if (!cart) {
      console.log('Initializing cart...');
      initializeCart();
    } else {
      console.log('Current cart state:', cart);
    }
  }, [cart, initializeCart]);

  // すべての商品をカートから削除
  const handleClearAllItems = async () => {
    if (cart && cart.items.length > 0) {
      const confirmed = confirm('カート内のすべての商品を削除しますか？');
      if (confirmed) {
        console.log('Clearing cart with ID:', cart.cartId);
        try {
          await clearCart();
          console.log('Cart cleared successfully');
        } catch (error) {
          console.error('カートのクリアに失敗しました:', error);
          // より詳細なエラー情報を表示
          let errorMessage = 'カートのクリアに失敗しました';
          if (error instanceof Error) {
            errorMessage = error.message;
          }
          alert(`エラー: ${errorMessage}`);
        }
      }
    } else {
      console.log('Cart is empty or not loaded');
    }
  };

  // エラー表示
  if (error) {
    return (
      <MainLayout>
        <div className="min-h-screen bg-gray-50 py-8">
          <div className="max-w-4xl mx-auto px-4">
            <div className="bg-red-50 border border-red-200 text-red-700 px-6 py-4 rounded-lg">
              <div className="flex justify-between items-center">
                <div>
                  <h2 className="text-lg font-medium">エラーが発生しました</h2>
                  <p className="mt-1">{error}</p>
                </div>
                <button
                  onClick={clearError}
                  className="text-red-500 hover:text-red-700"
                  aria-label="エラーメッセージを閉じる"
                >
                  ×
                </button>
              </div>
            </div>
          </div>
        </div>
      </MainLayout>
    );
  }

  // ローディング表示
  if (loading && !cart) {
    return (
      <MainLayout>
        <div className="min-h-screen bg-gray-50 py-8">
          <div className="max-w-4xl mx-auto px-4">
            <div className="flex justify-center items-center py-16">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900"></div>
            </div>
          </div>
        </div>
      </MainLayout>
    );
  }

  // カートが空の場合
  if (cart && cart.items.length === 0) {
    return (
      <MainLayout>
        <div className="min-h-screen bg-gray-50 py-8">
          <div className="max-w-4xl mx-auto px-4">
            <div className="text-center py-16">
              <ShoppingCartIcon className="w-24 h-24 mx-auto text-gray-300 mb-6" />
              <h1 className="text-2xl font-semibold text-gray-900 mb-4">
                カートは空です
              </h1>
              <p className="text-gray-600 mb-8">
                お気に入りの商品を見つけて、カートに追加してください。
              </p>
              <Link
                href="/categories"
                className="inline-flex items-center bg-blue-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-blue-700 transition-colors"
              >
                <ArrowLeftIcon className="w-5 h-5 mr-2" />
                商品を見る
              </Link>
            </div>
          </div>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* パンくずナビ */}
          <nav className="flex items-center space-x-2 text-sm text-gray-600 mb-8">
            <Link href="/" className="hover:text-blue-600">ホーム</Link>
            <span>/</span>
            <span className="text-gray-800">ショッピングカート</span>
          </nav>

          {/* ページヘッダー */}
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-gray-900 mb-4">
              ショッピングカート
            </h1>
            {/* デバッグ情報（開発時のみ） */}
            {process.env.NODE_ENV === 'development' && cart && (
              <div className="bg-gray-100 p-2 rounded text-sm text-gray-600 mb-4">
                Debug: Cart ID = {cart.cartId}, Items = {cart.items.length}
              </div>
            )}
          </div>

          {cart && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* カート商品一覧 */}
              <div className="lg:col-span-2">
                <div className="bg-white rounded-lg shadow-lg overflow-hidden">
                  <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
                    <div>
                      <button
                        onClick={handleClearAllItems}
                        className="text-lg font-bold text-blue-600 hover:text-blue-800 transition-colors cursor-pointer"
                        disabled={loading}
                      >
                        すべての商品の選択解除
                      </button>
                      <p className="text-sm text-gray-600">
                        ご利用可能ポイント: 4,710
                      </p>
                    </div>
                    <span className="text-sm text-gray-600">価格</span>
                  </div>

                  <div className="divide-y divide-gray-200">
                    {cart.items.map((item) => (
                      <CartItemRow
                        key={item.itemId}
                        item={item}
                        onUpdateQuantity={updateQuantity}
                        onRemove={removeItem}
                        loading={loading}
                      />
                    ))}
                  </div>
                </div>
              </div>

              {/* 注文概要 */}
              <div className="lg:col-span-1">
                <div className="bg-white rounded-lg shadow-lg p-6 sticky top-4">
                  <h3 className="text-lg font-bold text-gray-900 mb-6">
                    小計 ({cart.itemCount} 個の商品)
                  </h3>
                  
                  {/* 価格詳細 */}
                  <div className="space-y-3 mb-6">
                    <div className="flex justify-between items-center">
                      <span className="text-gray-600">小計 (税抜):</span>
                      <span className="font-medium">¥{cart.totals.subtotalAmount.toLocaleString()}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-gray-600">税金:</span>
                      <span className="font-medium">¥{cart.totals.taxAmount.toLocaleString()}</span>
                    </div>
                    {cart.totals.discountAmount > 0 && (
                      <div className="flex justify-between items-center text-green-600">
                        <span>割引:</span>
                        <span className="font-medium">-¥{cart.totals.discountAmount.toLocaleString()}</span>
                      </div>
                    )}
                    <div className="border-t border-gray-200 pt-3">
                      <div className="flex justify-between items-center">
                        <span className="text-lg font-bold text-gray-900">合計 (税込):</span>
                        <span className="text-2xl font-bold text-gray-900">¥{cart.totals.totalAmount.toLocaleString()}</span>
                      </div>
                    </div>
                  </div>
                  
                  <div className="space-y-3 mb-6">
                    <div className="flex items-center space-x-2">
                      <input type="checkbox" id="gift" className="rounded" />
                      <label htmlFor="gift" className="text-sm text-gray-700">
                        ギフトに設定 (贈り主の氏名を表示)
                      </label>
                    </div>
                  </div>

                  <button className="w-full bg-blue-600 text-white py-3 px-4 rounded-lg font-medium hover:bg-blue-700 transition-colors mb-3">
                    レジに進む
                  </button>
                  
                  <div className="text-center">
                    <p className="text-lg font-bold text-gray-900 mb-2">
                      もう一度買う
                    </p>
                    <Link
                      href="/categories"
                      className="text-blue-600 hover:text-blue-800 text-sm"
                    >
                      買い物を続ける
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </MainLayout>
  );
};
