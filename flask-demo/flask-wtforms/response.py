import json
from enum import Enum

from flask import Response


class ResponseStatus:
    class StatusCode(Enum):
        SUCCESS = 0
        ARGS_VALIDATE_ERROR = 400000

    class StatusMessage(Enum):
        SUCCESS = "success"
        ARGS_VALIDATE_ERROR = "arguments validate error"


def generate_response(code, message, data=None, headers=None):
    _result = {"code": code, "message": message, "data": data}
    result = json.dumps(_result, ensure_ascii=False)
    response = Response(result)

    _headers = {"Content-Type": "application/json; charset=utf-8"}
    if headers is not None:
        _headers.update(headers)
    for k, v in _headers.items():
        response.headers[k] = v

    return response
