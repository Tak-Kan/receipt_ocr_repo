# 家計簿Webアプリケーション システム構成資料

## 概要

このプロジェクト（**dj_test_2025**）は、個人利用向けの**家計簿Webアプリケーション**です。マイクロサービス的なアーキテクチャを採用し、Java/Spring Boot製フロントエンド、Python/Flask製のOCR連携バックエンド、MariaDBデータベースをDocker Composeで統合したマルチティアシステムとなっています。特筆すべき特徴として、レシート画像をAzure Document Intelligenceで自動OCR解析し、デジタル家計簿へ自動入力する機能を実装しています。

**主な用途**：
- ユーザー認証・権限管理を備えた家計簿データ管理
- レシート画像のOCR解析による自動データ入力
- マスタ管理（ユーザー・ロール・カテゴリ）

---

## システム構成図

```
┌─────────────────────────────────────────────────────┐
│          Windows 11 + WSL2 + Docker Desktop          │
├─────────────────────────────────────────────────────┤
│                                                       │
│  ┌──────────────────┐  ┌──────────────────┐        │
│  │   Frontend       │  │    Backend       │        │
│  │  (Spring Boot)   │  │   (Flask API)    │        │
│  │   Port 8080      │  │    Port 5000     │        │
│  │ Java 17 JRE      │  │   Python 3.11    │        │
│  │ Thymeleaf        │  │ Azure Doc Intel  │        │
│  │ Spring Security  │  │ gunicorn         │        │
│  └────────┬─────────┘  └──────────┬───────┘        │
│           │                       │                 │
│           └───────────────────────┘                 │
│                 (API calls)                         │
│                      ↓                              │
│           ┌──────────────────────┐                 │
│           │    MariaDB 10.x      │                 │
│           │   Port 3306          │                 │
│           │  python_schema       │                 │
│           └──────────────────────┘                 │
│                      ↓                              │
│    External Storage (NAS via drvfs mount)          │
│    /mnt/nas/hams_uploads → /app/uploads            │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 技術スタック詳細

### **1. フロントエンド層（frontend/）**

#### 言語・フレームワーク
- **言語**: Java 17
- **フレームワーク**: Spring Boot 3.1.5
- **ビューエンジン**: Thymeleaf（Spring Security 6対応拡張タグ利用）
- **UIフレームワーク**: Bootstrap 5
- **ビルドツール**: Maven 3.9

#### 主要ライブラリ（pom.xml から）

| ライブラリ | バージョン | 用途 |
|-----------|-----------|------|
| spring-boot-starter-web | 3.1.5 (親) | Webアプリケーション基盤 |
| spring-boot-starter-thymeleaf | 3.1.5 (親) | HTMLテンプレートエンジン |
| thymeleaf-extras-springsecurity6 | 3.1.5 (親) | セキュリティ認可制御タグ（`sec:authorize`） |
| spring-boot-starter-security | 3.1.5 (親) | 認証・認可フレームワーク |
| spring-boot-starter-data-jpa | 3.1.5 (親) | ORM（Hibernate） |
| mariadb-java-client | 3.3.x (親) | MariaDBドライバ |
| mapstruct | 1.5.5.Final | Entity ↔ DTO マッピング（コンパイル時生成） |
| mapstruct-processor | 1.5.5.Final | MapStructコンパイラプラグイン |
| tess4j | 5.8.0 | Tesseract OCR（ローカルOCR用、現在は使用していない可能性） |
| commons-io | 2.11.0 | ファイルI/O ユーティリティ |
| lombok | 親 | ボイラープレート削減（ゲッター・セッター自動生成） |
| spring-boot-starter-validation | 3.1.5 (親) | JSR 303/380 バリデーション |

#### ビルドプロセス（Dockerfile - Multi-stage）

```dockerfile
# Stage 1: Build（Maven 3.9 + JDK 17）
- pom.xml 先行ロード → 依存キャッシング（`--mount=type=cache`）
- ソースコピー → mvn package -DskipTests

