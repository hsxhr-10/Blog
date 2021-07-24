# Executor 笔记

1. [Executor](https://github.com/zongzhenh/Blog/blob/master/Python/%E5%B9%B6%E5%8F%91/Executor%E7%AC%94%E8%AE%B0.md#executor)
2. [ThreadPoolExecutor 的使用](https://github.com/zongzhenh/Blog/blob/master/Python/%E5%B9%B6%E5%8F%91/Executor%E7%AC%94%E8%AE%B0.md#threadpoolexecutor-%E7%9A%84%E4%BD%BF%E7%94%A8)
3. [Future 对象](https://github.com/zongzhenh/Blog/blob/master/Python/%E5%B9%B6%E5%8F%91/Executor%E7%AC%94%E8%AE%B0.md#future-%E5%AF%B9%E8%B1%A1)
4. [Executor 的调度流程](https://github.com/zongzhenh/Blog/blob/master/Python/%E5%B9%B6%E5%8F%91/Executor%E7%AC%94%E8%AE%B0.md#executor-%E7%9A%84%E8%B0%83%E5%BA%A6%E6%B5%81%E7%A8%8B)
5. [异常](https://github.com/zongzhenh/Blog/blob/master/Python/%E5%B9%B6%E5%8F%91/Executor%E7%AC%94%E8%AE%B0.md#%E5%BC%82%E5%B8%B8)

Executor 提供了池、Future、调度等功能，可以用于并发、异步等处理场景。具体有线程池执行器 ThreadPoolExecutor 和进程池执行器 ProcessPoolExecutor 两个子类，
ThreadPoolExecutor 用于 IO 密集型任务，ProcessPoolExecutor 用于 CPU 密集型任务。
ThreadPoolExecutor 和 ProcessPoolExecutor 的用法差不多，下面以 Executor 和 ThreadPoolExecutor 为主

> 这个类支持 `with` 语句。
> 
> 听说这个库是直接抄 Java 的 Executor 😂

## Executor

Executor 不应该直接使用，应该使用它的子类 ThreadPoolExecutor 或者 ProcessPoolExecutor

### submit(fn, *args, **kwargs)

提交任务到执行器中，等待被调度执行

```python
# thread.py:146

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

关闭执行器，关闭后不能再提交任务

```python
# _base.py:606

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

## ThreadPoolExecutor 的使用

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
        headers = {"User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36"}
        try:
            data = requests.get(url, headers=headers).content
            return data
        except:
            raise
            return "网络异常"

    executor = concurrent.futures.ThreadPoolExecutor(max_workers=os.cpu_count()*2)
    futures = [executor.submit(humble_download, url) for url in urls]
    
    # 等到 Future 结果，先完成的先返回
    for future in concurrent.futures.as_completed(futures):
        print(future.result())
```

## Future 对象

Executor Future 对象提供的操作和 [asyncio Future 对象](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E7%AC%94%E8%AE%B0.md#3-%E4%BA%8B%E4%BB%B6%E5%BE%AA%E7%8E%AF%E5%92%8C-future)
大体上差不多，但是也有一些区别：

- Executor Future 的 `result()` 带超时功能，而且当 Future 未完成调用 `result()` 不会立即抛出异常
- Executor Future 的 `set_result()` 会直接触发所绑定的回调函数。asyncio Future 不会直接执行，而是把回调函数加入 `self._ready` 调度队列中，等待被调用
    ```python
    # _base.py:513

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
    
  
    # _base.py:321
  
    def _invoke_callbacks(self):
        for callback in self._done_callbacks:
            try:
                # 调用回调函数
                callback(self)
            except Exception:
                LOGGER.exception('exception calling callback for %r', self)
    ```

## Executor 的调度流程

> 以 ThreadPoolExecutor 为例

1. 写个 demo，打上断点，在调试模式下可以看到调用栈

```python
import concurrent.futures


if __name__ == "__main__":
    def foo():
        print("Hello Pool")

    pool = concurrent.futures.ThreadPoolExecutor(max_workers=2)
    future = pool.submit(foo)
    future.result()
```

```BASH
_invoke_callbacks, _base.py:322
set_result, _base.py:524
run, thread.py:63
_worker, thread.py:80
run, threading.py:870 # 执行线程的相关步骤
_bootstrap_inner, threading.py:926  # 执行线程的相关步骤
_bootstrap, threading.py:890  # 执行线程的相关步骤
```

2. thread.py 的 _worker() 函数

线程池里的工作线程对应的 `target` 并不是 `submit()` 提交的任务，而是 `_worker()` 函数。`_worker()` 函数会进入事件循环，
不断从调度队列 `work_queue` 中获取 _`WorkItem` 对象（阻塞获取），并执行它的 `run()` 方法

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

3. thread.py 的 run() 方法

先执行提交过来的任务，然后调用对应 Future 对象的 `set_result()` 方法设置运行结果

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

4. _base.py 的 set_result() 方法

先设置结果，然后唤醒外部阻塞等待结果的线程，最后调用回调函数

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

5. _base.py 的 _invoke_callbacks() 方法

```python
def _invoke_callbacks(self):
    for callback in self._done_callbacks:
        try:
            # 调用回调函数
            callback(self)
        except Exception:
            LOGGER.exception('exception calling callback for %r', self)
```

## 异常

[这里](https://docs.python.org/3/library/concurrent.futures.html#exception-classes)
