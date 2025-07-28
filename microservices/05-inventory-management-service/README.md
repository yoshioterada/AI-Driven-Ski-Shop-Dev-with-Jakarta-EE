# Inventory Management Service

Event-Driven Architecture対応のレンタル設備在庫管理サービス

## 概要

Inventory Management Serviceは、Product Catalog ServiceからのイベントをリアルタイムでConsumeし、レンタル用設備情報をキャッシュ管理するEvent Consumer として機能します。Product Catalogが商品データの単一情報源として機能し、本サービスはレンタル業務に特化した機能を提供します。

### 主要機能

- **Event消費**: Product Catalog Serviceからの商品イベント処理
- **Equipment管理**: レンタル用設備情報のキャッシュ管理
- **レンタル料金計算**: 商品タイプ別の動的料金算出
- **在庫管理**: レンタル可能設備の在庫追跡
- **予約管理**: レンタル予約の管理と状態制御
- **データ移行**: 段階的な旧システムからの移行サポート

### Event-Driven Architecture

Product Catalog Serviceから以下のイベントを自動消費：

- `PRODUCT_CREATED`: レンタル対象商品のEquipment作成
- `PRODUCT_UPDATED`: キャッシュされた商品情報の更新
- `PRODUCT_DELETED`: Equipmentの非アクティブ化
- `PRODUCT_ACTIVATED/DEACTIVATED`: レンタル可用性の更新

### レンタル料金計算

設備タイプ別の動的料金計算：

- **基本料金**: Product Catalog の basePrice * 10%
- **設備タイプ別係数**:
  - SKI_BOARD (スキー板): 1.2倍
  - BOOT (ブーツ): 1.1倍
  - HELMET (ヘルメット): 0.8倍
  - POLE (ストック): 0.6倍
  - GOGGLE (ゴーグル): 0.5倍
  - GLOVE (グローブ): 0.4倍

## 技術仕様

- **Java**: 21 LTS
- **Framework**: Jakarta EE 11
- **Application Server**: WildFly 31.0.1
- **Database**: PostgreSQL 16
- **Message Queue**: Apache Kafka 3.7
- **Build Tool**: Maven

## アーキテクチャ

```text
Product Catalog Service ─[Events]→ Apache Kafka ─[Consume]→ Inventory Service
                                         │
                                    product-events
                                    Topic
```

### データ設計

Equipment テーブルは段階的移行をサポート：

- **旧フィールド**: sku, name, category, brand, equipment_type
- **キャッシュフィールド**: cached_sku, cached_name, cached_category, cached_brand, cached_equipment_type
- **ビジネスフィールド**: daily_rate, is_rental_available

## 開発環境セットアップ

### 前提条件

- Java 21 LTS
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16+
- Apache Kafka 3.7+
- WildFly 31.0.1

### 起動方法

#### 1. Docker Composeでの起動

```bash
# PostgreSQL・Kafka起動
docker-compose up -d postgres kafka

# アプリケーション起動
./mvnw compile
./mvnw exec:java
```

#### 2. WildFlyでの起動

```bash
# ビルド
mvn clean package

# WildFlyにデプロイ
cp target/inventory-management-service.war $WILDFLY_HOME/standalone/deployments/
```

### アクセス情報

- **管理API**: http://localhost:8084/admin
- **予約API**: http://localhost:8084/reservations
- **ヘルスチェック**: http://localhost:8084/health

## API エンドポイント

### 設備管理（管理者用）

| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/equipment` | 設備一覧取得 |
| GET | `/admin/equipment/{id}` | 設備詳細取得 |
| GET | `/admin/equipment/sku/{sku}` | SKUによる設備取得 |
| PUT | `/admin/equipment/{id}/rate` | レンタル料金更新 |
| PUT | `/admin/equipment/{id}/activate` | 設備アクティベート |
| PUT | `/admin/equipment/{id}/deactivate` | 設備ディアクティベート |

### 在庫管理

| Method | Path | Description |
|--------|------|-------------|
| GET | `/inventory/items` | 在庫アイテム一覧 |
| GET | `/inventory/items/{id}` | 在庫アイテム詳細 |
| POST | `/inventory/items` | 在庫アイテム作成 |
| PUT | `/inventory/items/{id}/status` | 在庫状態更新 |

### レンタル予約

| Method | Path | Description |
|--------|------|-------------|
| GET | `/reservations` | 予約一覧取得 |
| GET | `/reservations/{id}` | 予約詳細取得 |
| POST | `/reservations` | 予約作成 |
| PUT | `/reservations/{id}/confirm` | 予約確定 |
| DELETE | `/reservations/{id}` | 予約キャンセル |

### データ移行（管理者用）

| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/migration/status` | 移行状況確認 |
| POST | `/admin/migration/legacy-to-cache` | 旧データのキャッシュ移行 |

## Kafka設定

### Consumer設定