# Stage 2: Runtime（Eclipse Temurin 17-jre）
- JAR 配置 → java -jar 実行
```

**結果**: 最終イメージサイズが削減（JDKではなくJRE使用）

#### セキュリティ設定

- **認証**: DaoAuthenticationProvider + CustomUserDetailsService（DB `m_user` テーブル照合）
- **パスワード**: BCrypt暗号化
- **CSRF対策**: Thymeleafの自動CSRF トークン生成（`th:action`）
- **認可**: Spring Security の `@PreAuthorize` または `sec:authorize` タグで権限チェック
- **セッション管理**: Spring Security の標準設定（セッションID固定攻撃対策は検討要）

---

### **2. バックエンド層（backend/）**

#### 言語・フレームワーク
- **言語**: Python 3.11（slim イメージ）
- **Webフレームワーク**: Flask 2.2.5
- **ORM**: SQLAlchemy 1.4.49
- **アプリケーションサーバー**: Gunicorn 20.1.0
- **ビルド**: Docker（Dockerfile ベース）

#### 主要ライブラリ（requirements.txt）

| ライブラリ | バージョン | 用途 |
|-----------|-----------|------|
| Flask | 2.2.5 | マイクロWebフレームワーク |
| Flask-SQLAlchemy | 3.0.3 | SQLAlchemy Flask統合 |
| SQLAlchemy | 1.4.49 | ORM |
| pymysql | 1.0.3 | MySQL/MariaDB ピュアPythonドライバ |
| gunicorn | 20.1.0 | 本番用WSGIサーバー |
| python-dotenv | 1.0.0 | .env ファイル読み込み |
| azure-ai-documentintelligence | latest | Azure Document Intelligence SDK（OCR） |
| azure-identity | latest | Azure 認証 |
| boto3 | latest | AWS SDK（将来の拡張用） |

#### 主要API エンドポイント

**POST `/api/analyze-receipt`**
- **入力**: `multipart/form-data` で画像ファイル（`file` パラメータ）
- **処理フロー**:
  1. リクエストから画像バイト列を取得
  2. Azure Document Intelligence クライアント初期化（`AZURE_DOC_INTEL_ENDPOINT`, `AZURE_DOC_INTEL_KEY` から）
  3. `begin_analyze_document("prebuilt-receipt", body=image_bytes, ...)` で解析開始
  4. ポーラー結果から以下を抽出：
     - `MerchantName`: 店舗名
     - `TransactionDate`, `TransactionTime`: 取引日時
     - `Items[].Description`, `Items[].TotalPrice`: 商品・金額
     - `Total`: 合計金額
     - レシート番号（正規表現 `[Tt]\d{13}` で検索）
  5. JSON レスポンス返却

- **出力例**:
  ```json
  {
    "merchant_name": "STORE NAME",
    "date": "2025-07-16",
    "time": "14:30",
    "invoice_number": "T1234567890123",
    "items": [
      {"name": "Item 1", "price": "1000"},
      {"name": "Item 2", "price": "2000"}
    ],
    "total_amount": "3000"
  }
  ```

#### Dockerfile（Backend）

```dockerfile
FROM python:3.11-slim
WORKDIR /app
RUN apt-get update && apt-get install -y build-essential libmariadb-dev-compat libmariadb-dev
COPY requirements.txt . && pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 5000
ENV FLASK_APP=app.py FLASK_ENV=production
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "app:app"]
```

**ポイント**:
- 依存キャッシング（`--no-cache-dir`）
- MariaDB開発ライブラリインストール（C拡張用）
- Gunicorn でマルチワーカー対応

---

### **3. データベース層**

#### データベース
- **RDBMS**: MariaDB 10.x（MySQLの互換フォーク）
- **スキーマ名**: `python_schema`
- **接続方式**: Docker ネットワーク内（service name: `db`）
- **ホストOS からのアクセス**: `localhost:3306`

#### 初期化
- `db/init.sql` が Docker Compose 起動時に自動実行
- テーブル作成・ダンプ投入

#### マイグレーション SQL ファイル
- `db/create_image_table.sql`: 画像テーブル追加
- `db/migrate_roles.sql`: ロール関連テーブル マイグレーション

#### テーブル構成（推定）

| テーブル名 | 用途 | 備考 |
|-----------|------|------|
| m_user | ユーザーマスタ | BCryptで暗号化されたパスワード、ロール外部キー |
| m_role | ロールマスタ | ROLE_ADMIN, ROLE_USER など |
| m_category | カテゴリマスタ | 家計簿費目（食費・光熱費など） |
| t_account | 家計簿 | 年月日、金額、カテゴリ外部キー、画像パス |
| t_account_detail | 家計簿明細 | t_account の行詳細、複数行対応 |

---

### **4. インフラストラクチャ**

#### Docker Compose 構成（docker-compose.yml）

```yaml
services:
  backend:
    build: ./backend
    image: sample-backend
    ports: ["5000:5000"]
    environment:
      - AZURE_DOC_INTEL_ENDPOINT
      - AZURE_DOC_INTEL_KEY
    networks: [sample-network]
    restart: unless-stopped

  frontend:
    build: ./frontend
    image: sample-frontend
    ports: ["8080:8080"]
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mariadb://db:3306/...
      - SPRING_DATASOURCE_USERNAME
      - SPRING_DATASOURCE_PASSWORD
      - BACKEND_API_URL=http://backend:5000/api
    volumes:
      - /mnt/nas/hams_uploads:/app/uploads
    networks: [sample-network]
    restart: unless-stopped

  db:
    image: mariadb:10
    environment:
      - MYSQL_ROOT_PASSWORD
      - MYSQL_DATABASE
      - MYSQL_USER
      - MYSQL_PASSWORD
    networks: [sample-network]
    volumes:
      - ./db/init.sql:/docker-entrypoint-initdb.d/init.sql

