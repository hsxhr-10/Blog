from flask import Flask

from logger import ConcurrentSafeLogger, LoggerType, LoggerLevel

app = Flask(__name__)

PATH_LOG = "./test.log"
logger = ConcurrentSafeLogger(name=LoggerType.CONCURRENT_SAFE_LOGGER.value, level=LoggerLevel.INFO.value, log_path=PATH_LOG).get_logger()


@app.route("/hello")
def handle():
    logger.info("这是一条测试日志")
    return "ok"


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=12345, debug=True)
