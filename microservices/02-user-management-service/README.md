# User Management Service

ユーザー管理サービス（User Management Service）は、スキーリゾート管理システムのユーザー情報管理を担当するマイクロサービスです。

## 概要

このサービスは以下のユーザー管理機能を提供します：

- **ユーザープロファイル管理**: 個人情報、設定、スキルレベルの管理
- **ロール・権限管理**: カスタマー、スタッフ、管理者などの権限制御
- **プリファレンス管理**: 個人設定、言語、通知設定
- **アカウントライフサイクル**: 有効化、無効化、削除
- **プロファイル検証**: 身分証明書、スキルレベル認定
- **データプライバシー**: GDPR準拠、データ削除要求

## 技術スタック

- **Quarkus 3**: ネイティブクラウド向け Java ランタイム
- **Java 21 LTS**: プログラミング言語
- **RESTEasy Reactive**: JAX-RS 実装
- **Hibernate ORM / Bean Validation**: データアクセスと検証
- **PostgreSQL**: メインデータベース
- **SmallRye OpenAPI / Health**: API ドキュメンテーションとヘルスチェック

## アーキテクチャ

```text
┌─────────────────────────────────────────────────────────┐
│                User Management Service                   │
├─────────────────────────────────────────────────────────┤
│  REST Layer (JAX-RS)                                   │
│  ├─ UserResource                                        │
│  ├─ ProfileResource                                     │
│  └─ Exception Handlers                                  │
├─────────────────────────────────────────────────────────┤
│  Service Layer                                          │
│  ├─ UserService                                         │
│  ├─ ProfileService                                      │
│  └─ PreferenceService                                   │
├─────────────────────────────────────────────────────────┤
│  Repository Layer                                       │
│  ├─ UserRepository                                      │
│  ├─ UserProfileRepository                               │
│  └─ UserPreferenceRepository                            │
├─────────────────────────────────────────────────────────┤
│  Entity Layer (JPA)                                     │
│  ├─ User                                                │
│  ├─ UserProfile                                         │
│  └─ UserPreference                                      │
└─────────────────────────────────────────────────────────┘
```

## エンティティ設計

### User (ユーザー基本情報)

- ユーザーID、ユーザー名、メールアドレス
- アカウントステータス（アクティブ、一時停止、無効）
- 作成日時、更新日時、最終ログイン
- ロール（CUSTOMER、STAFF、ADMIN）

### UserProfile (ユーザープロファイル)

- 個人情報（氏名、生年月日、性別、電話番号）
- 住所情報（国、都道府県、市区町村、郵便番号）
- スキー情報（レベル、経験年数、好みのゲレンデ）
- 緊急連絡先情報

### UserPreference (ユーザー設定)

- 言語設定、タイムゾーン
- 通知設定（メール、SMS、プッシュ）
- プライバシー設定
- マーケティング同意設定

## API エンドポイント

### ユーザー管理API

| メソッド | エンドポイント | 説明 |
|---------|---------------|------|
| GET | `/users` | ユーザー一覧取得（管理者のみ） |
| GET | `/users/{userId}` | ユーザー詳細取得 |
| PUT | `/users/{userId}` | ユーザー情報更新 |
| DELETE | `/users/{userId}` | ユーザー削除（論理削除） |
| PUT | `/users/{userId}/status` | アカウントステータス変更 |

### プロファイル管理API

| メソッド | エンドポイント | 説明 |
|---------|---------------|------|
| GET | `/users/{userId}/profile` | プロファイル取得 |
| PUT | `/users/{userId}/profile` | プロファイル更新 |
| POST | `/users/{userId}/profile/verify` | 身分証明書アップロード |

### 設定管理API

| メソッド | エンドポイント | 説明 |
|---------|---------------|------|
| GET | `/users/{userId}/preferences` | 設定取得 |
| PUT | `/users/{userId}/preferences` | 設定更新 |

## セキュリティ機能

### アクセス制御

- ロールベースアクセス制御（RBAC）
- 自分の情報のみアクセス可能（管理者は例外）
- APIキーベース認証（サービス間通信）

### データプライバシー

- GDPR準拠のデータ処理
- データ削除要求への対応
- 個人情報の暗号化

### 入力検証

- Jakarta Validationによる厳密な検証
- XSS、SQLインジェクション対策
- ファイルアップロード制限

