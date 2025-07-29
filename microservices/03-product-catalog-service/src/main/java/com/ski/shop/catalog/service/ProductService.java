package com.ski.shop.catalog.service;

import com.ski.shop.catalog.domain.*;
import com.ski.shop.catalog.dto.*;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CacheKey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品サービス
 */
@ApplicationScoped
public class ProductService {

    @Inject
    ProductEventPublisher eventPublisher;

    /**
     * 商品一覧を検索条件に基づいて取得
     */
    @Transactional
    @CacheResult(cacheName = "products")
    public List<ProductSummaryResponse> searchProducts(
            @CacheKey String keyword,
            @CacheKey UUID categoryId,
            @CacheKey List<UUID> categoryIds,
            @CacheKey boolean includeSubcategories,
            @CacheKey UUID brandId,
            @CacheKey SkiType skiType,
            @CacheKey DifficultyLevel difficultyLevel,
            @CacheKey Double minPrice,
            @CacheKey Double maxPrice,
            @CacheKey String sortBy,
            @CacheKey int page,
            @CacheKey int size) {

        StringBuilder query = new StringBuilder("p.publishStatus = ?1 AND p.isActive = ?2");
        List<Object> params = new ArrayList<>();
        params.add(PublishStatus.PUBLISHED);
        params.add(true);

        // キーワード検索（商品名、説明、ブランド名、カテゴリ名を対象）
        if (keyword != null && !keyword.trim().isEmpty()) {
            // より効率的な検索クエリに変更
            query.append(" AND (LOWER(p.name) LIKE ?").append(params.size() + 1)
                 .append(" OR LOWER(p.brand.name) LIKE ?").append(params.size() + 2).append(")");
            String searchTerm = "%" + keyword.toLowerCase() + "%";
            params.add(searchTerm);
            params.add(searchTerm);
        }

        // カテゴリフィルター - 複数カテゴリIDまたは単一カテゴリIDをサポート
        if (categoryIds != null && !categoryIds.isEmpty()) {
            if (includeSubcategories) {
                // サブカテゴリも含める場合は、メインカテゴリとそのサブカテゴリを検索
                query.append(" AND (p.category.id IN (?").append(params.size() + 1).append(")");
                query.append(" OR p.category.parent.id IN (?").append(params.size() + 2).append("))");
                params.add(categoryIds);
                params.add(categoryIds);
            } else {
                // 指定されたカテゴリのみ
                query.append(" AND p.category.id IN (?").append(params.size() + 1).append(")");
                params.add(categoryIds);
            }
        } else if (categoryId != null) {
            if (includeSubcategories) {
                // 単一カテゴリでサブカテゴリも含める場合
                query.append(" AND (p.category.id = ?").append(params.size() + 1);
                query.append(" OR p.category.parent.id = ?").append(params.size() + 2).append(")");
                params.add(categoryId);
                params.add(categoryId);
            } else {
                // 単一カテゴリのみ
                query.append(" AND p.category.id = ?").append(params.size() + 1);
                params.add(categoryId);
            }
        }

        // ブランドフィルター
        if (brandId != null) {
            query.append(" AND p.brand.id = ?").append(params.size() + 1);
            params.add(brandId);
        }

        // スキータイプフィルター
        if (skiType != null) {
            query.append(" AND p.skiType = ?").append(params.size() + 1);
            params.add(skiType);
        }

        // 難易度フィルター
        if (difficultyLevel != null) {
            query.append(" AND p.difficultyLevel = ?").append(params.size() + 1);
            params.add(difficultyLevel);
        }

        // 価格フィルター（以上）
        if (minPrice != null && minPrice > 0) {
            query.append(" AND p.basePrice >= ?").append(params.size() + 1);
            params.add(minPrice);
        }

        // 価格フィルター（以下）
        if (maxPrice != null && maxPrice > 0) {
            query.append(" AND p.basePrice <= ?").append(params.size() + 1);
            params.add(maxPrice);
        }

        // ソート
        String orderBy = getOrderByClause(sortBy);
        if (orderBy != null) {
            query.append(" ORDER BY ").append(orderBy);
        } else {
            query.append(" ORDER BY p.createdAt DESC");
        }

        // 完全なクエリを構築してページネーション付きで商品を取得
        String jpqlQuery = "SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.brand " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.tags " +
            "LEFT JOIN FETCH p.additionalSpecs " +
            "WHERE " + query.toString();

        List<Product> products = Product.find(jpqlQuery, params.toArray())
                .page(page, size)
                .list();

        return products.stream()
                .map(this::toProductSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * 商品一覧を検索条件に基づいて取得（後方互換性のため）
     */
    @Transactional
    @CacheResult(cacheName = "products")
    public List<ProductSummaryResponse> searchProducts(
            @CacheKey String keyword,
            @CacheKey UUID categoryId,
            @CacheKey UUID brandId,
            @CacheKey SkiType skiType,
            @CacheKey DifficultyLevel difficultyLevel,
            @CacheKey Double minPrice,
            @CacheKey Double maxPrice,
            @CacheKey String sortBy,
            @CacheKey int page,
            @CacheKey int size) {

        return searchProducts(keyword, categoryId, null, false, brandId, skiType, difficultyLevel, minPrice, maxPrice, sortBy, page, size);
    }

    /**
     * 商品詳細を取得
     */
    @CacheResult(cacheName = "products")
    public ProductResponse getProduct(@CacheKey UUID productId) {
        // まず基本的な商品情報を取得（imagesとvariantsは除く）
        Product product = Product.find("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand LEFT JOIN FETCH p.tags LEFT JOIN FETCH p.additionalSpecs WHERE p.id = ?1", productId)
                .firstResult();
        if (product == null) {
            throw new NotFoundException("Product not found: " + productId);
        }

        // ビュー数を増加
        product.incrementViewCount();
        product.persist();

        return toProductResponse(product);
    }

    /**
     * SKUで商品を取得
     */
    @CacheResult(cacheName = "products")
    public ProductResponse getProductBySku(@CacheKey String sku) {
        Optional<Product> productOpt = Product.findBySku(sku);
        if (productOpt.isEmpty()) {
            throw new NotFoundException("Product not found: " + sku);
        }

        Product product = productOpt.get();
        product.incrementViewCount();
        product.persist();

        return toProductResponse(product);
    }

    /**
     * 注目商品一覧を取得
     */
    @CacheResult(cacheName = "products")
    public List<ProductSummaryResponse> getFeaturedProducts() {
        List<Product> products = Product.findFeatured();
        return products.stream()
                .map(this::toProductSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * カテゴリ別商品一覧を取得
     */
    @CacheResult(cacheName = "products")
    public List<ProductSummaryResponse> getProductsByCategory(@CacheKey UUID categoryId) {
        List<Product> products = Product.findByCategory(categoryId);
        return products.stream()
                .map(this::toProductSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * ブランド別商品一覧を取得
     */
    @CacheResult(cacheName = "products")
    public List<ProductSummaryResponse> getProductsByBrand(@CacheKey UUID brandId) {
        List<Product> products = Product.findByBrand(brandId);
        return products.stream()
                .map(this::toProductSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * 商品を作成
     */
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        // SKU重複チェック
        if (Product.findBySku(request.getSku()).isPresent()) {
            throw new IllegalArgumentException("SKU already exists: " + request.getSku());
        }

        // カテゴリ存在チェック
        Category category = Category.findById(request.getCategoryId());
        if (category == null) {
            throw new NotFoundException("Category not found: " + request.getCategoryId());
        }

        // ブランド存在チェック
        Brand brand = Brand.findById(request.getBrandId());
        if (brand == null) {
            throw new NotFoundException("Brand not found: " + request.getBrandId());
        }

        Product product = new Product();
        product.sku = request.getSku();
        product.name = request.getName();
        product.description = request.getDescription();
        product.shortDescription = request.getShortDescription();
        product.category = category;
        product.brand = brand;
        product.material = request.getSpecification().material();
        product.skiType = request.getSpecification().skiType();
        product.difficultyLevel = request.getSpecification().difficultyLevel();
        product.length = request.getSpecification().length();
        product.width = request.getSpecification().width();
        product.weight = request.getSpecification().weight();
        product.radius = request.getSpecification().radius();
        product.flex = request.getSpecification().flex();
        product.basePrice = request.getBasePrice();
        product.salePrice = request.getSalePrice();
        product.costPrice = request.getCostPrice();

        if (request.getStatus() != null) {
            product.publishStatus = request.getStatus().publishStatus();
            product.isActive = request.getStatus().isActive();
            product.isFeatured = request.getStatus().isFeatured();
            product.isDiscontinued = request.getStatus().isDiscontinued();
        }

        if (request.getTags() != null) {
            product.tags = new HashSet<>(request.getTags());
        }

        if (request.getAdditionalSpecs() != null) {
            product.additionalSpecs = new HashMap<>(request.getAdditionalSpecs());
        }

        product.persist();
        
        // イベント発行
        eventPublisher.publishProductCreated(product);
        
        return toProductResponse(product);
    }

    /**
     * 商品を更新
     */
    @Transactional
    public ProductResponse updateProduct(UUID productId, ProductCreateRequest request) {
        Product product = Product.findById(productId);
        if (product == null) {
            throw new NotFoundException("Product not found: " + productId);
        }

        // SKU重複チェック（自分以外）
        Optional<Product> existingProduct = Product.findBySku(request.getSku());
        if (existingProduct.isPresent() && !existingProduct.get().id.equals(productId)) {
            throw new IllegalArgumentException("SKU already exists: " + request.getSku());
        }

        // 更新前の状態を保存（イベント発行のため）
        Product oldProduct = cloneProduct(product);

        product.sku = request.getSku();
        product.name = request.getName();
        product.description = request.getDescription();
        product.shortDescription = request.getShortDescription();
        product.basePrice = request.getBasePrice();
        product.salePrice = request.getSalePrice();
        product.costPrice = request.getCostPrice();

        if (request.getTags() != null) {
            product.tags = new HashSet<>(request.getTags());
        }

        if (request.getAdditionalSpecs() != null) {
            product.additionalSpecs = new HashMap<>(request.getAdditionalSpecs());
        }

        product.persist();
        
        // 変更がある場合のみイベント発行
        if (hasChanges(oldProduct, product)) {
            eventPublisher.publishProductUpdated(oldProduct, product);
        }
        
        return toProductResponse(product);
    }

    /**
     * 商品を削除
     */
    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = Product.findById(productId);
        if (product == null) {
            throw new NotFoundException("Product not found: " + productId);
        }

        String sku = product.sku;
        product.delete();
        
        // イベント発行
        eventPublisher.publishProductDeleted(productId, sku);
    }

    // プライベートメソッド

    private String getOrderByClause(String sortBy) {
        switch (sortBy != null ? sortBy : "created_desc") {
            case "name_asc": return "p.name ASC";
            case "name_desc": return "p.name DESC";
            case "price_asc": return "p.basePrice ASC";
            case "price_desc": return "p.basePrice DESC";
            case "created_asc": return "p.createdAt ASC";
            case "created_desc": return "p.createdAt DESC";
            case "popularity": return "p.salesCount DESC";
            default: return "p.createdAt DESC";
        }
    }

    private ProductSummaryResponse toProductSummaryResponse(Product product) {
        CategorySummaryResponse categoryResponse = null;
        if (product.category != null) {
            categoryResponse = new CategorySummaryResponse(
                    product.category.id, product.category.name, product.category.path);
        }

        BrandSummaryResponse brandResponse = null;
        if (product.brand != null) {
            brandResponse = new BrandSummaryResponse(
                    product.brand.id, product.brand.name, product.brand.logoUrl, product.brand.country);
        }

        String primaryImageUrl = null;
        try {
            primaryImageUrl = product.getPrimaryImage()
                    .map(img -> img.imageUrl)
                    .orElse(null);
        } catch (Exception e) {
            // 画像データが取得できない場合はnullのまま処理を続行
            primaryImageUrl = null;
        }

        return new ProductSummaryResponse(
                product.id,
                product.sku,
                product.name,
                product.shortDescription,
                categoryResponse,
                brandResponse,
                product.getCurrentPrice(),
                product.basePrice,
                product.isOnSale(),
                product.getDiscountPercentage(),
                primaryImageUrl,
                true, // TODO: 在庫サービスと連携
                product.isFeatured,
                null, // TODO: レビューサービスと連携
                0,    // TODO: レビューサービスと連携
                product.tags,
                product.createdAt
        );
    }

    private ProductResponse toProductResponse(Product product) {
        CategorySummaryResponse categoryResponse = null;
        if (product.category != null) {
            categoryResponse = new CategorySummaryResponse(
                    product.category.id, product.category.name, product.category.path);
        }

        BrandSummaryResponse brandResponse = null;
        if (product.brand != null) {
            brandResponse = new BrandSummaryResponse(
                    product.brand.id, product.brand.name, product.brand.logoUrl, product.brand.country);
        }

        return new ProductResponse(
                product.id,
                product.sku,
                product.name,
                product.description,
                product.shortDescription,
                categoryResponse,
                brandResponse,
                null, // TODO: ProductSpecificationResponse
                product.basePrice,
                product.salePrice,
                product.getCurrentPrice(),
                product.isOnSale(),
                product.getDiscountPercentage(),
                null, // TODO: ProductStatusResponse
                product.tags,
                Collections.emptyList(), // TODO: ProductImageResponse
                Collections.emptyList(), // TODO: ProductVariantResponse
                product.salesCount,
                product.viewCount,
                product.createdAt,
                product.updatedAt
        );
    }
    
    /**
     * 商品を複製（更新前の状態保存用）
     */
    private Product cloneProduct(Product original) {
        Product clone = new Product();
        clone.id = original.id;
        clone.sku = original.sku;
        clone.name = original.name;
        clone.description = original.description;
        clone.shortDescription = original.shortDescription;
        clone.category = original.category;
        clone.brand = original.brand;
        clone.material = original.material;
        clone.skiType = original.skiType;
        clone.difficultyLevel = original.difficultyLevel;
        clone.length = original.length;
        clone.width = original.width;
        clone.weight = original.weight;
        clone.radius = original.radius;
        clone.flex = original.flex;
        clone.publishStatus = original.publishStatus;
        clone.isActive = original.isActive;
        clone.isFeatured = original.isFeatured;
        clone.isDiscontinued = original.isDiscontinued;
        clone.publishedAt = original.publishedAt;
        clone.discontinuedAt = original.discontinuedAt;
        clone.basePrice = original.basePrice;
        clone.salePrice = original.salePrice;
        clone.costPrice = original.costPrice;
        clone.tags = new HashSet<>(original.tags);
        clone.additionalSpecs = new HashMap<>(original.additionalSpecs);
        clone.salesCount = original.salesCount;
        clone.viewCount = original.viewCount;
        clone.createdAt = original.createdAt;
        clone.updatedAt = original.updatedAt;
        return clone;
    }
    
    /**
     * 商品に変更があったかチェック
     */
    private boolean hasChanges(Product oldProduct, Product newProduct) {
        return !oldProduct.name.equals(newProduct.name) ||
               !oldProduct.description.equals(newProduct.description) ||
               oldProduct.basePrice.compareTo(newProduct.basePrice) != 0 ||
               oldProduct.isActive != newProduct.isActive ||
               !oldProduct.category.id.equals(newProduct.category.id) ||
               !oldProduct.brand.id.equals(newProduct.brand.id);
    }
}
