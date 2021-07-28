from flask import Flask, request

from form import UserForm
from response import generate_response, ResponseStatus

app = Flask(__name__)


@app.route("/hello", methods=["POST"])
def hello_world():
    form = UserForm(request.form)
    if form.validate():
        user = {
            "username": form.username.data,
            "email": form.email.data,
            "password": form.password.data,
            "confirm": form.confirm.data,
            "is_vip": form.is_vip.data
        }
        return generate_response(ResponseStatus.StatusCode.SUCCESS.value, ResponseStatus.StatusMessage.SUCCESS.value, user)
    else:
        return generate_response(ResponseStatus.StatusCode.ARGS_VALIDATE_ERROR.value, ResponseStatus.StatusMessage.ARGS_VALIDATE_ERROR.value, [])


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=12345, debug=True)
