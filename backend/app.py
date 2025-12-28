from flask import Flask, jsonify
import datetime

app = Flask(__name__)

@app.route('/api/data', methods=['GET'])
def get_data():
    # Java側に送るテストデータ
    payload = {
        "status": "success",
        "message": "Hello from Python Backend!",
        "server_time": datetime.datetime.now().isoformat(),
        "items": [1, 2, 3, 4, 5]
    }
    return jsonify(payload)

if __name__ == '__main__':
    # 0.0.0.0で起動することでコンテナ外（Java側）からの接続を許可する
    app.run(host='0.0.0.0', port=5000)