# WSGI 笔记

1. [简单实现](https://github.com/zongzhenh/Blog/blob/master/Python-Web/WSGI%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0.md#%E7%AE%80%E5%8D%95%E5%AE%9E%E7%8E%B0)
2. [Flask 中的 WSGI](https://github.com/zongzhenh/Blog/blob/master/Python-Web/WSGI%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0.md#flask-%E4%B8%AD%E7%9A%84-wsgi)

WSGI 是 Python Web 的一种协议和规范，分为应用端（Flask、Tornado、Django 等 Web 框架）和服务端（Gunicorn、uWSGI 等应用服务器）。
WSGI 解耦了应用端和服务端，让它们可以灵活搭配使用，比如 Flask 既可以运行在 Gunicorn 上，也可以运行在 uWSGI 上。其他编程语言也有类似的协议，像 Java 的 Servlet

> uwsgi 是另一种 Python Web 协议，而 uWSGI 是既实现了 WSGI 协议，又实现了 uwsgi 协议的应用服务器

## 简单实现

参考 PEP-3333 的实现

### WSGI 应用端

```python
class AppClass:
 
    def __call__(self, environ, start_response):
        status = '200 OK'
        response_headers = [('Content-type', 'text/plain')]
        start_response(status, response_headers)
        return [b"Hello WSGI"]
```

应用端需要满足三个要求：

- 是一个可调用对象，比如函数、实现了 `__call__()` 方法的类
- 接受两个参数，environ（一个字典，包含 WSGI 环境信息） 和 start_response（该函数由服务端实现，并传给应用端，主要功能是组装 HTTP headers、返回 `write()` 方法）
- 返回一个可迭代对象

### WSGI 服务端

```python
import os, sys


enc, esc = sys.getfilesystemencoding(), 'surrogateescape'


def unicode_to_wsgi(u):
    return u.encode(enc, esc).decode('iso-8859-1')


def wsgi_to_bytes(s):
    return s.encode('iso-8859-1')


def run_wsgi(application):
    # environ 字典包含 CGI 变量、WSGI 变量，可能还有系统环境变量、Web 服务器的相关变量
    # 完整的字典字段参考这里：https://www.python.org/dev/peps/pep-3333/#environ-variables
    # 注意：这里简化了 environ，实际中大多数字段需要从 HTTP 中解析出来
    environ = {k: unicode_to_wsgi(v) for k, v in os.environ.items()}
    environ['wsgi.input'] = sys.stdin.buffer
    environ['wsgi.errors'] = sys.stderr
    environ['wsgi.version'] = (1, 0)
    environ['wsgi.multithread'] = False
    environ['wsgi.multiprocess'] = True
    environ['wsgi.run_once'] = True

    if environ.get('HTTPS', 'off') in ('on', '1'):
        environ['wsgi.url_scheme'] = 'https'
    else:
        environ['wsgi.url_scheme'] = 'http'

    # 存放 HTTP headers
    headers_set = []
    headers_sent = []

    def write(data):
        """发送 HTTP headers 和数据"""
        out = sys.stdout.buffer

        # 确保必须先设置 headers，再发送数据
        if not headers_set:
            raise AssertionError("write() before start_response()")

        # 发送 headers
        elif not headers_sent:
            status, response_headers = headers_sent[:] = headers_set
            out.write(wsgi_to_bytes('Status: %s\r\n' % status))
            for header in response_headers:
                out.write(wsgi_to_bytes('%s: %s\r\n' % header))
            out.write(wsgi_to_bytes('\r\n'))

        # 发送数据
        out.write(data)
        out.flush()

    def start_response(status, response_headers, exc_info=None):
        """
        :param status: HTTP 响应状态
        :param response_headers: HTTP headers 信息
        :param exc_info: 异常信息
        """
        # 异常处理
        if exc_info:
            try:
                if headers_sent:
                    raise exc_info[1].with_traceback(exc_info[2])
            finally:
                exc_info = None  # avoid dangling circular ref
        elif headers_set:
            raise AssertionError("Headers already set!")
        
        # 封装 status 和 headers
        headers_set[:] = [status, response_headers]

        return write
    
    # 调用应用端
    result = application(environ, start_response)
    try:
        # 发送响应数据
        for data in result:
            if data:  # don't send headers until body appears
                write(data)
        # 只需要发送 headers
        if not headers_sent:
            write('')
    finally:
        if hasattr(result, 'close'):
            result.close()
```

服务端需要满足五个要求：

- 解析出 environ
- 定义用于发送数据的 `write()` 函数
- 定义用于设置 status、headers 的 `start_response()` 函数
- 调用应用端并获取结果
- 将结果返回给请求方

执行 `run_wsgi(AppClass())` 输出：

```BASH
Status: 200 OK
Content-type: text/plain

Hello WSGI
```

### 交互流程

1. 服务端实现接收到请求信息，解析出 environ
2. 服务端调用应用端，传入 environ 字典和 `start_response()` 函数
3. 应用端根据 environ 的信息执行相应的业务逻辑；调用 `start_response()` 函数，将 status、headers 返回给服务端；返回可迭代对象
4. 服务端将接收到的 status、headers、数据（可迭代对象）封装成 HTTP 响应，返回给请求方

## Flask 中的 WSGI

1. 写个 demo，打上断点，在调试模式下可以看到调用栈

```python
from flask import Flask

app = Flask(__name__)


@app.route('/hello')
def hello_world():
    return 'Hello, World!'


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=12345)
```

```BASH
write, serving.py:265
execute, serving.py:315
run_wsgi, serving.py:323
handle_one_request, serving.py:379  # 从这个地方开始，下面的堆栈信息是请求转发过来的路径，不是这次分析的重点
handle, server.py:426
handle, serving.py:345
__init__, socketserver.py:720
finish_request, socketserver.py:360
process_request_thread, socketserver.py:650
run, threading.py:870
_bootstrap_inner, threading.py:926
_bootstrap, threading.py:890
```

2. serving.py 的 handle_one_request() 方法

serving.py 是 werkzeug 的模块

```python
def handle_one_request(self):
    self.raw_requestline = self.rfile.readline()
    if not self.raw_requestline:
        self.close_connection = 1
    # 判断是否有效的请求
    elif self.parse_request():
        # 重点是这里，调用 run_wsgi() 方法
        return self.run_wsgi()
```

3. serving.py 的 run_wsgi() 方法

大体上跟 PEP-3333 的例子差不多

```python
def run_wsgi(self):
    # 忽略一些检查
    # ...
    
    # 1. 解析 environ
    self.environ = environ = self.make_environ()
    # 存放 status, headers 的容器, 给 start_response() 使用
    headers_set = []
    headers_sent = []
    
    # 2. 定义用于发送数据的 write() 函数
    def write(data):
        assert headers_set, "write() before start_response"
        if not headers_sent:
            status, response_headers = headers_sent[:] = headers_set
            try:
                code, msg = status.split(None, 1)
            except ValueError:
                code, msg = status, ""
            code = int(code)
            # 发送 HTTP 响应状态行
            self.send_response(code, msg)
            # 忽略大段大段的 headers 处理 
            # ...
            # 发送 HTTP headers
            self.end_headers()

        assert isinstance(data, bytes), "applications must write bytes"
        # 发送数据
        if data:
            self.wfile.write(data)
        self.wfile.flush()
    
    # 3. 定义用于设置 status、headers 的 start_response() 函数
    def start_response(status, response_headers, exc_info=None):
        if exc_info:
            try:
                if headers_sent:
                    reraise(*exc_info)
            finally:
                exc_info = None
        elif headers_set:
            raise AssertionError("Headers already set")
        headers_set[:] = [status, response_headers]
        return write

    def execute(app):
        # 4. 调用应用端获取结果
        application_iter = app(environ, start_response)
        try:
            # 5. 调用 write() 将结果返回给请求方
            for data in application_iter:
                write(data)
            if not headers_sent:
                write(b"")
        finally:
            if hasattr(application_iter, "close"):
                application_iter.close()

    try:
        # 入口
        execute(self.server.app)
    except (_ConnectionError, socket.timeout) as e:
        self.connection_dropped(e, environ)
    except Exception:
        # 忽略大段大段的异常处理
        # ...
```

## 参考

- [PEP-3333](https://www.python.org/dev/peps/pep-3333/#environ-variables)
