## requests

- request + gevent -> 异步 HTTP 客户端
- 异步 HTTP 客户端：https://github.com/encode/httpx

Session 对象：

`Session()` 会使用底层库 urllib3 的连接池，对 TCP 连接进行复用，对于向同一个目标主机进行多次请求的场景会有性能提升。版本比较新的
requests（比如 2.25.1） 默认使用 `Session()`。

```python
import requests

s = requests.Session()
s.get('https://httpbin.org/cookies')
```

常用配置：

```python
import requests

# 连接池大小
requests.adapters.DEFAULT_POOLSIZE = 30
# 重试次数
requests.adapters.DEFAULT_RETRIES = 3

s = requests.Session()

r = s.get("http://www.example.org")
print(r.status_code)
```

常见操作：

```
import requests
import shutil

# GET
r = requests.get(<url>, params=<dict>, headers=<headers>, timeout=<timeout>)

# POST
r = requests.post(<url>, data=<dict>, headers=<headers>, timeout=<timeout>)
r = requests.post(<url>, json=<dict>, headers=<headers>, timeout=<timeout>)

# 上传
files = [
    ("image", open("test.png", "rb")),
]
r = requests.post(<url>, data=<dict>, files=files, headers=<headers>, timeout=<timeout>)

# 下载
r = requests.get(url=<url>, stream=True, headers=<headers>, timeout=<timeout>)

with open('./test.png', 'wb') as out_file:
    shutil.copyfileobj(response.raw, out_file)
    
# 客户端证书验证
r = requests.get(<url>, cert=("/path/client.cert", "/path/client.key"), timeout=<timeout>)

# SSL 证书验证
r = requests.get(<url>, verify="/path/to/certfile", timeout=<timeout>)
r = requests.get(<url>, verify=False, timeout=<timeout>)
```
