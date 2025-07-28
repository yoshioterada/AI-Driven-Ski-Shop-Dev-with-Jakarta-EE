'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import Image from 'next/image';
import Link from 'next/link';
import { MainLayout } from '@/components/layout/MainLayout';
import { useCartStore } from '@/stores/cartStore';
import type { Product } from '@/types/product';
import { getCategoryHierarchy, getCategoryInfoFilterUrl, CategoryInfo } from '@/utils/categoryUtils';
import InventoryStatus from '@/components/InventoryStatus';

export default function ProductDetailPage() {
  const params = useParams();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [categoryHierarchy, setCategoryHierarchy] = useState<{
    mainCategory: CategoryInfo | null;
    subCategory: CategoryInfo | null;
  }>({ mainCategory: null, subCategory: null });
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  const { addItem, loading: cartLoading, error: cartError } = useCartStore();

  useEffect(() => {
    const loadProduct = async () => {
      try {
        setLoading(true);
        const productId = params.id as string;
        
        // まず直接APIを呼び出してみる
        const response = await fetch(
          `${process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8083'}/api/v1/products/${productId}`
        );
        
        if (!response.ok) {
          throw new Error(`商品が見つかりません (${response.status})`);
        }
        
        const apiProduct = await response.json();
        
        // APIレスポンスをProduct型に変換
        const mappedProduct: Product = {
          id: apiProduct.id,
          name: apiProduct.name,
          description: apiProduct.shortDescription || apiProduct.description || '',
          shortDescription: apiProduct.shortDescription,
          price: apiProduct.currentPrice,
          originalPrice: apiProduct.basePrice,
          discountPercentage: apiProduct.discountPercentage,
          sku: apiProduct.sku,
          categoryId: apiProduct.category.id,
          category: {
            id: apiProduct.category.id,
            name: apiProduct.category.name,
            slug: apiProduct.category.path.replace(/^\//, '').replace(/\//g, '-'),
            description: '',
            children: [],
            isActive: true,
            sortOrder: 0,
            productCount: 0,
          },
          brandId: apiProduct.brand.id,
          brand: apiProduct.brand.name ? {
            id: apiProduct.brand.id,
            name: apiProduct.brand.name,
            slug: apiProduct.brand.name.toLowerCase().replace(/\s+/g, '-'),
            description: '',
            logo: apiProduct.brand.logoUrl,
            isActive: true,
          } : undefined,
          images: apiProduct.primaryImageUrl ? [{
            id: `${apiProduct.id}-primary`,
            url: apiProduct.primaryImageUrl,
            alt: apiProduct.name,
            isPrimary: true,
            sortOrder: 0,
          }] : [],
          attributes: [],
          variants: [],
          inventory: {
            quantity: 10, // デフォルト在庫数
            reservedQuantity: 0,
            availableQuantity: 10, // デフォルト利用可能数
            lowStockThreshold: 5,
            status: 'in_stock' as const, // デフォルトで在庫ありとする
          },
          specifications: [],
          reviews: [],
          rating: apiProduct.rating || 0,
          reviewCount: apiProduct.reviewCount,
          tags: apiProduct.tags,
          isActive: true,
          isFeatured: apiProduct.featured,
          isOnSale: apiProduct.onSale,
          createdAt: apiProduct.createdAt,
          updatedAt: apiProduct.createdAt,
        };
        
        setProduct(mappedProduct);
        
        // カテゴリ階層を取得
        if (apiProduct.category.path) {
          const hierarchy = await getCategoryHierarchy(apiProduct.category.path);
          setCategoryHierarchy(hierarchy);
        }
      } catch (err) {
        console.error('商品詳細の取得に失敗しました:', err);
        setError(err instanceof Error ? err.message : '商品の読み込みに失敗しました');
      } finally {
        setLoading(false);
      }
    };

    if (params.id) {
      loadProduct();
    }
  }, [params.id]);

  const handleAddToCart = async () => {
    if (!product) return;
    
    setIsAddingToCart(true);
    try {
      console.log('Adding to cart:', product.id, product.sku, quantity);
      await addItem(product.id, product.sku, quantity);
      
      // 成功通知
      alert(`${product.name} を ${quantity} 個カートに追加しました`);
    } catch (error) {
      console.error('カートへの追加に失敗しました:', error);
      const errorMessage = cartError || '商品の追加に失敗しました';
      alert(`エラー: ${errorMessage}`);
    } finally {
      setIsAddingToCart(false);
    }
  };

  const handleBuyNow = () => {
    if (!product) return;
    
    // 即購入機能は後で実装
    alert(`${product.name} の購入手続きに進みます`);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">商品情報を読み込み中...</p>
        </div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <MainLayout>
        <div className="min-h-screen bg-gray-50 flex items-center justify-center">
          <div className="text-center">
            <h1 className="text-2xl font-bold text-gray-800 mb-4">商品が見つかりません</h1>
            <p className="text-gray-600 mb-6">{error || '指定された商品は存在しません'}</p>
            <Link
              href="/products"
              className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-colors"
            >
              商品一覧に戻る
            </Link>
          </div>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* パンくずナビ */}
        <nav className="flex items-center space-x-2 text-sm text-gray-600 mb-8">
          <Link href="/" className="hover:text-blue-600">ホーム</Link>
          <span>/</span>
          <Link href="/products" className="hover:text-blue-600">商品一覧</Link>
          {categoryHierarchy.mainCategory && (
            <>
              <span>/</span>
              <Link 
                href={getCategoryInfoFilterUrl(categoryHierarchy.mainCategory)} 
                className="hover:text-blue-600"
              >
                {categoryHierarchy.mainCategory.name}
              </Link>
            </>
          )}
          {categoryHierarchy.subCategory && (
            <>
              <span>/</span>
              <Link 
                href={getCategoryInfoFilterUrl({
                  id: categoryHierarchy.subCategory.id,
                  name: categoryHierarchy.subCategory.name,
                  description: categoryHierarchy.subCategory.description || '',
                })} 
                className="hover:text-blue-600"
              >
                {categoryHierarchy.subCategory.name}
              </Link>
            </>
          )}
          <span>/</span>
          <span className="text-gray-800">{product.name}</span>
        </nav>

        <div className="bg-white rounded-lg shadow-lg overflow-hidden">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 p-8">
            {/* 商品画像セクション */}
            <div className="space-y-4">
              <div className="aspect-square bg-gray-100 rounded-lg overflow-hidden">
                {product.images[selectedImageIndex] ? (
                  <Image
                    src={product.images[selectedImageIndex].url}
                    alt={product.images[selectedImageIndex].alt}
                    width={600}
                    height={600}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      const target = e.target as HTMLImageElement;
                      target.style.display = 'none';
                      // 親要素に代替コンテンツを表示
                      const parent = target.parentElement;
                      if (parent) {
                        parent.innerHTML = `
                          <div class="w-full h-full flex items-center justify-center text-gray-400 bg-gray-100">
                            <div class="text-center">
                              <div class="text-6xl mb-2">📷</div>
                              <p>画像を読み込めません</p>
                            </div>
                          </div>
                        `;
                      }
                    }}
                  />
                ) : (
                    <div className="w-full h-full flex items-center justify-center text-gray-400 bg-gray-100">
                      <div className="text-center">
                        <div className="text-6xl mb-2">📷</div>
                        <p>画像なし</p>
                      </div>
                    </div>
                )}
              </div>

              {/* サムネイル（複数画像がある場合） */}
              {product.images.length > 1 && (
                <div className="flex space-x-2">
                  {product.images.map((image, index) => (
                    <button
                      key={image.id}
                      onClick={() => setSelectedImageIndex(index)}
                      className={`w-20 h-20 rounded-md overflow-hidden border-2 ${
                        selectedImageIndex === index
                          ? 'border-blue-600'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                      aria-label={`商品画像 ${index + 1}を表示`}
                    >
                      <Image
                        src={image.url}
                        alt={image.alt}
                        width={80}
                        height={80}
                        className="w-full h-full object-cover"
                      />
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* 商品情報セクション */}
            <div className="space-y-6">
              {/* 商品名とブランド */}
              <div>
                {product.brand && (
                  <p className="text-blue-600 font-medium mb-2">{product.brand.name}</p>
                )}
                <h1 className="text-3xl font-bold text-gray-900 mb-2">{product.name}</h1>
                <p className="text-gray-600">{product.shortDescription}</p>
              </div>

              {/* レーティングとレビュー */}
              <div className="flex items-center space-x-4">
                <div className="flex items-center">
                  {[...Array(5)].map((_, i) => (
                    <span
                      key={i}
                      className={`text-lg ${
                        i < Math.floor(product.rating)
                          ? 'text-yellow-400'
                          : 'text-gray-300'
                      }`}
                    >
                      ★
                    </span>
                  ))}
                  <span className="ml-2 text-gray-600">
                    {product.rating.toFixed(1)} ({product.reviewCount} レビュー)
                  </span>
                </div>
              </div>

              {/* 価格 */}
              <div className="space-y-2">
                <div className="flex items-center space-x-4">
                  <span className="text-3xl font-bold text-gray-900">
                    ¥{product.price.toLocaleString()}
                  </span>
                  {product.originalPrice && product.originalPrice > product.price && (
                    <>
                      <span className="text-lg text-gray-500 line-through">
                        ¥{product.originalPrice.toLocaleString()}
                      </span>
                      <span className="bg-red-100 text-red-800 px-2 py-1 rounded text-sm font-medium">
                        {product.discountPercentage}% OFF
                      </span>
                    </>
                  )}
                </div>
                {product.isOnSale && (
                  <p className="text-red-600 font-medium">セール中！</p>
                )}
              </div>

              {/* 在庫状況 */}
              <InventoryStatus 
                sku={product.sku} 
                showDetails={true} 
                size="md"
                className="mb-4"
              />

              {/* SKU */}
              <p className="text-sm text-gray-500">
                商品コード: {product.sku}
              </p>

              {/* 数量選択 */}
              {product.inventory.status === 'in_stock' && (
                <div className="space-y-4">
                  <div className="flex items-center space-x-4">
                    <label htmlFor="quantity" className="font-medium text-gray-700">
                      数量:
                    </label>
                    <select
                      id="quantity"
                      value={quantity}
                      onChange={(e) => setQuantity(Number(e.target.value))}
                      className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      {[...Array(Math.min(10, product.inventory.availableQuantity))].map((_, i) => (
                        <option key={i + 1} value={i + 1}>
                          {i + 1}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* アクションボタン */}
                  <div className="flex space-x-4">
                    <button
                      onClick={handleAddToCart}
                      disabled={isAddingToCart || cartLoading}
                      className="flex-1 bg-blue-600 text-white py-3 px-6 rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {isAddingToCart || cartLoading ? 'カートに追加中...' : 'カートに追加'}
                    </button>
                    <button
                      onClick={handleBuyNow}
                      className="flex-1 bg-orange-600 text-white py-3 px-6 rounded-lg font-medium hover:bg-orange-700 transition-colors"
                    >
                      今すぐ購入
                    </button>
                  </div>
                </div>
              )}

              {/* タグ */}
              {product.tags.length > 0 && (
                <div className="space-y-2">
                  <h3 className="font-medium text-gray-700">タグ:</h3>
                  <div className="flex flex-wrap gap-2">
                    {product.tags.map((tag, index) => (
                      <span
                        key={index}
                        className="bg-gray-100 text-gray-700 px-3 py-1 rounded-full text-sm"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* 商品詳細説明 */}
          <div className="border-t border-gray-200 p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">商品詳細</h2>
            <div className="prose max-w-none">
              <p className="text-gray-700 leading-relaxed">
                {product.description || product.shortDescription || '詳細な商品説明は現在準備中です。'}
              </p>
            </div>
          </div>

          {/* 仕様・スペック */}
          <div className="border-t border-gray-200 p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">仕様</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <div className="flex justify-between py-2 border-b border-gray-100">
                  <span className="font-medium text-gray-700">カテゴリ</span>
                  <span className="text-gray-600">{product.category.name}</span>
                </div>
                {product.brand && (
                  <div className="flex justify-between py-2 border-b border-gray-100">
                    <span className="font-medium text-gray-700">ブランド</span>
                    <span className="text-gray-600">{product.brand.name}</span>
                  </div>
                )}
                <div className="flex justify-between py-2 border-b border-gray-100">
                  <span className="font-medium text-gray-700">商品コード</span>
                  <span className="text-gray-600">{product.sku}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    </MainLayout>
  );
}
