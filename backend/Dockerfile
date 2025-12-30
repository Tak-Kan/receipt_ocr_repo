FROM python:3.11-slim

# 作業ディレクトリ
WORKDIR /app

# 依存関係
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# アプリ本体
COPY app ./app
COPY run.py .

# ポート公開
EXPOSE 5000

# 起動コマンド
CMD ["python", "run.py"]
