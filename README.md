# receipt_ocr_repo

家計簿Webアプリケーションのサンプルです。レシート画像をOCRで読み取り、内容を確認・編集してから家計簿データとして登録します。

## システム構成
- **Webアプリ**: Java 17、Spring Boot 3.1.5、Spring Security、Spring Data JPA、Thymeleaf
- **OCR API**: Python 3.11、Flask、Azure Document Intelligence
- **データベース**: MariaDB（Compose外部のDBへ接続）
- **画像ストレージ**: ローカル/NAS、Azure Blob Storage、AWS S3の実装(`application.properties`の設定で切り替え)
- **実行環境**: Docker Compose (Windows 11 の WSL2上の動作を想定)

AWS S3モードでは、Spring BootアプリがAWS SDK for Javaを使って画像を保存・配信します。Flask APIはOCR処理を担当します。

## レシート登録の流れ

1. ログイン後、Web画面からレシート画像をアップロードします。
2. Spring Bootアプリは画像を一時保存し、Flask APIへ画像を渡します。
3. Flask APIがAzure Document Intelligenceでレシートを解析し、店舗名・日付・明細・合計金額などをJSONで返します。
4. Web画面でOCR結果を確認・編集します。
5. 登録確定時に、画像を選択中のストレージへ移し、家計簿データと画像パスをMariaDBへ保存します。

## 起動前の準備

- WSL2にはdocker, docker-compose がインストールされていること。
- MariaDBが用意されていること（外部からの接続設定可能になっていること）
- Azure Document IntelligenceのEndpoint、Keyを取得済み
- 画像ファイルをNASに保存する場合、Windows側にネットワークドライブとして割り当て済みになっていること。
- 画像ファイルをAWS S3に保存する場合、AWS側の各種設定済みで対象バケットとAWS認証情報を取得済みになっていること。

リポジトリのルートに `.env` を作り、以下を環境に合わせて設定してください。

```dotenv
# MariaDB接続先（Compose内の名前または到達可能なホスト名）
DB_HOST=db
DB_PORT=3306
DB_NAME=sampledb
DB_USER=appuser
DB_PASSWORD=replace-me

# Azure Document Intelligence
AZURE_DOC_INTEL_ENDPOINT=https://your-resource.cognitiveservices.azure.com/
AZURE_DOC_INTEL_KEY=replace-me

# AWS S3モードで使用
S3_BUCKET=your-private-bucket
AWS_REGION=ap-northeast-1
```

`DB_HOST` の既定例 `db` は、Composeネットワーク内にMariaDBサービスを定義しているという意味ではありません。このブランチのComposeにはMariaDBサービスがないため、実際のDBホスト名を指定し、コンテナから接続できるようにしてください。


## AWS S3モード

このブランチの `frontend/src/main/resources/application.properties` は `storage.type=aws` を規定値として指定しています。Composeは `S3_BUCKET` と `AWS_REGION` を `APP_STORAGE_S3_BUCKET`、`APP_STORAGE_S3_REGION` としてフロントエンドへ渡します。リージョンの既定値は `ap-northeast-1` ですが、バケット名は設定が必要です。

AWS SDK for Javaの `DefaultCredentialsProvider` が認証情報を取得します。ローカル開発では、Composeがホストの `${HOME}/.aws` をコンテナの `/root/.aws` に読み取り専用でマウントするため、AWS CLIのプロファイルを利用できます。AWS認証キーをソースコードや `.env` に書き込まないでください。運用環境では実行環境に適したIAMロール等を利用してください。

S3モードの画像処理は次のとおりです。

