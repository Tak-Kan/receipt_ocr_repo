"""
Flask app configured to connect to a remote MariaDB (e.g. Synology DSM) or a local DB.
Usage:
 - Either set DATABASE_URL (full SQLAlchemy URL) or set DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASS.
 - Optional retry settings: DB_CONNECT_RETRIES, DB_CONNECT_DELAY (seconds)

Example SQLAlchemy URL:
  mysql+pymysql://appuser:apppass@192.168.1.50:3306/sampledb
"""
import os
import time
import logging
from datetime import datetime
from urllib.parse import quote_plus

from flask import Flask, jsonify, request
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import create_engine, text
from sqlalchemy.exc import OperationalError
from sqlalchemy.orm import declarative_base, sessionmaker
import json
# クラウドSDK
import boto3
from azure.core.credentials import AzureKeyCredential
from azure.ai.documentintelligence import DocumentIntelligenceClient

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

# Build DATABASE_URI from env or use provided full URL
DATABASE_URL = os.getenv("DATABASE_URL") or os.getenv("DB_URL") or os.getenv("DB_URI")
if not DATABASE_URL:
    db_host = os.getenv("DB_HOST", "db")
    db_port = os.getenv("DB_PORT", "3306")
    db_name = os.getenv("DB_NAME", "sampledb")
    db_user = os.getenv("DB_USER", "appuser")
    db_pass = os.getenv("DB_PASS", "apppass")
    # quote password in case it contains special chars
    db_pass_quoted = quote_plus(db_pass)
    DATABASE_URL = f"mysql+pymysql://{db_user}:{db_pass_quoted}@{db_host}:{db_port}/{db_name}"

# Connection retry configuration
MAX_RETRIES = int(os.getenv("DB_CONNECT_RETRIES", "10"))
RETRY_DELAY = int(os.getenv("DB_CONNECT_DELAY", "3"))  # seconds
CONNECT_TIMEOUT = int(os.getenv("DB_CONNECT_TIMEOUT", "5"))  # seconds for connect attempt

def wait_for_db(uri: str, max_retries: int = 10, delay: int = 3, timeout: int = 5) -> bool:
    """
    Try connecting to the database repeatedly until success or retries exhausted.
    Returns True if reachable, False otherwise.
    """
    logger.info("Testing DB connection to: %s", uri)
    attempt = 0
    while attempt < max_retries:
        attempt += 1
        try:
            # create a short-lived engine for the check
            engine = create_engine(uri, connect_args={"connect_timeout": timeout}, pool_pre_ping=True)
            with engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            logger.info("DB connection successful on attempt %d", attempt)
            return True
        except Exception as e:
            logger.exception("Unexpected error when testing DB connection: %s", e)
            time.sleep(delay)
    logger.error("Could not connect to DB after %d attempts", max_retries)
    return False

# Wait for DB to be ready (useful when connecting to remote DB that may be slow to accept connections)
db_ready = wait_for_db(DATABASE_URL, max_retries=MAX_RETRIES, delay=RETRY_DELAY, timeout=CONNECT_TIMEOUT)

# Initialize Flask + SQLAlchemy regardless of db_ready (so app can still run and return health/fail info)
app = Flask(__name__)
app.config["SQLALCHEMY_DATABASE_URI"] = DATABASE_URL
# Avoid tracking modifications to save overhead
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
# Engine options - pool_pre_ping helps with stale connections
app.config.setdefault("SQLALCHEMY_ENGINE_OPTIONS", {"pool_pre_ping": True, "pool_size": 5, "pool_recycle": 1800})

db = SQLAlchemy(app)

def convert_datetime(date: str, time: str) -> datetime:
    # 2. 日付と時間を結合して1つの文字列にする
    # 例: "2026年01月01日 12:34"
    combined_str = f"{date} {time}"

    # 3. 文字列をPythonのdatetimeオブジェクトに変換する
    # %Y: 4桁の年, %m: 0埋めの月, %d: 0埋めの日, %H: 24時間表記の時, %M: 分
    dt_obj = datetime.strptime(combined_str, "%Y年%m月%d日 %H:%M")

    # 4. MySQLのDATETIME型用のフォーマット（YYYY-MM-DD HH:MM:SS）に変換する
    # %S: 秒（元のデータに秒がない場合は 00 になります）
    mysql_datetime_str = dt_obj.strftime("%Y-%m-%d %H:%M:%S")

    return mysql_datetime_str

