## Executor

- 线程池 `ThreadPoolExecutor` 用于处理 I/O 密集型任务
- 进程池 `ProcessPoolExecutor` 用于处理 CPU 密集型任务
- 让同步代码不阻塞 asyncio 的事件循环
- Executor 的大致执行流程
  ```
  1. submit() 方法提交的任务，会被封装成 WorkItem 对象，然后进入调度队列（阻塞）
  2. 通过死循环不断从调度队列中去任务，然后把任务丢给空闲的线程/进程执行
  while True:
    work_item = queue.get(block=True)
    work_item.run()
  ```
- `Executor` 不应该直接使用，应该使用它的子类 `ThreadPoolExecutor` 或者 `ProcessPoolExecutor`

### submit(fn, *args, **kwargs)

```python
# thread.py:146
# 提交任务到执行器中，等待被调度执行


def submit(*args, **kwargs):
    # 省略大段大段的参数检查
    pass

    # submit 是一个带锁的操作
    with self._shutdown_lock:
        # 判断执行器是否正常
        if self._broken:
            raise BrokenThreadPool(self._broken)

        # 如果已经调用了 shutdown() 关闭 Executor，就不能再调用 submit() 提交任务
        if self._shutdown:
            raise RuntimeError('cannot schedule new futures after shutdown')
        if _shutdown:
            raise RuntimeError('cannot schedule new futures after '
                               'interpreter shutdown')

        # 为任务创建一个对应的 Future 对象
        f = _base.Future()
        # 将 Future 对象、任务、任务的参数封装成一个 _WorkItem 对象 
        w = _WorkItem(f, fn, args, kwargs)

        # self._work_queue 是 queue.SimpleQueue 类型，线程安全的先进先出队列
        # 将 _WorkItem 对象入队，等待被调度执行
        self._work_queue.put(w)
        # 一些额外的操作
        self._adjust_thread_count()
        # 返回 Future 对象
        return f
```

### shutdown(wait=True, *, cancel_futures=False)

```python
# _base.py:606
# 关闭执行器，关闭后不能再提交任务

def shutdown(self, wait=True):
    # shutdown 是一个带锁的操作
    with self._shutdown_lock:
        # 标记关闭
        self._shutdown = True
        self._work_queue.put(None)
    # 如果 wait 为 True，等待所有工作线程执行完任务
    if wait:
        for t in self._threads:
            t.join()
```

### ThreadPoolExecutor 使用例子

```python
import concurrent.futures
import requests
import os

if __name__ == "__main__":
    urls = [
        "http://qq.com",
        "http://163.com"
    ]


    def humble_download(url):
        headers = {
            "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36"}
        try:
            data = requests.get(url, headers=headers).content
            return data
        except:
            raise
            return "网络异常"


    executor = concurrent.futures.ThreadPoolExecutor(
        max_workers=os.cpu_count() * 2)
    futures = [executor.submit(humble_download, url) for url in urls]

    # 等到 Future 结果，先完成的先返回
    for future in concurrent.futures.as_completed(futures):
        print(future.result())
```

### Executor 执行流程

> 以 ThreadPoolExecutor 为例

```python
import concurrent.futures

if __name__ == "__main__":
    def foo():
        print("Hello Pool")


    pool = concurrent.futures.ThreadPoolExecutor(max_workers=2)
    future = pool.submit(foo)
    future.result()
```

```
_invoke_callbacks, _base.py:322
set_result, _base.py:524
run, thread.py:63
_worker, thread.py:80
run, threading.py:870 # 执行线程的相关步骤
_bootstrap_inner, threading.py:926  # 执行线程的相关步骤
_bootstrap, threading.py:890  # 执行线程的相关步骤
```

```python
def _worker(executor_reference, work_queue, initializer, initargs):
    # 忽略相关检查
    pass

    try:
        # 每个工作线程进入事件循环
        while True:
            # work_queue 是核心数据结构，类型是 queue.SimpleQueue，存放的是 _WorkItem 对象
            work_item = work_queue.get(block=True)
            if work_item is not None:
                # 执行 _WorkItem 对象的 run() 方法
                work_item.run()
                # Delete references to object. See issue16284
                del work_item
                continue
            executor = executor_reference()
            # 忽律退出事件循环的一些处理
            pass
            del executor
    except BaseException:
        _base.LOGGER.critical('Exception in worker', exc_info=True)
```

```python
def run(self):
    if not self.future.set_running_or_notify_cancel():
        return

    try:
        # 这里执行的才是 `submit()` 提交的任务
        result = self.fn(*self.args, **self.kwargs)
    except BaseException as exc:
        self.future.set_exception(exc)
        # Break a reference cycle with the exception 'exc'
        self = None
    else:
        # 调用 Future 的 set_result()，从而唤醒阻塞的线程、执行回调函数 
        self.future.set_result(result)
```

```python
def set_result(self, result):
    # set_result() 是一个带锁操作
    with self._condition:
        # 相关设置
        self._result = result
        self._state = FINISHED
        for waiter in self._waiters:
            waiter.add_result(self)
        # 通知调用了 result() 阻塞等待的线程
        self._condition.notify_all()
    # 直接调用绑定的回调函数
    self._invoke_callbacks()
```

```python
def _invoke_callbacks(self):
    for callback in self._done_callbacks:
        try:
            # 调用回调函数
            callback(self)
        except Exception:
            LOGGER.exception('exception calling callback for %r', self)
```
