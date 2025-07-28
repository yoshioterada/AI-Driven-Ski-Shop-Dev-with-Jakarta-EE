# Event-Driven Architecture Implementation

このドキュメントでは、Product CatalogサービスとInventory Managementサービス間のデータ重複問題を解決するために実装されたEvent-Driven Architectureについて説明します。

## 概要

### 解決された問題

**以前の問題:**
```sql
-- Product Catalog Service
products (id, sku, name, category, brand, price, specifications...)

-- Inventory Management Service (重複データ)
equipment (product_id, sku, name, category, brand, daily_rate, description...)
```

**解決策:**
- Product Catalog Service が商品マスタの単一信頼できる情報源（Single Source of Truth）
- Inventory Management Service は在庫管理に専念し、必要な商品情報をキャッシュ
- Kafkaを使用したイベント駆動アーキテクチャで同期

## アーキテクチャ図

```text
┌─────────────────────┐     Events     ┌─────────────────────┐
│ Product Catalog     │─────────────→│ Apache Kafka        │
│ Service             │               │ (product-events)    │
│ (Source of Truth)   │               │                     │
└─────────────────────┘               └─────────────────────┘
                                                │
                                                │ Subscribe
                                                ▼
                                    ┌─────────────────────┐
                                    │ Inventory Mgmt      │
                                    │ Service             │
                                    │ (Event Consumer)    │
                                    └─────────────────────┘
```

## 実装の詳細

### 1. イベントタイプ

以下のイベントが実装されています：

- `PRODUCT_CREATED`: 新しい商品が作成された時
- `PRODUCT_UPDATED`: 商品情報が更新された時  
- `PRODUCT_DELETED`: 商品が削除された時
- `PRODUCT_ACTIVATED`: 商品が有効化された時
- `PRODUCT_DEACTIVATED`: 商品が無効化された時

### 2. データ分離

| サービス | 責任 | 保持データ |
|---------|------|-----------|
| **Product Catalog** | 商品マスタ管理 | 商品情報（名前、SKU、価格）<br>カテゴリ・ブランド情報<br>商品仕様・画像 |
| **Inventory Management** | 在庫管理専門 | 在庫数・場所・状態<br>レンタル料金（ビジネスロジック）<br>キャッシュされた商品情報 |

## セットアップ手順

### 1. Kafkaの起動

```bash
# Kafkaを起動（docker-composeを使用）
cd microservices
docker-compose up -d kafka zookeeper
```

### 2. データベース初期化

```bash
# Product Catalog Service
cd 03-product-catalog-service
mvn quarkus:dev

# Inventory Management Service  
cd 05-inventory-management-service
mvn quarkus:dev
```

### 3. データ移行（既存データがある場合）

```bash
# 既存データをキャッシュフィールドに移行
curl -X POST http://localhost:8084/admin/migrate

# 移行状況確認
curl http://localhost:8084/admin/migration-status
```

## 使用方法

### 1. 商品作成（Product Catalog Service）

```bash
curl -X POST http://localhost:8083/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SKI-001",
    "name": "Advanced Ski",
    "categoryId": "category-uuid",
    "brandId": "brand-uuid",
    "basePrice": 50000,
    "specification": {
      "skiType": "SKI_BOARD",
      "difficultyLevel": "INTERMEDIATE"
    }
  }'
```

→ 自動的に `PRODUCT_CREATED` イベントが発行され、Inventory Serviceに設備レコードが作成されます。

### 2. 設備確認（Inventory Management Service）

```bash
# 設備一覧確認
curl http://localhost:8084/admin/equipment

# 特定商品の設備確認
curl http://localhost:8084/api/equipment/by-product/{productId}
```

### 3. データ同期ヘルスチェック

```bash
# 同期状況確認
curl http://localhost:8084/admin/sync-health

# Quarkusヘルスチェック
curl http://localhost:8084/q/health
```

## 監視・運用

### ヘルスチェックエンドポイント

| エンドポイント | 説明 |
|-------------|------|
| `/admin/sync-health` | データ同期の健全性チェック |
| `/admin/migration-status` | 移行進捗状況 |
| `/q/health` | 全体的なサービス健全性 |

### メトリクス

以下のメトリクスが利用可能です：

- 処理されたイベント数
- イベント処理時間
- データ同期ラグ
- キャッシュヒット率

```bash
# Prometheusメトリクス確認
curl http://localhost:8083/q/metrics
curl http://localhost:8084/q/metrics
```

## レンタル料金計算ロジック

設備のレンタル料金は商品タイプに応じて自動計算されます：

| 設備タイプ | 計算式 | 例（基本価格50,000円） |
|----------|-------|---------------------|
| SKI_BOARD | 基本価格 × 10% × 1.2 | 6,000円/日 |
| BOOT | 基本価格 × 10% × 1.1 | 5,500円/日 |
| HELMET | 基本価格 × 10% × 0.8 | 4,000円/日 |
| POLE | 基本価格 × 10% × 0.6 | 3,000円/日 |
| GOGGLE | 基本価格 × 10% × 0.5 | 2,500円/日 |
| GLOVE | 基本価格 × 10% × 0.4 | 2,000円/日 |
| その他 | 基本価格 × 10% | 5,000円/日 |

## トラブルシューティング

### よくある問題と解決方法

1. **イベントが処理されない**
   ```bash
   # Kafkaの接続確認
   docker-compose logs kafka
   
   # デッドレターキューの確認
   kafka-console-consumer --bootstrap-server localhost:9092 --topic product-events-dlq
   ```

2. **データ同期の不整合**
   ```bash
   # キャッシュデータの修復
   curl -X POST http://localhost:8084/admin/repair
   ```

3. **パフォーマンス問題**
   ```bash
   # メトリクス確認
   curl http://localhost:8084/q/metrics | grep product_event
   ```

### ログレベル調整

```properties
# application.properties
quarkus.log.category."com.ski.shop.catalog.event".level=DEBUG
quarkus.log.category."com.skiresort.inventory.service".level=DEBUG
```

## テスト

### 単体テスト実行

```bash
# Product Catalog Service
cd 03-product-catalog-service
mvn test

# Inventory Management Service
cd 05-inventory-management-service  
mvn test
```

### 統合テスト

```bash
# 統合テストの実行
mvn test -Dtest=EventDrivenIntegrationTest
```

## 今後の拡張計画

1. **API Gateway統合**: 商品+在庫情報の統合エンドポイント
2. **リアルタイム更新**: WebSocketによる在庫状況のリアルタイム通知
3. **分析サービス**: 商品イベントの分析とレポート機能
4. **キャッシュサービス**: Redisを使用した分散キャッシュ

## 参考資料

- [Event-Driven Architecture設計書](../TODO-Event-Driven-Design.md)
- [Quarkus Reactive Messaging](https://quarkus.io/guides/kafka)
- [MicroProfile Reactive Messaging](https://microprofile.io/project/eclipse/microprofile-reactive-messaging)