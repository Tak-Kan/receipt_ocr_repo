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
4. ブラウザまたは curl で確認:
   - Frontend: http://localhost:8080/hello
   - Backend:  http://localhost:5000/api/users
   - MySQL: (ホスト) localhost:3306（必要であれば MySQL クライアントで接続）

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