networks:
  sample-network:
    driver: bridge
```

**ネットワーク**: `bridge` ドライバで `backend` → `frontend` → `db` が通信可能


#### ストレージ統合（NAS マウント）

**アーキテクチャ**:
1. Windows 11 で NAS をネットワークドライブとして割り当て（例: `Z:`）
2. WSL2 (Ubuntu) で `drvfs` を使ってマウント: `sudo mount -t drvfs Z: /mnt/nas`
3. Docker Compose で `/mnt/nas/hams_uploads` を `/app/uploads` にマウント

**目的**: DB の BLOB 肥大化を防止。ファイルパスのみ DB に保存

---

## データフロー

### ユースケース1: レシート OCR 解析 → 家計簿登録

```
ユーザー（Frontend 画面）
    ↓
[レシート画像アップロード]
    ↓
Spring Boot Controller (/account/upload など)
    ↓
OcrService (RestTemplate)
    ↓ [HTTP POST /api/analyze-receipt]
    ↓
Flask backend (/api/analyze-receipt)
    ↓
Azure Document Intelligence SDK (クラウド側)
    ↓ [OCR 結果 JSON 返却]
    ↓
OcrResponseDto（Java）
    ↓
OcrMapper（MapStruct）
    ↓ [Entity → AccountForm 変換]
    ↓
Spring Security + Spring Data JPA
    ↓ [INSERT / UPDATE]
    ↓
MariaDB (t_account, t_account_detail)
    ↓
画像ファイル → /app/uploads (NAS)
```

### ユースケース2: ユーザー認証フロー

```
ユーザー
    ↓ [GET /login]
    ↓
Thymeleaf (login.html 表示)
    ↓ [POST /login（フォーム送信）]
    ↓
Spring Security DaoAuthenticationProvider
    ↓
CustomUserDetailsService.loadUserByUsername()
    ↓
m_user テーブル クエリ
    ↓
BCrypt パスワード比較
    ↓
認証成功 → SecurityContext に UserPrincipal 格納
    ↓ [Redirect /top]
    ↓
HomeController（@PreAuthorize("isAuthenticated()")）
    ↓
