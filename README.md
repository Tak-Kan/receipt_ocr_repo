# dj_test_2025
# Docker Compose サンプル: Flask (Python) + Spring Boot (Java) + MariaDB

## 概要
- Backend1: Java 17, Spring Boot, Spring Security, Spring Data JPA
- Backend2: Python 3 + Flask (REST API)
- Frontend: Thymeleaf (Spring Security拡張タグ利用), Bootstrap 5
- DB: MariaDB 10
- Orchestration: docker compose
- OCR連携: Azure Document Intelligence
- 実行環境: Windows 11 の WSL2（Docker Desktop with WSL2 backend 推奨）

## 前提条件
- WSL2にはdocker, docker-compose がインストールされていること。
- MariaDBが用意されていること（外部からの接続設定可能になっていること）
- Azure Document IntelligenceのEndpoint、Keyを取得済み
- 画像ファイルをNASに保存する場合、Windows側にネットワークドライブとして割り当て済みになっていること。

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
4. ブラウザまたは curl で動作確認:
   - Frontend: http://localhost:8080/hello
   - Backend:  http://localhost:5000/api/users
   - MySQL: (ホスト) localhost:3306（必要であれば MySQL クライアントで接続）

5. 環境変数の設定
   5.1. `cp .env.template .env` コマンドを実行し、設定ファイルをコピー。
   5.2. `.env` ファイルを開き、各自の環境に合わせてパスワード等を書き換える。
   5.3. `docker compose up -d` を実行。

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

## WSLへのNASマウント状況確認
### ステップ1：WSL2 と NAS のマウント状況の確認
Windowsを再起動したり、ネットワークが瞬断したりすると、WSL2上の `drvfs` マウントは外れてしまいます。外れた状態でDockerを起動すると、WSL2のローカルにファイルが保存されます。

【確認・解決方法】<br>
WSL2（Ubuntu）のターミナルを開き、NASをマウントしているはずのディレクトリの中身を確認します。（※仮にマウントポイントを `/mnt/nas` とします）

```Bash
# マウントされているか確認
mount | grep drvfs

# フォルダの中身を確認
ls -la /mnt/nas
```

もし何も表示されない、あるいはエラーが出る場合はマウントが外れています。Dockerコンテナを一度停止（`docker compose down`）してから、再度NASをWSL2にマウントし直してください。

### ステップ2：`docker-compose.yml` のボリューム設定の確認
Dockerコンテナの `/app/uploads` と、WSL2上のNASマウントポイントが正しく紐づいていない可能性があります。

【確認・解決方法】<br>
`docker-compose.yml` の `volumes` の設定を確認してください。ホスト側（左側）のパスが、実際にNASがマウントされているWSL2の絶対パスになっている必要があります。

```YAML
services:
  app:
    # ...
    volumes:
      # ❌ 誤り（現在のディレクトリ内に uploads フォルダを作ってしまう）
      # - ./uploads:/app/uploads
      
      # ⭕️ 正しい設定（WSL2側でNASをマウントしている絶対パスを指定）
      - /mnt/nas/receipts:/app/uploads
```

設定を修正した場合は、`docker compose up -d` でコンテナを再作成してください。

### ステップ3：コンテナ再起動による「揮発」の確認（テスト）
現在どこに保存されているかを特定するためのテストです。

【確認・解決方法】<br>
以下のコマンドでDockerコンテナを一度破棄し、再度起動してみてください。

```Bash
docker compose down
docker compose up --build frontend
```

起動後、アプリの編集画面を開いて画像がリンク切れ（表示されない）になれば、画像は「コンテナ内部」にしか存在していなかったことが確定します（コンテナ破棄と共に画像も消滅した状態です）。<br>
逆に画像がまだ表示される場合は、「WSL2のローカルディレクトリ（マウントされていない状態の `/mnt/...` など）」に保存されていることが確定します。

Dockerと外部ストレージの連携では「マウントの順番（必ずNASをマウントしてからDockerを起動する）」が非常に重要になります。


## WSLへのNASマウント手順
### Step 1: 既存のDockerコンテナを停止する
マウントが外れた状態の「ローカルフォルダ」を掴んだままにならないよう、まずはコンテナを停止します。<br>
WSL2のターミナルで対象のディレクトリに移動し、以下のコマンドを実行します。

```Bash
docker compose down
```

### Step 2: Windows側でのネットワークドライブ割り当て（確認）
エクスプローラーを開き、NASの共有フォルダが 「Zドライブ」や「Yドライブ」などのネットワークドライブとして割り当てられているか を確認してください。<br>
（※ここでは仮に `Z:` ドライブとして割り当てられているものとして進めます。ご自身の環境に合わせて文字を読み替えてください。）

### Step 3: WSL2 で NAS をマウントする
WSL2（Ubuntu）のターミナルを開き、以下のコマンドを実行します。
1. マウントポイントの作成（すでに存在する場合は不要です）
   ```Bash
   sudo mkdir -p /mnt/nas
   ```
2. `drvfs` を使ったマウントの実行<br>
   Windowsの `Z:` ドライブを、WSL2の `/mnt/nas` にマウントします。
   ```Bash
   sudo mount -t drvfs Z: /mnt/nas
   ```

### Step 4: マウント成功の確認
以下のコマンドで、NASの中身（ファイルやフォルダ）が表示されるか確認します。

```Bash
ls -la /mnt/nas
```

ここでNASのファイル群が見えれば、マウントは成功です！

### Step 5: Dockerコンテナの再起動
NASが正しくマウントされた状態で、再度Dockerコンテナを起動します。

```Bash
docker compose up -d
```

これで、コンテナ内の `/app/uploads` とNASが正しく繋がり、画像が直接NASに保存されるようになります。

## NAS自動マウント設定
WSL2のターミナルで `sudo nano /etc/fstab` を開き、末尾に以下の1行を追記して保存してください。

```Bash
Z: /mnt/nas drvfs defaults 0 0
```
（※ `Z:` は実際のドライブレターに合わせてください）

これを設定しておけば、次回以降はWSL2起動時に自動的にマウントされるようになります。まずは手動でのマウント（Step 3〜4）を試し、NASのファイルが見えるか確認してみてください！



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