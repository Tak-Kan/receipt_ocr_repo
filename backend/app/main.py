from flask import Flask

def create_app():
    app = Flask(__name__)

    from app.routes.hello import hello_bp
    app.register_blueprint(hello_bp)

    return app
