import { Category } from '@/services/api/product-catalog';

export interface CategoryInfo {
  id: string;
  name: string;
  path: string;
  level: number;
  slug: string;
}

interface ApiCategory {
  id: string;
  name: string;
  path: string;
  level: number;
  description: string;
  sortOrder: number;
  imageUrl: string | null;
  productCount: number;
  active: boolean;
}

/**
 * カテゴリパスからメインカテゴリとサブカテゴリの情報を取得
 */
export async function getCategoryHierarchy(categoryPath: string): Promise<{
  mainCategory: CategoryInfo | null;
  subCategory: CategoryInfo | null;
}> {
  try {
    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8083'}/api/v1/categories`
    );
    
    if (!response.ok) {
      throw new Error('カテゴリ情報の取得に失敗しました');
    }
    
    const categories: ApiCategory[] = await response.json();
    
    // パスの正規化（先頭の/を除去）
    const normalizedPath = categoryPath.replace(/^\//, '');
    const pathSegments = normalizedPath.split('/');
    
    let mainCategory: CategoryInfo | null = null;
    let subCategory: CategoryInfo | null = null;
    
    if (pathSegments.length >= 1) {
      // メインカテゴリを検索（level: 0）
      const mainCategoryPath = `/${pathSegments[0]}`;
      const foundMainCategory = categories.find((cat: ApiCategory) => 
        cat.level === 0 && cat.path === mainCategoryPath
      );
      
      if (foundMainCategory) {
        mainCategory = {
          ...foundMainCategory,
          slug: foundMainCategory.path.replace(/^\//, '').replace(/\//g, '-')
        };
      }
    }
    
    if (pathSegments.length >= 2) {
      // サブカテゴリを検索（level: 1）
      const subCategoryPath = `/${pathSegments[0]}/${pathSegments[1]}`;
      const foundSubCategory = categories.find((cat: ApiCategory) => 
        cat.level === 1 && cat.path === subCategoryPath
      );
      
      if (foundSubCategory) {
        subCategory = {
          ...foundSubCategory,
          slug: foundSubCategory.path.replace(/^\//, '').replace(/\//g, '-')
        };
      }
    }
    
    return { mainCategory, subCategory };
  } catch (error) {
    console.error('カテゴリ階層の取得に失敗しました:', error);
    return { mainCategory: null, subCategory: null };
  }
}

/**
 * カテゴリスラッグから商品一覧ページのURLを生成
 */
export const getCategoryFilterUrl = (category: Category) => {
  return `/products?category=${encodeURIComponent(category.path)}`;
};

/**
 * CategoryInfoオブジェクトから商品一覧ページのURLを生成
 */
export const getCategoryInfoFilterUrl = (categoryInfo: CategoryInfo) => {
  return `/products?category=${encodeURIComponent(categoryInfo.path)}`;
};

/**
 * メインカテゴリに属するすべてのサブカテゴリのIDを取得する
 */
export const getSubCategoryIds = (mainCategory: Category, subCategories: Category[]): string[] => {
  return subCategories
    .filter(subCat => subCat.path.startsWith(mainCategory.path + '/'))
    .map(subCat => subCat.id);
};

/**
 * カテゴリフィルターオプションを生成する
 * @param mainCategoryId - 選択されたメインカテゴリID
 * @param subCategoryId - 選択されたサブカテゴリID（空文字の場合は「すべてのサブカテゴリ」）
 * @param mainCategories - すべてのメインカテゴリ
 * @param subCategories - すべてのサブカテゴリ
 * @returns フィルター用のオプション（categoryId または categoryIds）
 */
export const getCategoryFilterOptions = (
  mainCategoryId: string,
  subCategoryId: string,
  mainCategories: Category[],
  subCategories: Category[]
): { categoryId?: string; categoryIds?: string[] } => {
  const selectedMainCategory = mainCategories.find(cat => cat.id === mainCategoryId);
  
  if (!selectedMainCategory) {
    return {};
  }

  // サブカテゴリが指定されている場合
  if (subCategoryId) {
    return { categoryId: subCategoryId };
  }

  // 「すべてのサブカテゴリ」が選択された場合
  const subCategoryIds = getSubCategoryIds(selectedMainCategory, subCategories);
  
  if (subCategoryIds.length > 0) {
    // サブカテゴリが存在する場合は、すべてのサブカテゴリIDを設定
    return { categoryIds: subCategoryIds };
  } else {
    // サブカテゴリが存在しない場合は、メインカテゴリIDを設定
    return { categoryId: mainCategoryId };
  }
};