def create_session(uri: str, max_retries: int = 10, delay: int = 3, timeout: int = 5) -> bool:
    """
    Try connecting to the database repeatedly until success or retries exhausted.
    Returns True if reachable, False otherwise.
    """
    logger.info("Testing DB connection to: %s", uri)
    attempt = 0
    while attempt < max_retries:
        attempt += 1
        try:
            # create a short-lived engine for the check
            engine = create_engine(uri, connect_args={"connect_timeout": timeout}, pool_pre_ping=True)
            with engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            logger.info("DB connection successful on attempt %d", attempt)
            return sessionmaker(autocommit=False, autoflush=False, bind=engine)
        except Exception as e:
            logger.exception("Unexpected error when testing DB connection: %s", e)
            time.sleep(delay)
    logger.error("Could not connect to DB after %d attempts", max_retries)
    return False

# セッションを生成するためのファクトリ（sessionmaker）を作成
if db_ready:
    SessionLocal = create_session(DATABASE_URL, max_retries=MAX_RETRIES, delay=RETRY_DELAY, timeout=CONNECT_TIMEOUT)


# simple model for the sample users table
class User(db.Model):
    __tablename__ = "users"
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100))
    email = db.Column(db.String(150))
    created_at = db.Column(db.DateTime)

# 家計簿情報テーブル
class Account(db.Model):
    __tablename__ = "T_ACCOUNT"
    account_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    store_name = db.Column(db.String(100))
    purchase_datetime = db.Column(db.DateTime)
    total_amount = db.Column(db.Integer)
    entry_datetime = db.Column(db.DateTime)
    entry_user = db.Column(db.String(10))
    update_datetime = db.Column(db.DateTime, nullable=True)
    update_user = db.Column(db.String(10), nullable=True)
    


@app.route("/health")
def health():
    """
    Health endpoint: checks app and DB connectivity.
    Returns JSON with status and optional DB error message.
    """
    status = {"status": "ok", "db": "unknown"}
    try:
        # quick DB check
        with db.engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        status["db"] = "ok"
        code = 200
    except Exception as e:
        logger.warning("Health DB check failed: %s", e)
        status["db"] = "error"
        status["db_error"] = str(e)
        code = 503
    return jsonify(status), code

@app.route("/api/users")
def list_users():
    """
    Return users from the database. If DB is not reachable, return error.
    """
    try:
        users = User.query.all()
        result = [{"id": u.id, "name": u.name, "email": u.email} for u in users]
        return jsonify(result)
    except Exception as e:
        logger.exception("Failed to list users: %s", e)
        return jsonify({"error": "DB error", "details": str(e)}), 500


