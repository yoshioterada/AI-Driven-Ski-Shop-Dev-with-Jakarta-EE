-- 在庫管理サービス：Event-Driven Architecture対応
-- V1.0.2__Add_product_cache_columns.sql

-- equipmentテーブルにキャッシュカラムを追加
ALTER TABLE equipment 
ADD COLUMN IF NOT EXISTS cached_sku VARCHAR(100),
ADD COLUMN IF NOT EXISTS cached_name VARCHAR(200),
ADD COLUMN IF NOT EXISTS cached_category VARCHAR(100),
ADD COLUMN IF NOT EXISTS cached_brand VARCHAR(100),
ADD COLUMN IF NOT EXISTS cached_equipment_type VARCHAR(50),
ADD COLUMN IF NOT EXISTS cached_size_range VARCHAR(50),
ADD COLUMN IF NOT EXISTS cached_difficulty_level VARCHAR(20),
ADD COLUMN IF NOT EXISTS cached_base_price DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS cached_description TEXT,
ADD COLUMN IF NOT EXISTS cached_image_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS cache_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 既存の重複データをキャッシュカラムにコピー
UPDATE equipment SET 
    cached_sku = sku,
    cached_name = name,
    cached_category = category,
    cached_brand = brand,
    cached_equipment_type = equipment_type,
    cached_size_range = size_range,
    cached_difficulty_level = difficulty_level,
    cached_description = description,
    cached_image_url = image_url,
    cache_updated_at = CURRENT_TIMESTAMP
WHERE cached_sku IS NULL;

-- キャッシュカラムのインデックス作成
CREATE INDEX IF NOT EXISTS idx_equipment_cached_sku ON equipment(cached_sku);
CREATE INDEX IF NOT EXISTS idx_equipment_cached_type ON equipment(cached_equipment_type);
CREATE INDEX IF NOT EXISTS idx_equipment_cache_updated ON equipment(cache_updated_at);

-- 重複カラムの削除は後のフェーズで実行
-- この段階では並行運用のため保持