Thymeleaf (top.html 表示、`sec:authorize` で権限表示制御)
```

---

## 実装済み機能

### 1. **認証・認可**
- Spring Security による従来型ログイン（セッションベース）
- BCrypt パスワードハッシュ化
- CSRF トークン自動生成
- ロールベースアクセス制御（RBAC）: `ROLE_ADMIN`, `ROLE_USER` など
- Thymeleaf `sec:authorize` タグで画面レベルの権限制御

### 2. **マスタ管理（/admin/** 配下）**
- **ユーザー管理**: 一覧、新規登録、編集、削除
  - パスワードは入力時のみ更新（null 許容）
  - スーパー管理者（ID:1）の削除・権限剥奪防止
- **ロール管理**: 使用中ロールの削除防止
- **カテゴリ管理**: 家計簿の費目マスタ
  - 使用中カテゴリの削除防止

### 3. **家計簿機能**
- Account（年月日、金額、カテゴリ）と AccountDetail（明細行）の親子関係
- `@ManyToOne` で Category と紐付け（未分類 = null 許容）
- 明細の連番は `AccountService` で自動付与

### 4. **OCR 連携**
- Thymeleaf から Flask バックエンドへ RESTful 連携
- Azure Document Intelligence v1.0.0+ 対応
- レシート番号を正規表現で抽出

### 5. **ファイル管理**
- `FileStorageService` インターフェース（戦略パターン）
- `LocalFileStorageServiceImpl`: ローカル/NAS 保存
- 将来: Azure Blob Storage への拡張用設計

### 6. **UI / UX**
- Bootstrap 5 ベース
- Thymeleaf fragment で共通ヘッダー部品化
- ドロップダウンメニューで管理機能への導線集約

---

## アーキテクチャパターン

### レイヤー化アーキテクチャ

```
┌────────────────────────────┐
│   Presentation Layer       │
│ (Controller + Thymeleaf)   │
└──────────┬─────────────────┘
           ↓
┌────────────────────────────┐
│   Service Layer            │
│ (Business Logic)           │
│ - AccountService           │
│ - OcrService (RestTemplate)│
│ - FileStorageService       │
└──────────┬─────────────────┘
           ↓
┌────────────────────────────┐
│   Repository Layer         │
│ (Spring Data JPA)          │
│ - AccountRepository        │
│ - UserRepository           │
└──────────┬─────────────────┘
           ↓
┌────────────────────────────┐
│   Data Layer               │
│ (Entity + Mapper)          │
│ - Hibernate ORM            │
│ - MapStruct                │
└──────────┬─────────────────┘
           ↓
┌────────────────────────────┐
│   Database Layer           │
│ (MariaDB)                  │
└────────────────────────────┘
```

### デザインパターン活用

| パターン | 実装場所 | 用途 |
|---------|---------|------|
| Strategy | FileStorageService | ストレージ実装の切り替え |
| DTO Mapper | MapStruct | Entity ↔ Form 変換の自動化 |
| Dependency Injection | @Service, @Repository | Spring がスコープ管理 |
| Repository | Spring Data JPA | データアクセスの抽象化 |
| Template Method | WebSecurityConfigurerAdapter | セキュリティ設定の段階実行 |

---

## 依存関係と通信フロー

### コンポーネント間の依存性

```
Frontend (Spring Boot)
  ├→ Spring Security (認証・認可)
  ├→ Spring Data JPA (Entity → DB)
  ├→ Thymeleaf (View)
  ├→ RestTemplate (HTTP Client)
  │  └→ Backend (Flask)
  │     ├→ Azure Document Intelligence
  │     └→ Flask-SQLAlchemy (SQLAlchemy)
  └→ MapStruct (DTO Mapping)

Backend (Flask)
  ├→ Flask (Web Framework)
  ├→ SQLAlchemy (ORM - 使用していない可能性)
  └→ Azure SDK
     └→ Azure Cognitive Services (OCR)

Database (MariaDB)
  ├← Spring Boot (JPA)
  └← Python (SQLAlchemy - 可能性)