# 画面1: 画像を受け取り、Azure Document Intelligenceで解析するAPI
@app.route('/api/analyze-receipt', methods=['POST'])
def analyze_receipt():
    azure_endpoint = os.getenv("AZURE_DOC_INTEL_ENDPOINT", "azureendpoint")
    azure_key = os.getenv("AZURE_DOC_INTEL_KEY", "azurekey")
    if azure_endpoint and azure_key:
        azure_client = DocumentIntelligenceClient(azure_endpoint, AzureKeyCredential(azure_key))
        # print("✅ Azure クライアント初期化成功")
    else:
        print("⚠️ Azureの認証情報が設定されていません")

    if 'file' not in request.files:
        return jsonify({'error': 'ファイルがありません'}), 400

    # 複数のレシートが解析された場合を考慮し、リストに格納します
    all_receipts_data = []

    file = request.files['file']
    
    # ---------------------------------------------------------
    # 【実装ポイント】
    # ここで file.read() などで画像データを取得し、
    # Azure Document Intelligence SDK を使用して解析を行います。
    # ---------------------------------------------------------
    try:
        image_bytes = file.read()
        # 分析ジョブの開始
        # SDK v1.0.0 以降の仕様に合わせて引数 'body' を使用します
        poller = azure_client.begin_analyze_document(
            "prebuilt-receipt",
            body=image_bytes,  # analyze_request から body に変更
            content_type="application/octet-stream"
        )

        # 取得したドキュメント（レシート）ごとに処理
        for receipt in poller.result().documents:
            print("=== レシート解析結果 ===")
            # 1件のレシートデータを格納する辞書を作成
            receipt_data = {
                "merchant_name": None,
                "date": None,
                "time": None,
                "items": [],
                "total_amount": None
            }
            
            # 1. 購入店舗 (MerchantName)
            merchant_name = receipt.fields.get("MerchantName")
            if merchant_name:
                # 新SDKでは .value ではなく .content を使用します
                # print(f"店舗名: {merchant_name.content}")
                receipt_data["merchant_name"] = merchant_name.content

            # 2. 日時 (TransactionDate / TransactionTime)
            transaction_date = receipt.fields.get("TransactionDate")
            transaction_time = receipt.fields.get("TransactionTime")
            if transaction_date:
                date_str = transaction_date.content
                time_str = transaction_time.content if transaction_time else ""
                # print(f"日時: {date_str} {time_str}")
                # 日付と時間を結合して格納
                # receipt_data["datetime"] = f"{date_str} {time_str}".strip()
                receipt_data["date"] = transaction_date.content
                receipt_data["time"] = time_str

            # 3. 商品名と金額 (Items)
            items = receipt.fields.get("Items")
            
            # 型の確認が必要な場合は items.type を使用します (例: if items.type == "array":)
            if items and items.value_array:
                print("商品一覧:")
                for idx, item in enumerate(items.value_array):
                    # 新SDKでは、辞書型のデータは value_object に入っています
                    item_fields = item.value_object
                    
                    if item_fields:
                        item_desc = item_fields.get("Description")
                        item_price = item_fields.get("TotalPrice")
                        
                        # 各項目の印字テキストを .content で取得
                        name = item_desc.content if item_desc else "不明"
                        price = item_price.content if item_price else "不明"
                        
                        # print(f"  {idx+1}. {name}: {price}")
                        # 個別の商品データを辞書としてリストに追加
                        receipt_data["items"].append({
                            "name": item_desc.content if item_desc else None,
                            "price": item_price.content if item_price else None
                        })

            # 4. 合計金額 (Total)
            total = receipt.fields.get("Total")
            if total:
                # print(f"合計金額: {total.content}")
                receipt_data["total_amount"] = total.content
            
            all_receipts_data.append(receipt_data)
            print(receipt_data)
            
    except Exception as e:
        return jsonify({'error': '読み取り処理でエラーが発生しました'}), 400
    
    return jsonify(receipt_data), 200

# 画面2: 編集後のデータを受け取り、DBに登録するAPI
@app.route('/api/receipts', methods=['POST'])
def save_receipt():
    data = request.json
    
    # ---------------------------------------------------------
    # 【実装ポイント】
    # ここで SQLAlchemy などのORMを利用して、MariaDBへINSERTします。
    # ---------------------------------------------------------
    # セッションのインスタンスを作成
    session = SessionLocal()
    try:
        # 購入日時の文字列を取得
        param_datetime = convert_datetime(data.get("date"), data.get("time"))

        # 合計金額の文字列を取得（キーが存在しない場合は "0" をデフォルトにする）
        amount_str = data.get("total_amount", "0")
        # replace()を使って「\」「¥」「,」を空文字に置換（削除）する
        cleaned_str = amount_str.replace("\\", "").replace("¥", "").replace(",", "")
        # 整数（int）に変換
        param_total_amount = int(cleaned_str)

        # 挿入したいデータのインスタンスを作成
        new_account = Account(store_name=data.get("storeName"), purchase_datetime=param_datetime, total_amount=param_total_amount,entry_datetime=datetime.now(),entry_user="test")
        
        # セッションに追加
        session.add(new_account)
        
        # コミットして確定
        session.commit()
        print(f"データが正常に挿入されました: {new_account}")
        
    except Exception as e:
        # エラー時はロールバック
        session.rollback()
        print(f"エラーが発生しました: {e}")
        
    finally:
        # 1.x系では最後に明示的にセッションを閉じるのが一般的です
        session.close()

    print(f"DB保存処理を実行しました: {data}")
    
    return jsonify({"status": "success", "message": "登録が完了しました"}), 201

if __name__ == "__main__":
    if not db_ready:
        logger.warning("Starting app although DB was unreachable during startup. Use /health to check DB status.")
    app.run(host="0.0.0.0", port=5000, debug=False)