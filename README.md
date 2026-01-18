# dj_test_2025
# Docker Compose サンプル: Flask (Python) + Spring Boot (Java) + MariaDB

## 概要
- Backend: Python 3 + Flask (REST API)
- Frontend: Java 17 + Spring Boot (シンプルなコントローラ)
- DB: MariaDB 10
- Orchestration: docker compose
- 実行環境: Windows 11 の WSL2（Docker Desktop with WSL2 backend 推奨）

## 前提条件
- WSL2にはdocker, docker-compose がインストールされていること。
- MariaDBが用意されていること（外部からの接続設定可能になっていること）

## セットアップ（WSL2 上）
1. WSL2 と Docker Desktop をインストールし、Docker Desktop の WSL integration で使用中のディストリビューションを有効にする。
2. このリポジトリ（フォルダ）を WSL2 のファイルシステム上に置く（例: `~/projects/sample`）。
3. WSL2 ターミナルでプロジェクトルートに移動して以下を実行:
   ```bash
   docker compose up --build
   ```
   - キャッシュクリアで実行する場合は以下を実行：
   ```bash
   docker compose build --no-cache frontend
   ```
4. ブラウザまたは curl で確認:
   - Frontend: http://localhost:8080/hello
   - Backend:  http://localhost:5000/api/users
   - MySQL: (ホスト) localhost:3306（必要であれば MySQL クライアントで接続）

## ログイン実行手順
1. ローカル実行:
   - cd frontend
   - mvn spring-boot:run
   - ブラウザで http://localhost:8080/login にアクセス。初期ユーザは user / password。
2. Docker 実行（compose を使う場合）:
   - docker compose build --no-cache frontend
   - docker compose up -d frontend
   - ブラウザで http://localhost:8080/login

---- 動作の流れ ----

   - ユーザが /login にアクセス -> login.html を表示
   - フォーム送信は /login (POST) に対して Spring Security が処理（CustomUserDetailsService が DB からユーザを読み込み、BCrypt でパスワードを比較）
   - 認証成功 -> /top にリダイレクト（HomeController が表示）
   - /top は認証が必要（未認証なら /login にリダイレクト）
   - /logout でログアウトし /login?logout に遷移



## 管理画面使い方メモ：

   - 管理画面は /admin/users、編集画面は /admin/users/{id}/edit。
      - ブラウザで http://localhost:8080/admin/users
      - ブラウザで http://localhost:8080/admin/users/{id}/edit
   - 初期ユーザ（DemoApplication の初期化）で admin/adminpass が作られるので、まず admin でログインして /admin/users を開いてください。
   - 新規登録は /register から実行。登録後は /login?registered にリダイレクトされます。

### 注意点
- WSL2 で Docker を使う際は、プロジェクトを Windows 側のファイルシステム（C:\...）に置くとファイル I/O が遅くなることがあるため、できれば WSL のホーム下に置くことを推奨します。
- MySQL の初期データは `./db/init.sql` に置いてあります。compose 起動時に自動で投入されます。
- 環境変数やパスワードを本番でそのまま使わないでください。サンプル用です。

## ファイル構成（抜粋）
- docker-compose.yml
- backend/
  - Dockerfile
  - app.py
  - requirements.txt
- frontend/
  - Dockerfile
  - pom.xml
  - src/main/java/com/example/demo/DemoApplication.java
  - src/main/java/com/example/demo/controller/HelloController.java
  - src/main/resources/application.properties

## DB接続確認
   ```bash
   mysql -h 192.168.11.26 -P 3307 -u testuser -p python_schema
   ```

   ```bash
   docker run --rm -it mysql:8.0 mysql -h 192.168.11.26 -P 3307 -u testuser -p python_schema
   ```



使い方・確認手順

上記ファイルを frontend プロジェクトに追加または置換してください。
ローカルで確認する場合:
cd frontend
mvn spring-boot:run
ブラウザで http://localhost:8080/login にアクセス
Docker 経由で実行する場合:
docker compose build frontend
docker compose up -d frontend
ブラウザで http://localhost:8080/login にアクセス
ログイン画面で認証後、/top に遷移することを確認してください。ログアウトは /logout。
注意（セキュリティ）

本サンプルは学習目的で簡易実装です。実運用では次を検討してください：
Spring Security の導入（認証・認可・CSRF 対策）
HTTPS（TLS）
パスワードのハッシュ化、データベースでのユーザ管理
セッション固定攻撃やセッションタイムアウト設定