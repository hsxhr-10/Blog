from flask import Flask, after_this_request

app = Flask(__name__)


@app.before_request
def before_handle():
    print("---> before handle request")


@app.route("/hello")
def handle():
    @after_this_request
    def after_handle(response):
        response.headers["custom-X"] = "X"
        print("---> after handle request")
        return response

    print("---> handle request")
    return "ok"


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=12345, debug=True)
