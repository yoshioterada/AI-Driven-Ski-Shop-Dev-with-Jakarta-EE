'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { HeartIcon, ShoppingCartIcon } from '@heroicons/react/24/outline';
import { HeartIcon as HeartIconSolid } from '@heroicons/react/24/solid';
import { Card, CardContent, Button, Badge } from '@/components/ui';
import { Product } from '@/types/product';
import { cn } from '@/utils/helpers';
import InventoryStatus from '@/components/InventoryStatus';

export interface ProductCardProps {
  product: Product;
  onAddToCart?: (productId: string) => void;
  onToggleFavorite?: (productId: string) => void;
  isFavorite?: boolean;
  isLoading?: boolean;
  className?: string;
  size?: 'sm' | 'md' | 'lg';
}

export const ProductCard: React.FC<ProductCardProps> = ({
  product,
  onAddToCart,
  onToggleFavorite,
  isFavorite = false,
  isLoading = false,
  className,
  size = 'md',
}) => {
  const router = useRouter();
  
  const sizeClasses = {
    sm: 'w-full max-w-xs',
    md: 'w-full max-w-sm',
    lg: 'w-full max-w-md',
  };

  const imageClasses = {
    sm: 'h-32',
    md: 'h-48',
    lg: 'h-64',
  };

  const isOutOfStock = product.inventory.availableQuantity === 0;
  const isLowStock = product.inventory.availableQuantity <= product.inventory.lowStockThreshold;
  const hasDiscount = product.originalPrice && product.originalPrice > product.price;

  const handleAddToCart = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!isOutOfStock && onAddToCart) {
      onAddToCart(product.id);
    }
  };

  const handleToggleFavorite = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (onToggleFavorite) {
      onToggleFavorite(product.id);
    }
  };

  const handleViewDetails = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    console.log('🔍 [ProductCard] 詳細を見るボタンがクリックされました!');
    console.log('🔍 [ProductCard] 商品ID:', product.id);
    console.log('🔍 [ProductCard] 遷移先URL:', `/products/${product.id}`);
    console.log('🔍 [ProductCard] router.push実行前');
    router.push(`/products/${product.id}`);
    console.log('🔍 [ProductCard] router.push実行後');
  };

  return (
    <Card
      className={cn(
        'group relative overflow-hidden transition-all duration-200',
        sizeClasses[size],
        className
      )}
      padding="none"
      hover="lift"
    >
      {/* Product Image */}
      <div className={cn('relative overflow-hidden bg-gray-100', imageClasses[size])}>
        <Link href={`/products/${product.id}`}>
          {product.images && product.images.length > 0 ? (
            <Image
              src={product.images[0].url}
              alt={product.images[0].alt || product.name}
              fill
              className="object-cover transition-transform duration-200 group-hover:scale-105"
              sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
            />
          ) : (
            <div className="flex items-center justify-center h-full bg-gray-200">
              <span className="text-gray-400 text-sm">画像なし</span>
            </div>
          )}

          {/* Badges */}
          <div className="absolute top-2 left-2 flex flex-col space-y-1">
            {product.isFeatured && (
              <Badge variant="primary" size="sm">
                おすすめ
              </Badge>
            )}
            {product.isOnSale && (
              <Badge variant="error" size="sm">
                セール
              </Badge>
            )}
            {isLowStock && !isOutOfStock && (
              <Badge variant="warning" size="sm">
                残りわずか
              </Badge>
            )}
            {isOutOfStock && (
              <Badge variant="default" size="sm">
                在庫切れ
              </Badge>
            )}
          </div>
        </Link>

        {/* Favorite Button */}
        <button
          type="button"
          className="absolute top-2 right-2 p-2 bg-white bg-opacity-80 rounded-full hover:bg-opacity-100 transition-all duration-200 opacity-0 group-hover:opacity-100"
          onClick={handleToggleFavorite}
          aria-label={isFavorite ? 'お気に入りから削除' : 'お気に入りに追加'}
        >
          {isFavorite ? (
            <HeartIconSolid className="h-5 w-5 text-red-500" />
          ) : (
            <HeartIcon className="h-5 w-5 text-gray-600" />
          )}
        </button>

        {/* Quick Add to Cart Button */}
        <div className="absolute bottom-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
          <Button
            size="sm"
            variant={isOutOfStock ? 'ghost' : 'default'}
            onClick={handleAddToCart}
            disabled={isOutOfStock || isLoading}
            className="shadow-lg"
          >
            <ShoppingCartIcon className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Product Info */}
      <CardContent className="p-4">
          {/* Category */}
          {product.category && (
            <p className="text-xs text-gray-500 mb-1">
              {product.category.name}
            </p>
          )}

          {/* Product Name */}
          <h3 className="text-sm font-medium text-gray-900 mb-2 line-clamp-2">
            {product.name}
          </h3>

          {/* Rating */}
          <div className="flex items-center mb-2">
            <div className="flex">
              {Array.from({ length: 5 }, (_, i) => (
                <span
                  key={i}
                  className={cn(
                    'text-xs',
                    i < Math.floor(product.rating)
                      ? 'text-yellow-400'
                      : 'text-gray-300'
                  )}
                >
                  ★
                </span>
              ))}
            </div>
            <span className="text-xs text-gray-500 ml-1">
              ({product.reviewCount})
            </span>
          </div>

          {/* Price */}
          <div className="flex items-center space-x-2 mb-3">
            {hasDiscount ? (
              <>
                <span className="text-lg font-bold text-red-600">
                  ¥{product.price.toLocaleString()}
                </span>
                <span className="text-sm text-gray-400 line-through">
                  ¥{product.originalPrice!.toLocaleString()}
                </span>
              </>
            ) : (
              <span className="text-lg font-bold text-gray-900">
                ¥{product.price.toLocaleString()}
              </span>
            )}
          </div>

          {/* Stock Status */}
          <div className="mb-3">
            <InventoryStatus 
              sku={product.sku} 
              size="sm"
            />
          </div>

          {/* Detail Button */}
          <div className="flex space-x-2">
            <Button
              variant="outline"
              size="sm"
              className="flex-1 text-blue-600 border-blue-600 hover:bg-blue-50"
              onClick={(e) => {
                console.log('🟢 [ProductCard] ボタンクリックイベント発生!');
                handleViewDetails(e);
              }}
            >
              詳細を見る
            </Button>
            <Button
              size="sm"
              variant={isOutOfStock ? 'ghost' : 'outline'}
              onClick={handleAddToCart}
              disabled={isOutOfStock || isLoading}
              className={`flex-1 ${!isOutOfStock ? 'text-blue-600 border-blue-600 hover:bg-blue-50' : ''}`}
            >
              {isOutOfStock ? '在庫切れ' : 'カートに追加'}
            </Button>
          </div>
        </CardContent>
    </Card>
  );
};
