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
from urllib.parse import quote_plus

from flask import Flask, jsonify
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import create_engine, text
from sqlalchemy.exc import OperationalError

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
        except OperationalError as e:
            logger.warning("DB connect attempt %d/%d failed: %s", attempt, max_retries, e)
            time.sleep(delay)
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

# simple model for the sample users table
class User(db.Model):
    __tablename__ = "users"
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100))
    email = db.Column(db.String(150))
    created_at = db.Column(db.DateTime)

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

if __name__ == "__main__":
    if not db_ready:
        logger.warning("Starting app although DB was unreachable during startup. Use /health to check DB status.")
    app.run(host="0.0.0.0", port=5000, debug=False)