# dj_test_2025
1. 起動手順
```bash
docker-compose up --build
```

2. ブラウザで確認

Spring フロント： [http://localhost:8080](http://localhost:8080)
→ Flask から取得したメッセージが表示される

Flask API 単体： [http://localhost:5000/api/hello](http://localhost:5000/api/hello)