Storage (NAS / drvfs Mount)
  └← Spring Boot (FileStorageService)
```

---

## セキュリティ考慮事項

### 実装済み
✅ パスワードハッシュ化（BCrypt）  
✅ CSRF トークン生成・検証  
✅ ロールベース認可  
✅ セッション管理  

### 検討が必要
⚠️ HTTPS/TLS（本番環境で必須）  
⚠️ セッション固定攻撃対策の確認  
⚠️ セッションタイムアウト設定  
⚠️ SQL インジェクション対策（パラメータクエリ使用）  
⚠️ XSS 対策（Thymeleaf 自動エスケープを確認）  
⚠️ 認可境界テスト（他ユーザーのデータアクセス防止）  
⚠️ Azure 認証情報の セキュリティ（Key Vault 検討）

---

## セットアップ・実行手順

### 前提条件
- Windows 11 + WSL2 (Ubuntu)
- Docker Desktop（WSL2 バックエンド）
- git clone 済み

### セットアップ

```bash
# 1. リポジトリクローン
git clone https://github.com/YuukiLearn/dj_test_2025.git
cd dj_test_2025

# 2. 環境変数設定
cp .env.template .env
# .env を編集して Azure ���証情報・DB パスワードを設定

# 3. NAS マウント（Windows ドライブが Z: の場合）
sudo mkdir -p /mnt/nas
sudo mount -t drvfs Z: /mnt/nas

# 4. Compose 起動
docker compose up --build

# 5. ブラウザアクセス
# Frontend: http://localhost:8080/login
# Backend: http://localhost:5000/api/analyze-receipt (POST)
```

### ローカル開発実行（Frontend のみ）

```bash
cd frontend
mvn spring-boot:run
# http://localhost:8080/login
```

### Docker 個別ビルド

```bash
# Backend イメージビルド
docker build -t backend_docker backend/

# Frontend イメージビルド
docker build -t frontend_docker frontend/
```

---

## 今後の拡張ポイント

### 短期
- [ ] セッションタイムアウト設定の追加
- [ ] エラーハンドリング・ログ記録の統一化
- [ ] ユニットテスト・統合テストの拡充
- [ ] XSS/SQLi 対策の診断

### 中期
- [ ] ダッシュボード・集計機能（月別集計・グラフ）
- [ ] API ドキュメント（Swagger/OpenAPI）
- [ ] 複数ユーザー向けの権限分離テスト
- [ ] ファイルアップロードサイズ制限・バリデーション

### 長期
- [ ] Azure Blob Storage への画像保存切り替え
- [ ] マイクロサービス化（認証・カテゴリを独立サービス化）
- [ ] キャッシング（Redis）
- [ ] 非同期処理（Kafka、Spring Batch）

---

## トラブルシューティング

### よくある問題と対応

| 問題 | 原因 | 対応 |
|-----|------|------|
| NAS が見えない（コンテナから） | drvfs マウント外れ | `docker compose down` → NAS 再マウント → 再起動 |
| DB 接続エラー | ホスト名解決失敗 | `SPRING_DATASOURCE_URL` の `db` が正確か確認 |
| OCR が 400 エラー | Azure キー無効 | `.env` の `AZURE_DOC_INTEL_*` を再確認 |
| ビルドタイムアウト | Maven 依存キャッシュ未効率 | `docker compose build --no-cache` で一度全リセット |

---

## 参考資料

- **Spring Boot 3.1.5**: https://spring.io/projects/spring-boot
- **Thymeleaf**: https://www.thymeleaf.org/
- **Spring Security 6**: https://spring.io/projects/spring-security
- **MapStruct**: https://mapstruct.org/
- **Azure Document Intelligence**: https://learn.microsoft.com/en-us/azure/ai-services/document-intelligence/
- **Docker Compose**: https://docs.docker.com/compose/
- **MariaDB**: https://mariadb.org/

---

**資料版**: v1.0（2025-07-16）  
**対象エンジニア**: 開発経験 5 年程度