```properties
# application.properties
mp.messaging.incoming.product-events.connector=smallrye-kafka
mp.messaging.incoming.product-events.topic=product-events
mp.messaging.incoming.product-events.group.id=inventory-management-service
mp.messaging.incoming.product-events.auto.offset.reset=earliest
```

### イベント消費例

```java
@Incoming("product-events")
public CompletionStage<Void> handleProductEvent(Message<ProductEvent> message) {
    ProductEvent event = message.getPayload();
    
    switch (event.getEventType()) {
        case "PRODUCT_CREATED":
            if (isRentalEligible(event)) {
                equipmentService.createEquipmentFromProduct(event);
            }
            break;
        case "PRODUCT_UPDATED":
            equipmentService.updateEquipmentFromProduct(event);
            break;
        // ... 他のイベント処理
    }
    
    return message.ack();
}
```

## データベース設定

### PostgreSQL設定

```bash
# データベース作成
createdb inventory_db

# 初期テーブル作成
psql -d inventory_db -f setup_inventory_db.sh
```

### テーブル構成

- **equipment**: 設備マスタ（商品カタログとの連携＋レンタル固有情報）
- **inventory_items**: 在庫アイテム（個別の設備）
- **reservations**: レンタル予約
- **inventory_movements**: 在庫移動履歴
- **locations**: 店舗・倉庫マスタ

## API使用例

### 設備一覧取得

```bash
curl http://localhost:8084/admin/equipment
```

レスポンス例：

```json
[
  {
    "id": 1,
    "productId": "p1234567-89ab-cdef-0123-456789abcdef",
    "cachedSku": "SKI-ROSS-HERO-165",
    "cachedName": "Rossignol Hero Athlete FIS GS",
    "cachedEquipmentType": "SKI_BOARD",
    "dailyRate": 14400.00,
    "isRentalAvailable": true,
    "isActive": true
  }
]
```

### 予約作成

```bash
curl -X POST http://localhost:8084/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 123,
    "equipmentId": 1,
    "startDate": "2024-01-15T09:00:00Z",
    "endDate": "2024-01-17T17:00:00Z",
    "sizeRequested": "165cm"
  }'
```

### 移行状況確認

```bash
curl http://localhost:8084/admin/migration/status
```

レスポンス例：

```json
{
  "totalEquipment": 30,
  "cachedEquipment": 25,
  "legacyEquipment": 5,
  "migrationPercentage": 83.33,
  "migrationCompleted": false
}
```

## 監視・運用

### ヘルスチェック

```bash
curl http://localhost:8084/health
```

### イベント処理監視

- Kafka Consumer Group状態確認
- イベント処理成功・失敗率
- レンタル料金計算処理時間

### データ整合性チェック

```bash
# キャッシュデータと旧データの整合性確認
curl http://localhost:8084/admin/migration/consistency-check
```

## トラブルシューティング

### よくある問題

1. **Kafka接続エラー**
   ```bash
   # Kafka Consumer Group状態確認
   kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group inventory-management-service
   ```

2. **イベント処理失敗**
   ```bash
   # アプリケーションログ確認
   tail -f $WILDFLY_HOME/standalone/log/server.log | grep ProductEventConsumer
   ```

3. **データ不整合**
   ```bash
   # データ整合性チェック実行
   curl http://localhost:8084/admin/migration/consistency-check
   ```

## テスト

### 単体テスト

```bash
mvn test
```

### 統合テスト

```bash
mvn test -Dtest=*IntegrationTest
```

### Event処理テスト

```bash
mvn test -Dtest=ProductEventConsumerTest
```

## 作成済みデータ

### 設備データ（30種類）

商品カタログサービスと連携して、以下の設備が管理されています：

#### スキー板（13種類）
- レーシングGS: Rossignol Hero Athlete、Atomic Redster X9 等
- パウダー: K2 Mindbender 108Ti、Volkl Mantra M6 等
- 初級・中級向け: Salomon S/Max 8、Atomic Vantage 75 C 等

#### その他設備（17種類）
- ストック、スキーブーツ、ヘルメット、ゴーグル、グローブ等

### 料金設定例

- **スキー板**: 基本料金の12%（例：50,000円 → 6,000円/日）
- **ブーツ**: 基本料金の11%（例：30,000円 → 3,300円/日）
- **ヘルメット**: 基本料金の8%（例：15,000円 → 1,200円/日）

## 開発者向け情報

### コード構成

```text
src/main/java/com/skiresort/inventory/
├── model/           # エンティティ
├── service/         # ビジネスロジック・イベント処理
├── resource/        # REST エンドポイント
├── event/           # イベント処理
└── exception/       # 例外クラス
```

### 主要クラス

- `Equipment`: 設備エンティティ（キャッシュ機能付き）
- `ProductEventConsumer`: Kafkaイベント消費
- `EquipmentService`: 設備管理ビジネスロジック
- `DataMigrationService`: データ移行管理
- `AdminResource`: 管理API

## ライセンス

MIT License
