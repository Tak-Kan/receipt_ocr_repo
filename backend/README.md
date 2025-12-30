# dj_test_2025

## Docker ビルド & 起動
1. イメージビルド
```bash
docker build -t backend_docker .
```

2. コンテナ起動
```bash
docker run -p 5000:5000 backend_docker
```

3. 動作確認<br>
ブラウザで[http://localhost:5000/hello](http://localhost:5000/hello)へアクセス

