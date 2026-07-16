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
import re
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

# Initialize Flask + SQLAlchemy regardless of db_ready (so app can still run and return health/fail info)
app = Flask(__name__)
# Avoid tracking modifications to save overhead
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

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
        # 1件のレシートデータを格納する辞書を作成
        receipt_data = {
            "merchant_name": None,
            "date": None,
            "time": None,
            "invoice_number": None,
            "items": [],
            "total_amount": None
        }

        # 取得したドキュメント（レシート）ごとに処理
        for receipt in poller.result().documents:
            print("=== レシート解析結果 ===")
            
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
            
        # レシート全体から読み取られた生のテキストデータを取得
        full_text = poller.result().content

        # 正規表現で「T」または「t」から始まり、13桁の数字が続くパターンを検索
        # （OCRの特性上、Tの後に意図せず半角スペースが入るケースも考慮しています）
        match = re.search(r'[Tt]\s*\d{13}', full_text)

        if match:
            # スペースなどの不要な文字を除去して整形
            receipt_data["invoice_number"] = match.group(0).replace(" ", "").upper()
        else:
            print("全文の中にも T+13桁 の番号は見つかりませんでした。")
            
            # 原因調査のため、読み取られた全文を出力して目視確認します
            # print("\n--- 読み取られた全文 ---")
            # print(full_text)

        print(receipt_data)
            
    except Exception as e:
        return jsonify({'error': '読み取り処理でエラーが発生しました'}), 400
    
    return jsonify(receipt_data), 200


if __name__ == "__main__":
    if not db_ready:
        logger.warning("Starting app although DB was unreachable during startup. Use /health to check DB status.")
    app.run(host="0.0.0.0", port=5000, debug=False)