## 設定

### 環境変数

| 変数名 | 説明 | デフォルト値 |
|--------|------|-------------|
| `DATABASE_URL` | データベース接続URL | `jdbc:postgresql://localhost:5432/skiresortdb` |
| `DATABASE_USER` | データベースユーザー | `skiresort` |
| `DATABASE_PASSWORD` | データベースパスワード | `skiresort` |
| `REDIS_HOST` | Redisホスト | `localhost` |
| `REDIS_PORT` | Redisポート | `6379` |
| `FILE_UPLOAD_MAX_SIZE` | ファイルアップロード上限 | `10MB` |
| `PROFILE_PHOTO_PATH` | プロファイル写真保存パス | `/var/uploads/profiles` |

## データベース設定

### PostgreSQL設定

```sql
-- ユーザー管理テーブル
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- プロファイルテーブル
CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    birth_date DATE,
    phone_number VARCHAR(20),
    ski_level VARCHAR(20),
    profile_photo_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## ビルドと実行

### 前提条件

- Java 21 LTS
- Maven 3.9+
- PostgreSQL 15+

### ビルド

```bash
# Quarkus のデベロップメントモード
mvn quarkus:dev

# テスト実行
mvn test

# 実行可能 JAR の作成
mvn package
```

### 実行

```bash
# Uber-JAR を使ったスタンドアローン実行
java -jar target/user-management-service-1.0.0-SNAPSHOT-runner.jar

# HTTP エンドポイント例
curl http://localhost:8081/user-management-service/api/users
```

### Docker / Compose

```bash
# イメージのビルドと起動
docker compose up --build -d

# 動作確認 (例)
curl http://localhost:8081/user-management-service/api/users

# 停止
docker compose down
```

### Docker実行

```bash
# Docker Compose で実行
docker-compose up user-management-service
```

## API使用例

### ユーザー詳細取得

```bash
curl -X GET http://localhost:8081/users/123 \
  -H "Authorization: Bearer your_jwt_token"
```

### プロファイル更新

```bash
curl -X PUT http://localhost:8081/users/123/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "firstName": "田中",
    "lastName": "太郎",
    "phoneNumber": "090-1234-5678",
    "skiLevel": "INTERMEDIATE"
  }'
```

### 設定更新

```bash
curl -X PUT http://localhost:8081/users/123/preferences \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "language": "ja",
    "timezone": "Asia/Tokyo",
    "emailNotifications": true,
    "smsNotifications": false
  }'
```

## 監視とロギング

### ヘルスチェック

- `/health` エンドポイントでサービス状態確認
- データベース接続状態
- Redis接続状態

### メトリクス

- ユーザー登録数
- プロファイル更新率
- API使用統計
- レスポンス時間

### ログ

- ユーザー操作ログ
- プロファイル変更履歴
- セキュリティイベント

## セキュリティ考慮事項

### 本番環境での推奨設定

1. **データベース暗号化**
   - 保存時暗号化（PII）
   - 転送時暗号化（TLS）

2. **アクセス制御の強化**
   - 最小権限の原則
   - 定期的なアクセス権見直し

3. **監査ログ**
   - すべての変更操作の記録
   - 不正アクセスの検知

## トラブルシューティング

### よくある問題

1. **データベース接続エラー**
   - 接続文字列の確認
   - 認証情報の確認

2. **ファイルアップロード失敗**
   - ファイルサイズ制限の確認
   - ディスク容量の確認

3. **権限エラー**
   - JWTトークンの確認
   - ロール設定の確認

## 今後の拡張予定

- [ ] ソーシャルプロファイル連携
- [ ] プロファイル検証自動化
- [ ] AI による不正プロファイル検知
- [ ] モバイルアプリサポート
- [ ] 多言語プロファイル対応

## 開発者向け情報

### コード構成

```text
src/main/java/
├── com/skiresort/user/
│   ├── entity/          # JPA エンティティ
│   ├── service/         # ビジネスロジック
│   ├── repository/      # データアクセス
│   ├── resource/        # REST エンドポイント
│   └── exception/       # 例外クラス
```

### 依存関係

- Jakarta EE 11 API
- MicroProfile Config
- MicroProfile Health
- PostgreSQL JDBC
- Redis Client
- Apache Commons IO

## ライセンス

このプロジェクトは MIT ライセンスの下で公開されています。
