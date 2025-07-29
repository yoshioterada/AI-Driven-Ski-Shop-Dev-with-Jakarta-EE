# Product Catalog Service

スキー用品ショップの商品カタログ管理サービス（Event-Driven Architecture対応）

## 概要

Product Catalog Serviceは、Event-Driven Architectureの中核として機能し、商品データの単一情報源（Single Source of Truth）を提供します。商品の作成、更新、削除時にKafkaイベントを発行し、他のマイクロサービスとリアルタイムで連携します。

### 主要機能

- **商品管理**: 商品情報の登録、更新、削除
- **Event Publishing**: 商品変更イベントのKafka発行
- **カテゴリ・ブランド管理**: 階層的な分類管理
- **商品検索**: 基本的な検索・フィルタリング機能
- **RESTful API**: 標準的なCRUD操作

### Event-Driven Architecture

商品データの変更時に以下のイベントを自動発行：

- `PRODUCT_CREATED`: 商品作成時
- `PRODUCT_UPDATED`: 商品更新時  
- `PRODUCT_DELETED`: 商品削除時
- `PRODUCT_ACTIVATED`: 商品アクティベート時
- `PRODUCT_DEACTIVATED`: 商品ディアクティベート時

## 技術仕様

- **Java**: 17 LTS
- **Framework**: Quarkus 3.8.1
- **Database**: PostgreSQL 16
- **Message Queue**: Apache Kafka 3.7
- **Build Tool**: Maven 3.9+
- **Container**: Docker

## 開発環境セットアップ

### 前提条件

- Java 17 LTS
- Docker & Docker Compose
- Maven 3.9+

### 起動方法

#### 1. Docker Composeでの起動

```bash
# プロジェクトルートで実行
docker-compose up -d

# ログの確認
docker-compose logs -f product-catalog-service
```

#### 2. ローカル開発モード

```bash
# PostgreSQL・Kafkaのみ起動
docker-compose up -d postgres kafka

# Quarkus開発モードで起動
./mvnw quarkus:dev
```

### アクセス情報

- **API**: http://localhost:8083
- **OpenAPI UI**: http://localhost:8083/q/swagger-ui/
- **Health Check**: http://localhost:8083/q/health
- **Metrics**: http://localhost:8083/q/metrics

## API エンドポイント

### 商品管理

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/products` | 商品一覧・検索 |
| GET | `/api/products/{id}` | 商品詳細取得 |
| GET | `/api/products/sku/{sku}` | SKUによる商品取得 |
| POST | `/api/products` | 商品登録 |
| PUT | `/api/products/{id}` | 商品更新 |
| DELETE | `/api/products/{id}` | 商品削除 |
| PUT | `/api/products/{id}/activate` | 商品アクティベート |
| PUT | `/api/products/{id}/deactivate` | 商品ディアクティベート |

### カテゴリ管理

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/categories` | カテゴリ一覧取得 |
| GET | `/api/categories/{id}` | カテゴリ詳細取得 |
| POST | `/api/categories` | カテゴリ登録 |
| PUT | `/api/categories/{id}` | カテゴリ更新 |

### ブランド管理

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/brands` | ブランド一覧取得 |
| GET | `/api/brands/{id}` | ブランド詳細取得 |
| POST | `/api/brands` | ブランド登録 |
| PUT | `/api/brands/{id}` | ブランド更新 |

## イベント発行

### Kafka設定

```yaml
mp:
  messaging:
    outgoing:
      product-events-out:
        connector: smallrye-kafka
        topic: product-events
        value:
          serializer: io.quarkus.kafka.client.serialization.JsonbSerializer
```

### イベント例

```json
{
  "eventType": "PRODUCT_CREATED",
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "productId": "987fcdeb-51a2-43d8-9876-543210fedcba",
  "sku": "SKI-ROSS-HERO-165",
  "name": "Rossignol Hero Athlete FIS GS",
  "category": "SKI_BOARD",
  "brand": "Rossignol",
  "equipmentType": "SKI_BOARD",
  "basePrice": 120000.00,
  "isRentalAvailable": true,
  "eventTime": "2024-01-15T10:30:00Z"
}
```

## テスト実行

```bash
# 単体テスト
mvn test

