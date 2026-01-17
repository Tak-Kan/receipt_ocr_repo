# backend/Dockerfile
FROM python:3.11-slim

# 作業ディレクトリ
WORKDIR /app

# システム依存パッケージ（必要なら）
RUN apt-get update && apt-get install -y build-essential libmariadb-dev-compat libmariadb-dev && rm -rf /var/lib/apt/lists/*
# 依存関係
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 5000

# 環境変数のデフォルト（docker-compose で上書き）
ENV FLASK_APP=app.py
ENV FLASK_ENV=production

CMD ["gunicorn", "--bind", "0.0.0.0:5000", "app:app"]