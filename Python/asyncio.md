## asyncio

- asyncio 基于 I/O 多路复用实现 I/O 事件的异步处理
- asyncio 事件循环不能被同步代码阻塞，使用异步库、Executor 等方法可解决

> asyncio 需要 Python3.5+，最好是 Python3.7+，功能会多一些，少量新功能需要 Python3.9。另外，asyncio 的接口存在不向后兼容的情况，
> 比如 ["Deprecated since version 3.8, will be removed in version 3.10: The loop parameter."](https://docs.python.org/3/library/asyncio-task.html#asyncio.sleep) 这类

Python 协程和 Go 协程的区别：

- coroutine 基于 asyncio 事件循环的调度，运行在一个线程上，当线程被阻塞，所有的 coroutine 会被阻塞，一般用于 IO
  密集型任务，而且需要配合异步库使用。 总的来说是并发，不是并行
- goroutine 基于 Go 运行时 GPM 模型的调度，一般运行在多个线程上，当某一个线程被阻塞时，其他 goroutine
  还可以运行在其他的线程上，既可以用于 IO 密集型任务，也可以用于 CPU 密集型任务。总的来说即是并发，也是并行

### 事件循环相关理论

#### IO 多路复用

```python
# asyncio 的事件循环包含着 IO 多路复用，专门用来处理 IO 事件，
# 多路复用本身也有一个事件循环，一般基于多路复用的代码长这样

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

#### 事件循环中的可调度对象

- Future
- Task
- `async` 声明的函数/方法（可以大概看成协程）

#### await 语句

- `await` 的作用：当可能会发生阻塞时，主动让出执行权给其他协程
- `await` 可以作用于协程、Task、Future

```python
# 设置超时时间
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

> asyncio 很多操作并没有提供 timeout 参数来控制超时，但是可以通过 `asyncio.wait_for()` 实现

#### 事件循环的调度流程

调度流程这块其实比并不好找，单步调试最多走到 events.py:88，貌似是因为协程底层是 C 实现的缘故。到网上查找发现切换相关的代码在 tasks.py
的 `__step()` 和 `__wakeup()` 方法上，
`__wakeup()` 调用 `__step()`，所以重点是 `__step()`。协程恢复中断的核心是通过生成器的 `send(None)`
来从中断的地方继续执行。

![](https://raw.githubusercontent.com/hsxhr-10/Blog/master/image/pythonio-4.png)

**※️ 整理一下调度流程大致如下**

![](https://raw.githubusercontent.com/hsxhr-10/Blog/master/image/pythonio-5.png)

从这里可以看出，协程和线程的一个区别是，前者主动礼让，后者抢着执行。

### 事件循环使用例子

```python
# 获取事件循环
import asyncio


async def main():
    loop1 = asyncio.get_event_loop()
    loop2 = asyncio.get_running_loop()
    loop3 = asyncio.new_event_loop()
    print(loop1 == loop2)  # True
    print(loop1 == loop2 == loop3)  # False


asyncio.run(main())
```

```python
# 启动和停止事件循环
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

```python
# 事件循环和 Future
import asyncio


async def main():
    loop = asyncio.get_event_loop()
    future = loop.create_future()  # 创建 Future 对象

    print(future.done())  # False
    print(future.cancelled())  # False
    print(loop == future.get_loop())  # True

    try:
        print("Get future result:", future.result())  # 抛出异常
    except asyncio.base_futures.InvalidStateError:
        print("Result is not set")

    future.add_done_callback(
        lambda _: print("Run future done callback!"))  # 给 Future 绑定回调函数
    future.set_result(
        111)  # 设置 result 的同时, 将 Future 的回调函数加入 self._ready 队列, 等待被事件循环调度执行
    try:
        print("get future result:", future.result())  # get future result: 111
    except asyncio.base_futures.InvalidStateError:
        print("Result is not set")


asyncio.run(main())
```

```python
# Callback Handle
# `loop.call_soon()` 和 `loop.call_later()`，可以直接往调度队列 `self._ready` 队列添加待执行函数
import asyncio


async def main():
    loop = asyncio.get_event_loop()
    loop.call_soon(lambda: print("Hello call_soon()"))  # 立即加入调度队列，并等待执行
    loop.call_later(1, lambda: print("Hello call_later()"))  # 1s 后加入调度队列，并等待执行
    await asyncio.sleep(2)


asyncio.run(main())
```

```python
# asyncio 和 Executor
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
    await loop.run_in_executor(thread_pool, blocking_io,
                               "/Users/tiger/develop/tmp/demo1.txt")

    # 将 CPU 密集型任务丢到进程池执行, 同时测试异步的效果
    process_pool = concurrent.futures.ProcessPoolExecutor(max_workers=4)
    await asyncio.gather(
        loop.run_in_executor(process_pool, cpu_bound),
        before_cpu_bound(),  # 会先于 cpu_bound() 执行完
    )


asyncio.run(main())
```

### 参考

- [asyncio — Asynchronous I/O](https://docs.python.org/3/library/asyncio.html)
- [小白的 asyncio ：原理、源码 到实现（1）](https://zhuanlan.zhihu.com/p/64991670)
- [How To Use Linux epoll with Python](http://scotdoyle.com/python-epoll-howto.html)
- [预备知识：我读过的对epoll最好的讲解](http://www.nowamagic.net/academy/detail/13321005)
- [Tornado IOLoop start()里的核心调度](http://www.nowamagic.net/academy/detail/13321037)
