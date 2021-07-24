# asyncio 笔记

1. [事件循环](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E7%AC%94%E8%AE%B0.md#%E4%BA%8B%E4%BB%B6%E5%BE%AA%E7%8E%AF)
2. [可调度对象](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E7%AC%94%E8%AE%B0.md#%E5%8F%AF%E8%B0%83%E5%BA%A6%E5%AF%B9%E8%B1%A1)
3. [await 语句](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E7%AC%94%E8%AE%B0.md#await-%E8%AF%AD%E5%8F%A5)
4. [Streams](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E7%AC%94%E8%AE%B0.md#streams)
5. [Queue](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E7%AC%94%E8%AE%B0.md#queue)
6. [Subprocesses](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E7%AC%94%E8%AE%B0.md#subprocesses)
7. [协程同步](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E7%AC%94%E8%AE%B0.md#%E5%8D%8F%E7%A8%8B%E5%90%8C%E6%AD%A5)
8. [异常](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E7%AC%94%E8%AE%B0.md#%E5%BC%82%E5%B8%B8)
9. [其他模块级别函数](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E7%AC%94%E8%AE%B0.md#%E5%85%B6%E4%BB%96%E6%A8%A1%E5%9D%97%E7%BA%A7%E5%88%AB%E5%87%BD%E6%95%B0)

很多 Python 异步 IO 框架，如果根据标准的 IO 模型来看，应该叫 IO 多路复用。asyncio 其实也是，
对于 IO 部分，asyncio 是基于 [selector](https://github.com/python/cpython/blob/3.9/Lib/asyncio/selector_events.py) 模块，
而 selector 基于 select 模块，select 基于操作系统提供的 IO 多路复用机制，比如 Linux 的 epoll，macOS 的 kqueue。但是 asyncio 实现的事件循环确实能实现异步的效果。

asyncio 的特点和主流的异步框架（Tornado 等）差不多：

- 擅长处理 IO 密集型任务，相比线程的昂贵（创建、销毁、上下文切换），asyncio 里的协程要轻量级很多，可以同时存在大量的协程，因此可以较好地提升应用的吞吐量，
  但是应用的响应时间还是要看单个协程的处理时长，跟异步与否无关
- 事件循环不能被阻塞，也就是不能存在阻塞代码，无论是自己写的、标准库的、还是第三方库的代码。这就需要搭配线程池、进程池等工具，或者要求生态要好，不然连个异步的数据库驱动都没有，也挺麻烦 😂
- 不擅长 CPU 密集型任务，可以结合进程池、Celery 等工具缓解这个问题

> asyncio 需要 Python3.5+，最好是 Python3.7+，功能会多一些，少量新功能需要 Python3.9。另外，asyncio 的接口存在不向后兼容的情况，
> 比如 ["Deprecated since version 3.8, will be removed in version 3.10: The loop parameter."](https://docs.python.org/3/library/asyncio-task.html#asyncio.sleep) 这类。

## 事件循环

### IO 多路复用

asyncio 的事件循环包含着 IO 多路复用，专门用来处理 IO 事件，多路复用本身也有一个事件循环，一般基于多路复用的代码长这样：

```python
# 回调函数映射表
callbacks = {}

while True:
    event_list = epoll.wait(timeout)
    for fd, event in event_list:
        if event == 连接就绪:
            # 为 fd 注册读写事件
            # 注册回调函数
            ...
        elif event == 读就绪:
            # 执行对应回调函数
            ...
        elif event == 写就绪:
            # 执行对应回调函数
            ...
        elif event == 中断事件:
            # 关闭连接, 取消注册事件, 回收相关资源
            ...
        else:
            # 默认操作
            ...
```

### 事件循环启动流程

1. 写个 demo 打上断点，在调试模式下可以看到调用栈

```python
import asyncio


async def sleep(second):
    await asyncio.sleep(second)
    print("hello asyncio")
    

async def main():
    await sleep(1)


if __name__ == "__main__":    
    asyncio.run(main())
```

```BASH
main, demo1.py:76 # 自己的代码
_run, events.py:88
_run_once, base_events.py:1786
run_forever, base_events.py:541
run_until_complete, base_events.py:574
run, runners.py:43
<module>, demo1.py:80 # 自己的代码
```

2. runners.py 的 run() 方法

```python
def run(main, *, debug=False):
    # 确保 asyncio.run() 方法不能运行在一个已存在的事件循环
    if events._get_running_loop() is not None:
        raise RuntimeError(
            "asyncio.run() cannot be called from a running event loop")
    
    # 判断传入的 main 对象是否协程类型
    if not coroutines.iscoroutine(main):
        raise ValueError("a coroutine was expected, got {!r}".format(main))
    
    # 新创建一个事件循环
    loop = events.new_event_loop()
    try:
        # 相关设置
        events.set_event_loop(loop)
        loop.set_debug(debug)
        # 执行 loop.run_until_complete(main) 方法
        return loop.run_until_complete(main)
    finally:
        # 相关收尾操作
        try:
            _cancel_all_tasks(loop)
            loop.run_until_complete(loop.shutdown_asyncgens())
        finally:
            events.set_event_loop(None)
            loop.close()
```

3. base_events.py 的 run_until_complete() 方法

```python
def run_until_complete(self, future):
    # 相关检查操作
    self._check_closed()
    self._check_runnung()
    
    # 这一块逻辑主要是将协程封装成 Task
    new_task = not futures.isfuture(future)
    future = tasks.ensure_future(future, loop=self)
    if new_task:
        # An exception is raised if the future didn't complete, so there
        # is no need to log the "destroy pending task" message
        future._log_destroy_pending = False

    future.add_done_callback(_run_until_complete_cb)
    try:
        # 启动事件循环
        self.run_forever()
    except:
        # 忽略异常处理代码
        ...
    finally:
        future.remove_done_callback(_run_until_complete_cb)
    # 相关错误处理
    if not future.done():
        raise RuntimeError('Event loop stopped before Future completed.')
    
    # 返回 Future 的结果
    return future.result()
```

4. base_events.py 的 run_forever() 方法

```python
def run_forever(self):
    # 相关检查
    self._check_closed()
    self._check_runnung()
    self._set_coroutine_origin_tracking(self._debug)
    self._thread_id = threading.get_ident()

    old_agen_hooks = sys.get_asyncgen_hooks()
    sys.set_asyncgen_hooks(firstiter=self._asyncgen_firstiter_hook,
                           finalizer=self._asyncgen_finalizer_hook)
    try:
        events._set_running_loop(self)
        # 事件循环本体
        while True:
            # 每次循环执行 _run_once() 方法
            self._run_once()
            # 外部调用 stop() 方法时会停止事件循环
            if self._stopping:
                break
    finally:
        # 相关收尾操作
        self._stopping = False
        self._thread_id = None
        events._set_running_loop(None)
        self._set_coroutine_origin_tracking(False)
        sys.set_asyncgen_hooks(*old_agen_hooks)
```

5. base_events.py 的 _run_once() 方法

```python
def _run_once(self):
    """Run one full iteration of the event loop.

    This calls all currently ready callbacks, polls for I/O,
    schedules the resulting callbacks, and finally schedules
    'call_later' callbacks.
    
    事件循环迭代一次就运行一次这个方法

    这个方法会执行所有就绪的回调函数，包括 IO 多路复用的回调、一般 future 的回调、
    call_later 的回调（比如含有 asyncio.sleep() 这种语句的函数），回调函数都会被封装成 Handle 对象
    """
    
    # 1. self._scheduled 对应的数据结构是最小二叉堆，用来存放所有的 call_later 回调，根据 time 排序
    # 2. 这一块逻辑主要是将已经取消的 call_later 回调从二叉堆中删除 
    sched_count = len(self._scheduled)
    if (sched_count > _MIN_SCHEDULED_TIMER_HANDLES and
        self._timer_cancelled_count / sched_count >
            _MIN_CANCELLED_TIMER_HANDLES_FRACTION):
        # Remove delayed calls that were cancelled if their number
        # is too high
        new_scheduled = []
        for handle in self._scheduled:
            if handle._cancelled:
                handle._scheduled = False
            else:
                new_scheduled.append(handle)

        heapq.heapify(new_scheduled)
        self._scheduled = new_scheduled
        self._timer_cancelled_count = 0
    else:
        # Remove delayed calls that were cancelled from head of queue.
        while self._scheduled and self._scheduled[0]._cancelled:
            self._timer_cancelled_count -= 1
            handle = heapq.heappop(self._scheduled)
            handle._scheduled = False
    
    # 确定 IO 多路复用的等待时间
    timeout = None
    if self._ready or self._stopping:
        timeout = 0
    elif self._scheduled:
        # Compute the desired timeout.
        when = self._scheduled[0]._when
        timeout = min(max(0, when - self.time()), MAXIMUM_SELECT_TIMEOUT)
    
    # 这一块逻辑主要是基于 IO 多路复用处理 IO 事件
    # 调试模式下
    if self._debug and timeout != 0:
        t0 = self.time()
        # 基于 selector 模块的 IO 多路复用
        event_list = self._selector.select(timeout)
        # 忽略大段大段的调试代码
        pass 
    # 非调试模式下
    else:
        # 基于 selector 模块的 IO 多路复用
        event_list = self._selector.select(timeout)
    self._process_events(event_list)

    # Handle 'later' callbacks that are ready.
    # 1. self._ready 对应的数据结构是 collections.deque，是整个事件循环的核心数据结构，
    # 任何就绪的回调函数都会放到这个队列，然后在每次事件循环中遍历队列，依次执行就绪的回调函数
    # 2. 这一块逻辑主要是将就绪的 call_later 回调放到 self._ready
    end_time = self.time() + self._clock_resolution
    while self._scheduled:
        handle = self._scheduled[0]
        if handle._when >= end_time:
            break
        handle = heapq.heappop(self._scheduled)
        handle._scheduled = False
        self._ready.append(handle)

    # This is the only place where callbacks are actually *called*.
    # All other places just add them to ready.
    # Note: We run all currently scheduled callbacks, but not any
    # callbacks scheduled by callbacks run this time around --
    # they will be run the next time (after another I/O poll).
    # Use an idiom that is thread-safe without using locks.
    # 
    # 这一块逻辑主要是遍历就绪队列 self._ready，
    # 逐个出队并执行 Handle 对象的 _run() 方法，_run() 方法内部执行的是就是回调函数
    ntodo = len(self._ready)
    # 遍历就绪队列
    for i in range(ntodo):
        # 出队
        handle = self._ready.popleft()
        if handle._cancelled:
            continue
        # 调试模式下
        if self._debug:
            try:
                self._current_handle = handle
                t0 = self.time()
                # 执行回调函数
                handle._run()
                # 忽略大段大段的调试代码
                pass
            finally:
                self._current_handle = None
        # 非调试模式下
        else:
            # 执行回调函数
            handle._run()
    handle = None  # Needed to break cycles when an exception occurs.
```

### 事件循环的调度流程

调度流程这块其实比并不好找，单步调试最多走到 events.py:88，貌似是因为协程底层是 C 实现的缘故。到网上查找发现切换相关的代码在 tasks.py 的 `__step()` 和 `__wakeup()` 方法上，
`__wakeup()` 调用 `__step()`，所以重点是 `__step()`。协程恢复中断的核心是通过生成器的 `send(None)` 来从中断的地方继续执行。

![](https://raw.githubusercontent.com/hsxhr-10/Blog/master/image/pythonio-4.png)

**⭐️ 整理一下调度流程大致如下 ⭐**

![](https://raw.githubusercontent.com/hsxhr-10/Blog/master/image/pythonio-5.png)

从这里可以看出，协程和线程的一个区别是，前者主动礼让，后者抢着执行。

### 事件循环的使用

#### (1) 获取事件循环

```python
import asyncio


async def main():
    loop1 = asyncio.get_event_loop()
    loop2 = asyncio.get_running_loop()
    loop3 = asyncio.new_event_loop()
    print(loop1 == loop2)   # True
    print(loop1 == loop2 == loop3)  # False


asyncio.run(main())
```

#### (2) 启动和停止事件循环

```python
import asyncio


async def sleep(second):
    await asyncio.sleep(second)
    print("sleep() done")
    

# 启动 case1
loop = asyncio.new_event_loop()
loop.run_until_complete(sleep(3))

# 启动 case2
asyncio.run(sleep(3))

# 停止
loop = asyncio.new_event_loop()
print(loop.is_closed())
print(loop.is_running())
loop.stop()
loop.close()
print(loop.is_closed())
```

#### (3) 事件循环和 Future

一个 Future 会对应一个事件循环，可以给 Future 绑定回调函数。当调用 `set_result()` 的时候，回调函数会被加入事件循环的 `self._ready` 队列，
等待被调度执行。

```python
import asyncio


async def main():
    loop = asyncio.get_event_loop()
    future = loop.create_future()   # 创建 Future 对象

    print(future.done())    # False
    print(future.cancelled())   # False
    print(loop == future.get_loop())    # True

    try:
        print("Get future result:", future.result())    # 抛出异常
    except asyncio.base_futures.InvalidStateError:
        print("Result is not set")

    future.add_done_callback(lambda _: print("Run future done callback!"))  # 给 Future 绑定回调函数
    future.set_result(111)  # 设置 result 的同时, 将 Future 的回调函数加入 self._ready 队列, 等待被事件循环调度执行
    try:
        print("get future result:", future.result())    # get future result: 111
    except asyncio.base_futures.InvalidStateError:
        print("Result is not set")


asyncio.run(main())
```

#### (4) 事件循环和 Task

Task 继承于 Future，提供的操作和 Future 差不多。

```python
import asyncio


async def main():
    async def say():
        print("Hi")

    loop = asyncio.get_event_loop()
    task = loop.create_task(say())  # 基于协程创建 Task
    await task


asyncio.run(main())
```

#### (5) Callback Handle

事件循环中执行的函数都会被封装成 Handle 对象，也就是说 `self._ready` 队列中保存的都是 Handle 对象。Handle 对象分为两类，
一种是直接入队等待调度执行的 Handle 类（await 作用的协程），另一种是延迟执行的 TimerHandle 类（继承于 Handle 类）。
asyncio 对外提供了两个接口 `loop.call_soon()` 和 `loop.call_later()`，可以直接往 `self._ready` 队列添加函数，跳过需要用 Future 对象设置的限制。

```python
import asyncio


async def main():
    loop = asyncio.get_event_loop()
    loop.call_soon(lambda: print("Hello call_soon()"))  # 立即加入调度队列，并等待执行
    loop.call_later(1, lambda: print("Hello call_later()")) # 1s 后加入调度队列，并等待执行
    await asyncio.sleep(2)


asyncio.run(main())
```

#### (6) 事件循环和池

事件循环除了可以搭配 IO 多路复用实现异步之外，还可以搭配进程池、线程池使用。将原本的阻塞代码丢到池里面去执行，也可以避免事件循环被阻塞，实现异步目的。
**特别是对于 CPU 密集型任务，或者没有异步版本的第三方库等场景，都非常有用。**

事件循环和池之间的调度关系可以参考 [事件循环的调度流程](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0.md#%E4%BA%8B%E4%BB%B6%E5%BE%AA%E7%8E%AF%E7%9A%84%E8%B0%83%E5%BA%A6%E6%B5%81%E7%A8%8B) ，
将 IO 多路复用的部分换成池。

```python
import asyncio
import concurrent.futures


def blocking_io(file):
    with open(file, "r", encoding="utf8") as f:
        result = f.read()
    print(result)


def cpu_bound():
    result = sum(i * i for i in range(10 ** 7))
    print(result)
    
    
async def before_cpu_bound():
    print("在 before_cpu_bound() 完成之前执行了")
    

async def main():
    loop = asyncio.get_event_loop()
    
    # 将同步 IO 丢到线程池执行
    thread_pool = concurrent.futures.ThreadPoolExecutor(max_workers=4)
    await loop.run_in_executor(thread_pool, blocking_io, "/Users/tiger/develop/tmp/demo1.txt")
    
    # 将 CPU 密集型任务丢到进程池执行, 同时测试异步的效果
    process_pool = concurrent.futures.ProcessPoolExecutor(max_workers=4)
    await asyncio.gather(
        loop.run_in_executor(process_pool, cpu_bound),
        before_cpu_bound(), # 会先于 cpu_bound() 执行完毕
    )


asyncio.run(main())
```

> 除了这些操作之外，还有异步的 socket 操作、异步的 DNS 操作、异步的信号处理、异步的子进程操作等，其中不少操作也提供了更易于使用的高级 API。

## 可调度对象

协程在底层会被封装成 Task，而 Task 是 Future 的子类，也就是说这三种可调度对象都可以看成 Future 对象。

### (1) Future

相关操作参考 [(3) 事件循环和 Future](https://github.com/zongzhenh/Blog/blob/master/Python/IO/asyncio%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0.md#3-%E4%BA%8B%E4%BB%B6%E5%BE%AA%E7%8E%AF%E5%92%8C-future) 。

### (2) Task

Task 继承于 Future 类，提供的操作也差不多。

### (3) 协程

#### 协程的定义

- 协程基于生成器实现，区别是生成器 `yield` 出的是基础类型或者容器类型，协程 `yield` 出的只能是 `None` 或者 Future 对象
- 协程在事件循环中会被封装成 Task 对象
- 从上层看，可以认为被 `async` 关键字声明的异步函数就是一个协程
- 直接调用协程并不会执行，必须在前面加 `await`

```python
import asyncio


async def sleep(second):
    # asyncio 提供的异步 sleep() 方法, 不会阻塞事件循环,
    # 如果换成 time.sleep() 就会阻塞事件循环了
    await asyncio.sleep(second)    
    print("hello asyncio")


async def test_sleep(second):
    # 并发执行 10 个协程
    await asyncio.gather(
        sleep(second),
        sleep(second),
        sleep(second),
        sleep(second),
        sleep(second),
        sleep(second),
        sleep(second),
        sleep(second),
        sleep(second),
        sleep(second),
    )
    

async def test_return():
    return 21


async def main():    
    # 测试并发执行一组协程
    await test_sleep(1)
    
    # 测试带返回值的协程
    res = await test_return()
    print(res)

    
if __name__ == "__main__":
    # asyncio.run() 一般作为 asyncio 应用的入口, 只被调用一次
    asyncio.run(main())
```

> 只考虑 async/await 的写法，不考虑旧的 @asyncio.coroutine 写法。

> Profile 一下看到耗时 1039ms，确实并发执行了。

#### Python 协程和 Go 协程的区别

- coroutine 基于 asyncio 事件循环的调度，运行在一个线程上，当线程被阻塞，所有的 coroutine 会被阻塞，一般用于 IO 密集型任务，而且需要配合异步库使用。
总的来说是并发，不是并行
- goroutine 基于 Go 运行时 GPM 模型的调度，一般运行在多个线程上，当某一个线程被阻塞时，其他 goroutine 还可以运行在其他的线程上，既可以用于 IO 密集型任务，也可以用于
CPU 密集型任务。总的来说即是并发，也是并行

## await 语句

`await` 的语义是执行一个协程，当遇到阻塞时，主动让出执行权给其他协程。`await` 可以作用于协程、Task、Future 这三类对象。

### 设置超时时间

```python
import asyncio


async def sleep(second):
    await asyncio.sleep(second)
    print("hello asyncio")


async def test_timeout(second):
    try:
        await asyncio.wait_for(sleep(second), timeout=2)
    except asyncio.TimeoutError:
        print("asyncio.TimeoutError")
        

async def main():
    # 测试超时时间
    await test_timeout(5)


if __name__ == "__main__":
    asyncio.run(main())
```

> asyncio 很多操作并没有提供 timeout 参数来控制超时，但是可以通过 `asyncio.wait_for()` 实现。

## Streams

Streams 是专门用来处理网络 IO 的一组高级 API。

### echo 案例

- 服务端

```python
import asyncio


async def handle(reader, writer):
    MAX_MSGLEN = 100    # 假设每个消息最多 100 个字节

    try:
        data = await reader.read(MAX_MSGLEN)
        print("接收数据:", data.decode("utf8"))

        writer.write(data)
        await writer.drain()  # 搭配 write() 方法使用, 类似 sendall()
        print("发送数据:", data.decode("utf8"))
    except:
        raise
    finally:
        print("关闭连接")
        writer.close()


async def main():
    server = await asyncio.start_server(client_connected_cb=handle, host="127.0.0.1", port=2444)

    addr = server.sockets[0].getsockname()
    print(f'服务器监听: {addr}')

    # 启动服务端
    async with server:
        await server.serve_forever()


asyncio.run(main())
```

- 客户端

```python
import asyncio


async def echo_client():
    MAX_MSGLEN = 100  # 假设每个消息最多 100 个字节

    reader, writer = await asyncio.open_connection(host="127.0.0.1", port=2444)

    try:
        data = "Hello Streams"
        writer.write(data.encode("utf8"))
        await writer.drain()
        print("发送数据:", data)

        recv_data = await reader.read(MAX_MSGLEN)
        print("接收数据:", recv_data)
    except:
        raise
    finally:
        writer.close()


asyncio.run(echo_client())
```

## Queue

异步队列，用法和 [queue](https://docs.python.org/3/library/queue.html#module-queue) 模块差不多，但是线程不安全。
get/put 操作没有超时功能，需要配合 `asyncio.wait_for()` 实现。

```python
import asyncio


async def consumer(queue):
    while not queue.empty():
        second = await queue.get()
        await asyncio.sleep(second)
        queue.task_done()
        print("完成任务")

    print("consumer() return")


async def producer():
    queue = asyncio.Queue(maxsize=30)

    for i in range(10):
        await queue.put(1)

    return queue


async def main():
    queue = await producer()
    await consumer(queue)
    await queue.join()
    print("所有任务执行完毕")


asyncio.run(main())
```

## Subprocesses

异步 Subprocesses

```python
import asyncio


async def test_subprocess(cmd):
    proc = await asyncio.create_subprocess_shell(cmd,
                                                 stdout=asyncio.subprocess.PIPE,
                                                 stderr=asyncio.subprocess.PIPE)

    # stdout 和 stderr 都是 StreamReader 对象, stdin 是 StreamWriter 对象
    stdout, stderr = await proc.communicate() 

    if stdout:
        print(f'[stdout]\n{stdout.decode()}')
    if stderr:
        print(f'[stderr]\n{stderr.decode()}')


asyncio.run(test_subprocess("ls -l"))
```

## 协程同步

用法和 [threading](https://docs.python.org/3/library/threading.html#module-threading) 模块差不多，
区别是 asyncio 提供的同步操作只用于协程，并不是线程级别的同步，也就是线程不安全。超时功能需要配合 `asyncio.wait_for()` 实现。

```python
import asyncio

# case1: 同步锁
lock = asyncio.Lock()
async with lock:
    pass

# case2: 条件变量
def is_available():
    pass


cond = asyncio.Condition()
async with cond:
    await cond.wait_for(is_available())

if is_available():
    pass

# case3: Event
pass


# case4: Semaphore
pass
```

## 异常

直接参考 [这里](https://docs.python.org/3/library/asyncio-exceptions.html#exceptions) 。

## 其他模块级别函数

- 异步 sleep：`asyncio.sleep(second)`
- 将协程丢到事件循环执行：`asyncio.run(aws)`
- 判断是否异步函数：`asyncio.iscoroutinefunction(func)`
- 输出当前协程：`asyncio.current_task()`
- 输出所有协程：`asyncio.all_tasks()`

## 参考

- [asyncio — Asynchronous I/O](https://docs.python.org/3/library/asyncio.html)
- [小白的 asyncio ：原理、源码 到实现（1）](https://zhuanlan.zhihu.com/p/64991670)
- [How To Use Linux epoll with Python](http://scotdoyle.com/python-epoll-howto.html)
- [预备知识：我读过的对epoll最好的讲解](http://www.nowamagic.net/academy/detail/13321005)
- [Tornado IOLoop start()里的核心调度](http://www.nowamagic.net/academy/detail/13321037)
