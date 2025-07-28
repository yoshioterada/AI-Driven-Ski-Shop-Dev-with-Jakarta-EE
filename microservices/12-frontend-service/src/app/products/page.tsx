'use client';

import React, { useState, useEffect, Suspense, useCallback, useRef } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Image from 'next/image';
import { MainLayout } from '@/components/layout/MainLayout';
import { 
  Product,
  ProductSearchParams, 
  Category, 
  productCatalogApi 
} from '@/services/api/product-catalog';
import { 
  ProductWithInventory,
  productInventoryIntegrationApi 
} from '@/services/api/product-inventory-integration';

/**
 * 商品一覧ページのメインコンポーネント
 */
function ProductsPageContent() {
  const searchParams = useSearchParams();
  const [products, setProducts] = useState<ProductWithInventory[]>([]);
  const [mainCategories, setMainCategories] = useState<Category[]>([]);
  const [subCategories, setSubCategories] = useState<Category[]>([]);
  const [selectedMainCategory, setSelectedMainCategory] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState<ProductSearchParams>({});
  
  // フィルター入力用のローカルstate
  const [searchInput, setSearchInput] = useState('');
  const [minPriceInput, setMinPriceInput] = useState('');
  const [maxPriceInput, setMaxPriceInput] = useState('');
  
  // debounce用のtimeout ref
  const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // URLパラメータから初期フィルターを設定
  useEffect(() => {
    const initialFilters: ProductSearchParams = {};
    
    const search = searchParams.get('search');
    const categoryId = searchParams.get('categoryId');
    const categorySlug = searchParams.get('category'); // カテゴリスラッグ
    const includeSubcategories = searchParams.get('includeSubcategories');
    const brandId = searchParams.get('brandId');
    const minPrice = searchParams.get('minPrice');
    const maxPrice = searchParams.get('maxPrice');
    const sort = searchParams.get('sort');
    const featured = searchParams.get('featured');
    
    if (search) initialFilters.search = search;
    
    // カテゴリスラッグからカテゴリIDを取得
    if (categorySlug && (mainCategories.length > 0 || subCategories.length > 0)) {
      const allCategories = [...mainCategories, ...subCategories];
      const categoryFromSlug = allCategories.find(cat => {
        const slug = cat.path.replace(/^\//, '').replace(/\//g, '-');
        return slug === categorySlug;
      });
      
      if (categoryFromSlug) {
        initialFilters.categoryId = categoryFromSlug.id;
        
        if (categoryFromSlug.level === 1) {
          // サブカテゴリが選択されている場合、対応するメインカテゴリを見つける
          const mainCat = mainCategories.find(mainCat => 
            categoryFromSlug.path.startsWith(mainCat.path + '/')
          );
          if (mainCat) {
            setSelectedMainCategory(mainCat.id);
          }
        } else if (categoryFromSlug.level === 0) {
          // メインカテゴリが選択されている場合
          setSelectedMainCategory(categoryFromSlug.id);
          // includeSubcategories が true の場合は、サブカテゴリも含める
          if (includeSubcategories === 'true') {
            initialFilters.includeSubcategories = true;
          } else {
            delete initialFilters.categoryId; // includeSubcategories が false の場合はメインカテゴリの商品のみ
          }
        }
      }
    } else if (categoryId) {
      // categoryIdから直接カテゴリを取得
      const allCategories = [...mainCategories, ...subCategories];
      const category = allCategories.find(cat => cat.id === categoryId);
      
      if (category) {
        initialFilters.categoryId = categoryId;
        
        // includeSubcategoriesパラメータの処理
        if (includeSubcategories === 'true') {
          initialFilters.includeSubcategories = true;
        }
        
        if (category.level === 1) {
          // サブカテゴリが選択されている場合、対応するメインカテゴリを見つける
          const mainCat = mainCategories.find(mainCat => 
            category.path.startsWith(mainCat.path + '/')
          );
          if (mainCat) {
            setSelectedMainCategory(mainCat.id);
          }
        } else if (category.level === 0) {
          // メインカテゴリが選択されている場合
          setSelectedMainCategory(categoryId);
        }
      } else {
        // カテゴリが見つからないが、categoryIdとincludeSubcategoriesが指定されている場合
        // （Footerからのリンクなど、カテゴリデータの読み込み前にパラメータが設定される場合）
        initialFilters.categoryId = categoryId;
        if (includeSubcategories === 'true') {
          initialFilters.includeSubcategories = true;
        }
        // メインカテゴリとして扱う（レベル0と仮定）
        setSelectedMainCategory(categoryId);
      }
    }
    if (brandId) initialFilters.brandId = brandId;
    if (minPrice) initialFilters.minPrice = Number(minPrice);
    if (maxPrice) initialFilters.maxPrice = Number(maxPrice);
    if (sort) initialFilters.sort = sort as 'price_asc' | 'price_desc' | 'name_asc' | 'name_desc';
    if (featured) initialFilters.featured = featured === 'true';
    
    setFilters(initialFilters);
    
    // ローカルstateも初期化
    setSearchInput(search || '');
    setMinPriceInput(minPrice || '');
    setMaxPriceInput(maxPrice || '');
  }, [searchParams, mainCategories, subCategories]);

  // 商品データを取得
  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        setError(null);
        
        console.log('商品検索フィルター:', filters);
        const productsData = await productInventoryIntegrationApi.getProductsWithInventory(filters);
        console.log('取得した商品数:', productsData.length);
        setProducts(productsData);
      } catch (err) {
        console.error('Failed to fetch products:', err);
        setError('商品データの取得に失敗しました。');
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, [filters]);

  // クリーンアップ処理
  useEffect(() => {
    return () => {
      if (debounceTimeoutRef.current) {
        clearTimeout(debounceTimeoutRef.current);
      }
    };
  }, []);

  // カテゴリデータを取得
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const categoriesData = await productCatalogApi.getAllCategories();
        
        // メインカテゴリとサブカテゴリに分ける
        const mainCats = categoriesData.filter(cat => cat.level === 0);
        const subCats = categoriesData.filter(cat => cat.level === 1);
        
        setMainCategories(mainCats);
        setSubCategories(subCats);
      } catch (err) {
        console.error('Failed to fetch categories:', err);
      }
    };

    fetchCategories();
  }, []);

  // フィルター変更処理
  const handleFilterChange = (newFilters: ProductSearchParams) => {
    setFilters(newFilters);
  };

  // 検索入力の変更処理（debounce付き）
  const handleSearchInputChange = useCallback((value: string) => {
    setSearchInput(value);
    
    // debounce処理
    if (debounceTimeoutRef.current) {
      clearTimeout(debounceTimeoutRef.current);
    }
    
    debounceTimeoutRef.current = setTimeout(() => {
      setFilters(currentFilters => ({
        ...currentFilters,
        search: value || undefined
      }));
    }, 300);
  }, []);

  // 価格入力の変更処理（debounce付き）
  const handlePriceInputChange = useCallback((type: 'min' | 'max', value: string) => {
    if (type === 'min') {
      setMinPriceInput(value);
    } else {
      setMaxPriceInput(value);
    }
    
    // debounce処理
    if (debounceTimeoutRef.current) {
      clearTimeout(debounceTimeoutRef.current);
    }
    
    debounceTimeoutRef.current = setTimeout(() => {
      setFilters(currentFilters => {
        const newFilters = { ...currentFilters };
        if (type === 'min') {
          newFilters.minPrice = value ? Number(value) : undefined;
        } else {
          newFilters.maxPrice = value ? Number(value) : undefined;
        }
        return newFilters;
      });
    }, 300);
  }, []);

  // メインカテゴリ変更処理
  const handleMainCategoryChange = (mainCategoryId: string) => {
    setSelectedMainCategory(mainCategoryId);
    
    // メインカテゴリが変更されたらサブカテゴリの選択をクリア
    const newFilters = { ...filters };
    delete newFilters.categoryId;
    delete newFilters.categoryIds; // 複数カテゴリフィルタもクリア
    
    // メインカテゴリが選択されている場合は、そのメインカテゴリとサブカテゴリを含めて検索
    if (mainCategoryId) {
      newFilters.categoryId = mainCategoryId;
      newFilters.includeSubcategories = true; // サブカテゴリも含める
    }
    
    setFilters(newFilters);
  };

  // サブカテゴリ変更処理
  const handleSubCategoryChange = (subCategoryId: string) => {
    const newFilters = { ...filters };
    
    if (subCategoryId) {
      // 特定のサブカテゴリが選択された場合、そのIDのみを設定
      newFilters.categoryId = subCategoryId;
      delete newFilters.includeSubcategories; // サブカテゴリを含めるフラグを削除
    } else {
      // "全サブカテゴリ"が選択された場合、メインカテゴリIDを設定してサブカテゴリも含める
      if (selectedMainCategory) {
        newFilters.categoryId = selectedMainCategory;
        newFilters.includeSubcategories = true; // サブカテゴリも含める
      } else {
        delete newFilters.categoryId;
        delete newFilters.includeSubcategories;
      }
    }
    
    setFilters(newFilters);
  };

  // 現在選択されているメインカテゴリに対応するサブカテゴリを取得
  const getAvailableSubCategories = () => {
    if (!selectedMainCategory) return [];
    
    const selectedMainCat = mainCategories.find(cat => cat.id === selectedMainCategory);
    if (!selectedMainCat) return [];
    
    return subCategories.filter(subCat => 
      subCat.path.startsWith(selectedMainCat.path + '/')
    );
  };

  // 価格フォーマット
  const formatPrice = (price: number): string => {
    return new Intl.NumberFormat('ja-JP', {
      style: 'currency',
      currency: 'JPY',
    }).format(price);
  };

  // 商品カード コンポーネント
  const ProductCard: React.FC<{ product: Product }> = ({ product }) => {
    const router = useRouter();

    const handleViewDetails = () => {
      console.log('🔍 [ProductCard] 詳細を見るボタンがクリックされました!');
      console.log('🔍 [ProductCard] 商品ID:', product.id);
      console.log('🔍 [ProductCard] 遷移先URL:', `/products/${product.id}`);
      router.push(`/products/${product.id}`);
    };

    return (
    <div className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300">
      <div className="aspect-w-1 aspect-h-1 w-full overflow-hidden bg-gray-200">
        {product.imageUrls && product.imageUrls.length > 0 ? (
          <Image
            className="h-full w-full object-cover object-center group-hover:opacity-75"
            src={product.imageUrls[0]}
            alt={product.name}
            width={300}
            height={300}
          />
        ) : (
          <div className="h-48 bg-gray-200 flex items-center justify-center">
            <span className="text-gray-400">画像なし</span>
          </div>
        )}
      </div>
      
      <div className="p-4">
        <h3 className="text-lg font-medium text-gray-900 mb-2">
          {product.name}
        </h3>
        
        {product.shortDescription && (
          <p className="text-sm text-gray-600 mb-2">
            {product.shortDescription}
          </p>
        )}
        
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center space-x-2">
            <span className="text-lg font-bold text-gray-900">
              {formatPrice(product.currentPrice)}
            </span>
            {product.basePrice !== product.currentPrice && (
              <span className="text-sm text-gray-500 line-through">
                {formatPrice(product.basePrice)}
              </span>
            )}
          </div>
          
          {product.onSale && (
            <span className="bg-red-100 text-red-800 text-xs font-medium px-2.5 py-0.5 rounded">
              セール
            </span>
          )}
        </div>
        
        <p className="text-xs text-gray-500 mb-2">
          SKU: {product.sku}
        </p>
        
        {product.tags && product.tags.length > 0 && (
          <div className="flex flex-wrap gap-1 mb-3">
            {product.tags.slice(0, 3).map((tag, index) => (
              <span 
                key={index}
                className="bg-blue-100 text-blue-800 text-xs font-medium px-2 py-1 rounded"
              >
                {tag}
              </span>
            ))}
          </div>
        )}
        
        <button 
          className="w-full bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors duration-200"
          onClick={handleViewDetails}
        >
          詳細を見る
        </button>
      </div>
    </div>
    );
  };

  // フィルター コンポーネント
  const ProductFilters: React.FC = () => (
    <div className="bg-white p-6 rounded-lg shadow-md mb-6">
      <h2 className="text-lg font-medium text-gray-900 mb-4">フィルター</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        {/* 検索 */}
        <div>
          <label htmlFor="search" className="block text-sm font-medium text-gray-700 mb-1">
            キーワード検索
          </label>
          <input
            type="text"
            id="search"
            value={searchInput}
            onChange={(e) => handleSearchInputChange(e.target.value)}
            placeholder="商品名やブランドで検索"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* メインカテゴリ */}
        <div>
          <label htmlFor="mainCategory" className="block text-sm font-medium text-gray-700 mb-1">
            メインカテゴリ
          </label>
          <select
            id="mainCategory"
            value={selectedMainCategory}
            onChange={(e) => handleMainCategoryChange(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">すべてのメインカテゴリ</option>
            {mainCategories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>

        {/* サブカテゴリ */}
        <div>
          <label htmlFor="subCategory" className="block text-sm font-medium text-gray-700 mb-1">
            サブカテゴリ
          </label>
          <select
            id="subCategory"
            value={(() => {
              // 現在のフィルター状態に基づいて選択値を決定
              if (!selectedMainCategory) return '';
              
              // includeSubcategoriesがtrueで、categoryIdがメインカテゴリIDと同じ場合は「すべてのサブカテゴリ」
              if (filters.includeSubcategories && filters.categoryId === selectedMainCategory) {
                return '';
              }
              
              // そうでなければ、現在のcategoryIdを返す
              return filters.categoryId || '';
            })()}
            onChange={(e) => handleSubCategoryChange(e.target.value)}
            disabled={!selectedMainCategory || getAvailableSubCategories().length === 0}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
          >
            <option value="">
              {!selectedMainCategory 
                ? 'まずメインカテゴリを選択してください' 
                : getAvailableSubCategories().length === 0
                  ? 'サブカテゴリはありません'
                  : 'すべてのサブカテゴリ'
              }
            </option>
            {selectedMainCategory && getAvailableSubCategories().map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>

        {/* 価格範囲 */}
        <div className="md:col-span-2 lg:col-span-1">
          <label className="block text-sm font-medium text-gray-700 mb-1">
            価格範囲
          </label>
          <div className="flex space-x-2">
            <input
              type="number"
              value={minPriceInput}
              onChange={(e) => handlePriceInputChange('min', e.target.value)}
              placeholder="最低価格"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <input
              type="number"
              value={maxPriceInput}
              onChange={(e) => handlePriceInputChange('max', e.target.value)}
              placeholder="最高価格"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        {/* ソート */}
        <div className="md:col-span-2 lg:col-span-1">
          <label htmlFor="sort" className="block text-sm font-medium text-gray-700 mb-1">
            並び順
          </label>
          <select
            id="sort"
            value={filters.sort || ''}
            onChange={(e) => handleFilterChange({ ...filters, sort: e.target.value as 'price_asc' | 'price_desc' | 'name_asc' | 'name_desc' || undefined })}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">デフォルト</option>
            <option value="price_asc">価格: 安い順</option>
            <option value="price_desc">価格: 高い順</option>
            <option value="name_asc">名前: A-Z</option>
            <option value="name_desc">名前: Z-A</option>
          </select>
        </div>
      </div>

      {/* 特別フィルター */}
      <div className="flex flex-wrap gap-4 mt-4">
        <label className="flex items-center">
          <input
            type="checkbox"
            checked={filters.featured || false}
            onChange={(e) => handleFilterChange({ ...filters, featured: e.target.checked || undefined })}
            className="mr-2"
          />
          <span className="text-sm text-gray-700">注目商品のみ</span>
        </label>
        
        <label className="flex items-center">
          <input
            type="checkbox"
            checked={filters.onSale || false}
            onChange={(e) => handleFilterChange({ ...filters, onSale: e.target.checked || undefined })}
            className="mr-2"
          />
          <span className="text-sm text-gray-700">セール商品のみ</span>
        </label>
      </div>
    </div>
  );

  return (
    <MainLayout>
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">商品一覧</h1>
        
        {/* フィルター */}
        <ProductFilters />
        
        {/* 商品数表示 */}
        <div className="mb-4 text-sm text-gray-600">
          {loading ? '読み込み中...' : `${products.length}件の商品が見つかりました`}
        </div>

        {/* エラー表示 */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-md p-4 mb-6">
            <div className="flex">
              <div className="text-red-700">
                <p>{error}</p>
              </div>
            </div>
          </div>
        )}

        {/* 商品一覧 */}
        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {[...Array(8)].map((_, index) => (
              <div key={index} className="bg-white rounded-lg shadow-md overflow-hidden animate-pulse">
                <div className="h-48 bg-gray-200"></div>
                <div className="p-4">
                  <div className="h-4 bg-gray-200 rounded mb-2"></div>
                  <div className="h-3 bg-gray-200 rounded mb-2"></div>
                  <div className="h-4 bg-gray-200 rounded mb-2"></div>
                  <div className="h-8 bg-gray-200 rounded"></div>
                </div>
              </div>
            ))}
          </div>
        ) : products.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg">条件に合う商品が見つかりませんでした。</p>
          </div>
        )}
      </div>
    </MainLayout>
  );
}

/**
 * 商品一覧ページ
 * Product Catalog Serviceから実際の商品データを取得して表示
 */
export default function ProductsPage() {
  return (
    <Suspense fallback={
      <MainLayout>
        <div className="container mx-auto px-4 py-8">
          <div className="text-center">読み込み中...</div>
        </div>
      </MainLayout>
    }>
      <ProductsPageContent />
    </Suspense>
  );
}