# 統合テスト
mvn verify

# テストカバレッジレポート生成
mvn jacoco:report
```

## ビルド・デプロイ

### JARファイル生成

```bash
mvn clean package
```

### Docker イメージビルド

```bash
mvn clean package -Dquarkus.container-image.build=true
```

### Native イメージビルド

```bash
mvn clean package -Pnative
```

## 設定

### 環境変数

| 変数名 | 説明 | デフォルト値 |
|--------|------|-------------|
| `QUARKUS_DATASOURCE_JDBC_URL` | PostgreSQL接続URL | `jdbc:postgresql://localhost:5433/product_catalog` |
| `QUARKUS_DATASOURCE_USERNAME` | DB ユーザー名 | `product_user` |
| `QUARKUS_DATASOURCE_PASSWORD` | DB パスワード | `product_pass` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafkaサーバー | `localhost:9092` |
| `QUARKUS_HTTP_PORT` | HTTP ポート | `8083` |

### Kafka Topic

- **Topic名**: `product-events`
- **パーティション数**: 3
- **レプリケーション**: 1（開発環境）
- **保持期間**: 7日

## API使用例

### 商品作成

```bash
curl -X POST http://localhost:8083/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SKI-TEST-001",
    "name": "テストスキー",
    "category": "SKI_BOARD",
    "brand": "Rossignol",
    "equipmentType": "SKI_BOARD",
    "basePrice": 50000,
    "sizeRange": "165cm",
    "difficultyLevel": "INTERMEDIATE",
    "description": "テスト用スキー"
  }'
```

### 商品検索

```bash
# 全商品取得
curl http://localhost:8083/api/products

# カテゴリ別検索
curl "http://localhost:8083/api/products?category=SKI_BOARD"

# ブランド別検索
curl "http://localhost:8083/api/products?brand=Rossignol"

# 価格範囲検索
curl "http://localhost:8083/api/products?minPrice=30000&maxPrice=70000"
```

## 監視・ヘルスチェック

### ヘルスチェック

```bash
curl http://localhost:8083/q/health
```

### メトリクス

```bash
curl http://localhost:8083/q/metrics
```

### ログレベル設定

```properties
# application.properties
quarkus.log.category."com.ski.shop.catalog".level=DEBUG
```

## トラブルシューティング

### よくある問題

1. **Kafka接続エラー**
   ```bash
   # Kafkaコンテナの確認
   docker-compose ps kafka
   
   # Kafkaログの確認
   docker-compose logs kafka
   ```

2. **データベース接続エラー**
   ```bash
   # PostgreSQLコンテナの確認
   docker-compose ps postgres
   
   # データベース接続テスト
   psql -h localhost -p 5433 -U product_user -d product_catalog
   ```

3. **イベント発行失敗**
   ```bash
   # アプリケーションログの確認
   docker-compose logs product-catalog-service | grep -i kafka
   ```

## 開発者向け情報

### コード構成

```text
src/main/java/com/ski/shop/catalog/
├── domain/          # エンティティ・ドメインモデル
├── service/         # ビジネスロジック・イベント発行
├── resource/        # REST エンドポイント
├── repository/      # データアクセス
├── event/           # イベントクラス
└── exception/       # 例外クラス
```

### 主要クラス

- `Product`: 商品エンティティ
- `ProductService`: 商品ビジネスロジック
- `ProductEventPublisher`: Kafkaイベント発行
- `ProductResource`: REST API
- `ProductRepository`: データアクセス

### イベント設計

全てのイベントは `ProductEvent` インターフェースを実装し、共通フィールドを持ちます：

- `eventId`: ユニークなイベントID
- `productId`: 対象商品のID
- `eventTime`: イベント発生時刻
- `eventType`: イベントタイプ文字列

## ライセンス

MIT License