- OCR・確認中の画像は、コンテナ内の一時領域 `/tmp/receipt-temp` に保存されます。
- 一時ファイル名はUUIDベースで生成され、24時間を超えたファイルは次回のアップロード時に掃除されます。
- 登録確定時、画像は `receipts/{ユーザーID}/{年}/{月}/{ファイル名}` のキーでS3にアップロードされます。
- DBには画像データではなく、`/images/receipts/...` 形式のアプリ内パスが保存されます。
- S3バケットは非公開のまま、Spring BootアプリがS3から画像を取得して配信します。
- 登録済み画像の取得はログイン必須です。新形式のキーでは本人または管理者のみが閲覧できます。旧形式の `receipts/{ファイル名}` は所有者を特定できないため、ログイン済みユーザーに許可されます。

## ローカル/NAS・Azure Blobモード

ストレージ実装は `StorageConfig` が `storage.type` に応じて選択します。

- `local`: ローカル/NAS用の実装。`storage.type` が未指定の場合もこの実装が選ばれます。
- `cloud`: Azure Blob Storage用の実装。
- `aws`: AWS S3用の実装。このブランチでは `application.properties` の設定値です。

ローカル/NASモードを利用する場合は、設定を `local` にし、保存先ディレクトリがアプリから利用できることを確認してください。DockerでNASを使う場合、ホスト側のNASマウント先をコンテナの保存先へマウントします。マウント元は各環境に合わせて設定してください。

## ログインとアクセス制御

`GET /login` でログイン画面を表示し、フォームは `POST /login-process` へユーザーコードとパスワードを送信します。Spring SecurityがDBのユーザー情報を読み込み、BCryptでパスワードを照合します。ログイン状態はサーバー側セッションで管理され、JWTやOAuthトークンを発行する方式ではありません。

- `/login` と静的ファイルなどは未ログインでもアクセスできます。
- `/admin/**` は `ROLE_ADMIN` が必要です。
- それ以外のWeb画面はログインが必要です。
- ログアウトではセッションを無効化します。
- CSRF保護はSpring Securityの既定設定で有効です。

開発用の初期管理者は、ユーザーテーブルが空の場合に `DataSeedConfig` が作成します。コード上のユーザーコードと初期パスワードを確認し、本番環境でサンプル資格情報を使用しないでください。パスワードはBCryptハッシュで保存されます。

## 起動

`.env` を準備したうえで、リポジトリのルートから起動します。

```bash
docker compose up --build
```

主なポートは、Webアプリが `8080`、OCR APIが `5000` です。Webアプリのログイン画面は `http://localhost:8080/login` です。MariaDBはComposeで起動しないため、事前にDBを用意してください。

### 設定値等を変更した場合

キャッシュクリアで実行する場合は以下を実行：

```bash
docker compose build --no-cache frontend
```

## 主要なコード

- `frontend/src/main/java/com/example/demo/security/SecurityConfig.java`: Web認証・認可
- `frontend/src/main/java/com/example/demo/controller/AccountController.java`: レシート登録の画面フロー
- `frontend/src/main/java/com/example/demo/service/OcrService.java`: Flask OCR API呼び出し
- `backend/app.py`: Azure Document IntelligenceによるOCR API
- `frontend/src/main/java/com/example/demo/config/StorageConfig.java`: ストレージ実装の選択
- `frontend/src/main/java/com/example/demo/service/impl/S3FileStorageServiceImpl.java`: S3への保存・削除・取得
- `frontend/src/main/java/com/example/demo/controller/S3ImageController.java`: S3画像の認証付き配信
- `frontend/src/main/resources/application.properties`: アプリ設定
- `docker-compose.yml`: コンテナと環境変数の設定

## 注意

- このリポジトリのREADMEや設計資料に残る過去の接続先・初期ユーザー情報・NAS手順は、現在の環境設定やコードと異なる場合があります。実際の設定値は `application.properties`、`docker-compose.yml`、各サービスの実装を確認してください。
- WSL2 で Docker を使う際は、プロジェクトを Windows 側のファイルシステム（C:\...）に置くとファイル I/O が遅くなることがあるため、できれば WSL のホーム下に置くことを推奨します。
