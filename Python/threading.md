# threading 笔记

1. [Python 的并发处理](https://github.com/zongzhenh/Blog/blob/master/Python/%E5%B9%B6%E5%8F%91/threading%E7%AC%94%E8%AE%B0.md#python-%E7%9A%84%E5%B9%B6%E5%8F%91%E5%A4%84%E7%90%86)
2. [threading 的使用](https://github.com/zongzhenh/Blog/blob/master/Python/%E5%B9%B6%E5%8F%91/threading%E7%AC%94%E8%AE%B0.md#threading-%E7%9A%84%E4%BD%BF%E7%94%A8)
3. [queue 的使用](https://github.com/zongzhenh/Blog/blob/master/Python/%E5%B9%B6%E5%8F%91/threading%E7%AC%94%E8%AE%B0.md#queue-%E7%9A%84%E4%BD%BF%E7%94%A8)

## Python 的并发处理

由于 GIL 的存在，无论 CPU 有多少个核心，一个时刻只能有一个线程使用 CPU 资源，所以 Python 线程是并发不是并行，适用于 IO 密集型任务，不适用于 CPU 密集型任务

Python 并发方案选择：

- 如果是 IO 密集型任务，并且每个 IO 操作很慢，又需要很多任务并发执行，使用基于协程的异步方案解决（比如 asyncio、gevent）
- 如果是 IO 密集型任务，但是每个 IO 操作很快，只需要有限数量的任务并发执行，使用多线程即可
- 如果是 CPU 密集型任务，使用多进程（加上用 Cython 优化~）

## threading 的使用

### 模块级别函数

```python
import threading


def main():
    t = threading.Thread(target=lambda: print("Hello threading"))
    t.start()
    
    print("当前活动线程总数:", threading.active_count())
    print(threading.current_thread() == threading.main_thread())
    print("主线程 ID:", threading.get_ident())
    print("活动线程列表:", threading.enumerate())
    print("锁的最大超时时间:", threading.TIMEOUT_MAX)
    print("main thread done.")


main()
```

### 线程对象

```python
import threading
import time


def task(i):
    time.sleep(2)
    print("task {} done.".format(i))


def main():
    # 创建线程对象
    t = threading.Thread(target=task, args=(-1, ))

    print("线程对象名字:", t.name)    #  Thread-1
    t.name = "ooxoo"
    print("新的线程对象名字:", t.name)  # ooxoo
    print("线程对象 ID:", t.ident)  # None
    print("线程对象是否已启动:", t.is_alive())    # False
    
    # 启动线程, start() 调用 run() 方法, run() 会执行 task() 函数
    t.start()

    print("线程对象是否已启动:", t.is_alive())    # True
    print("线程对象 ID:", t.ident)  # 123145445199872
    
    # 等待线程 t 执行完毕
    t.join()

    print("main thread done.")


main()
```

#### daemon

主线程结束后，如果想让其创建的子线程跟着自动结束，可以通过设置守护线程实现 

```python
import threading
import time


def task(i):
    time.sleep(2)
    print("task {} done.".format(i))


def main():
    workers = []
    for i in range(5):
        t = threading.Thread(target=task, args=(i,))

        # 设置成守护线程
        t.daemon = True

        workers.append(t)

    for worker in workers:
        worker.start()

    print("main thread done.")


main()
```

程序只会输出 main thread done.

### 线程同步

#### (1) 锁

函数说明：

- `threading.Lock()`：可以创建锁对象，自动返回当前系统所支持的锁类型
- `acquire(blocking=True, timeout=-1)`
    - 将锁对象状态设置成 locked（申请锁），如果成功则返回 True，否则返回 False
    - blocking 为 True 代表阻塞等待，直到申请锁成功，或者超时为止；为 False 代表如果不能立即申请锁，则不等待直接返回
    - timeout 为 -1 代表一直等待下去，`threading.TIMEOUT_MAX` 可以获取系统支持的最大等待时间
- `release()`
    - 将锁对象状态设置成 unlocked（释放锁），可以在任意线程中执行，不一定是持有锁的线程。该函数没有返回值
    - 如果有多个线程在等待锁，会随机选择一个线程持有锁
    - 如果释放 unlocked 的锁对象，则会抛出 RuntimeError
- `locked()`：如果锁对象状态为 locked 则返回 True，否则返回 False

```python
import threading
import time


lock = threading.Lock()


def main():
    try:
        # 阻塞等待
        res = lock.acquire()
        print("第一次申请锁结果:", res) # True

        # 不等待
        res = lock.acquire(blocking=False)
        print("第二次申请锁结果:", res) # False

        print("当前锁对象状态:", lock.locked())    # True

        # do something
        time.sleep(1)
        print("task done.")
    except:
        raise
    finally:
        lock.release()
        print("释放锁成功")


main()
```

> 支持 `with` 语句

#### (2) 可重入锁

支持的操作和锁对象差不多，没有 `locked()` 方法，和锁对象的区别是，对于持有锁的线程可以继续申请锁

```python
import threading
import time


rlock = threading.RLock()


def task(i):
    try:
        # 阻塞等待
        res = rlock.acquire()
        print("{} 第一次申请锁结果: {}".format(i, res))

        # 不等待
        res = rlock.acquire(blocking=False)
        print("{} 第二次申请锁结果: {}".format(i, res))

        # 阻塞等待
        res = rlock.acquire()
        print("{} 第三次申请锁结果: {}".format(i, res))

        # do something
        time.sleep(1)
        print("task {} done.".format(i))
    except:
        raise
    finally:
        # 需要释放相应次数，否则另外一个线程会一直等到
        rlock.release()
        rlock.release()
        rlock.release()
        print("{} 释放锁成功".format(i))


def main():
    workers = []
    for i in range(2):
        t = threading.Thread(target=task, args=(i,))
        workers.append(t)

    for worker in workers:
        worker.start()

    print("main thread done.")


main()
```

> 支持 `with` 语句

#### (3) 条件变量

condition 对象搭配锁对象使用，可以在线程不满足某种条件时主动释放锁，并阻塞等待，直到当条件满足时被其他线程唤醒。相对单纯的互斥锁，
这是一种基于通信的同步方式

函数说明：

- `threading.Condition(lock=None)`：可以创建 condition 对象，如果 lock 为 `None`，会默认创建一个 `RLock` 对象
- `acquire(*args)`：会去调用对应锁对象的 `acquire()` 方法
- `release()`：会去调用对应锁对象的 `release()` 方法
- `wait(timeout=None)`：主动释放锁，并进入等待状态，直到被唤醒或者超时
- `wait_for(predicate, timeout=None)`：主动释放锁，并进入等待状态，直到被唤醒或者超时，而且被唤醒后必须满足条件（也就是 predicate 必须是 True）才能继续往下执行，
  否则又会调用 `wait()` 进入等待状态。也就是相当于 `while not predicate: wait()` 的语法糖
- `notify(n=1)`：打算唤醒 n 个等待线程，该方法被调用后，线程并不会立即被唤醒，还是要等到 `release()` 被调用之后。必须是持有锁的线程调用，否则抛出 RuntimeError 异常
- `notify_all()`：打算唤醒所有等待线程，该方法被调用后，线程并不会立即被唤醒，还是要等到 `release()` 被调用之后。必须是持有锁的线程调用，否则抛出 RuntimeError 异常

案例 1：基本使用

```python
import threading
import time


lock = threading.Lock()
cond = threading.Condition(lock)


def task1():
    try:
        cond.acquire()
        print("线程 1 进入等待")
        cond.wait()
        print("线程 1 被唤醒")

        # do something
        time.sleep(1)
        print("线程 1 执行成功")
    except:
        raise
    finally:
        cond.release()
        print("线程 1 释放锁成功")


def task2():
    try:
        cond.acquire()
        # do something
        time.sleep(1)
        print("线程 2 执行成功")
        cond.notify_all()
        print("线程 2 唤醒线程 1")
    except:
        raise
    finally:
        cond.release()
        print("线程 2 释放锁成功")


def main():
    t1 = threading.Thread(target=task1)
    t1.start()
    t2 = threading.Thread(target=task2)
    t2.start()

    print("main thread done.")


main()
```

```BASH
线程 1 进入等待
main thread done.
线程 2 执行成功
线程 2 唤醒线程 1
线程 2 释放锁成功
线程 1 被唤醒)
线程 1 执行成功
线程 1 释放锁成功
```

案例 2：`wait()` 方法会一直等待，直到被唤醒或者超时

```python
import threading
import time


lock = threading.Lock()
cond = threading.Condition(lock)


def task1():
    try:
        cond.acquire()
        print("线程 1 进入等待")
        cond.wait()
        print("线程 1 被唤醒")

        # do something
        time.sleep(1)
        print("线程 1 执行成功")
    except:
        raise
    finally:
        cond.release()
        print("线程 1 释放锁成功")


def task2():
    try:
        cond.acquire()
        # do something
        time.sleep(1)
        print("线程 2 执行成功")
        # 注释这两行，不去唤醒
        # cond.notify_all()
        # print("线程 2 唤醒线程 1")
    except:
        raise
    finally:
        cond.release()
        print("线程 2 释放锁成功")


def main():
    t1 = threading.Thread(target=task1)
    t1.start()
    t2 = threading.Thread(target=task2)
    t2.start()

    print("main thread done.")


main()
```

线程 1 会一直阻塞下去

案例 3：`wait_for()` 被唤醒后，还需要满足条件，否则继续等待

```python
import threading
import time


lock = threading.Lock()
cond = threading.Condition(lock)
flag = 0


def predicate():
    return True if flag == 1 else False


def task1():
    try:
        cond.acquire()
        print("线程 1 不满足条件，进入等待")
        cond.wait_for(predicate)
        print("线程 1 满足条件")

        # do something
        time.sleep(1)
        print("线程 1 执行成功")
    except:
        raise
    finally:
        cond.release()
        print("线程 1 释放锁成功")


def task2():
    try:
        cond.acquire()
        # do something
        time.sleep(1)
        global flag
        
        # case1: 满足条件的修改，线程 1 被唤醒后可以继续往下执行
        flag += 1
        # case2: 不满足条件的修改，线程 1 被唤醒不能往下执行，继续调用 wait() 等待
        # flag += 10

        print("线程 2 执行成功")
        cond.notify_all()
        print("线程 2 唤醒线程 1")
    except:
        raise
    finally:
        cond.release()
        print("线程 2 释放锁成功")


def main():
    t1 = threading.Thread(target=task1)
    t1.start()
    t2 = threading.Thread(target=task2)
    t2.start()

    print("main thread done.")


main()
```

> 支持 `with` 语句

#### (4) Semaphore

在并发环境下，Semaphore 用于保护数量有限的资源

从源码上看可以知道 `BoundedSemaphore` 继承于 `Semaphore`，两者区别在于 `release()` 方法，`BoundedSemaphore` 在释放锁时会对比释放次数 `self._value` 和
初始化时指定的次数 `self._initial_value`，确保了释放次数不会超过指定的次数，否则抛出 ValueError 异常，更加方便可靠

```python
class Semaphore:
    def __init__(self, value=1):
        if value < 0:
            raise ValueError("semaphore initial value must be >= 0")
        self._cond = Condition(Lock())
        self._value = value

    def acquire(self, blocking=True, timeout=None):
        # 忽律
        pass

    __enter__ = acquire

    def release(self):
        with self._cond:
            self._value += 1
            self._cond.notify()

    def __exit__(self, t, v, tb):
        self.release()
```

```python
class BoundedSemaphore(Semaphore):
    def __init__(self, value=1):
        Semaphore.__init__(self, value)
        self._initial_value = value

    def release(self):
        with self._cond:
            if self._value >= self._initial_value:
                raise ValueError("Semaphore released too many times")
            self._value += 1
            self._cond.notify()
```

```python
import threading
import time

max_connections = 3
pool = threading.BoundedSemaphore(max_connections)


def task3(i):
    try:
        pool.acquire()
        print("线程 {} 获取到资源".format(i))
        # do something
        time.sleep(1)
    except:
        raise
    finally:
        pool.release()


def main():
    workers = []
    for i in range(9):
        t = threading.Thread(target=task3, args=(i, ))
        # t.daemon = True
        workers.append(t)

    for worker in workers:
        worker.start()

    print("main thread done.")


main()
```

输出是三个一组，说明限制有效

> 支持 `with` 语句

#### (5) Event

感觉条件变量已经覆盖了它的功能

#### (6) Timer

可以用来实现简单的定时任务

```python
import threading


timer = threading.Timer(3, lambda: print("Hello Timer"))
timer.start()
```

## queue 的使用

[queue](https://docs.python.org/3/library/queue.html) 模块提供了多种线程安全的队列，用于线程间通信。
[SimpleQueue](https://docs.python.org/3/library/queue.html#simplequeue-objects) 比较常用，标准库不少需要线程间通信的地方也是用的 SimpleQueue

简单的生产者消费者模型

```python
import queue
import threading
import time

if __name__ == "__main__":
    _queue = queue.SimpleQueue()

    def producer():
        for i in range(5):
            time.sleep(2)
            _queue.put(i)
            print("生产数据 {}".format(i))

        print("生产数据完毕")

    def consumer():
        while True:
            print("等待数据...")
            data = _queue.get()
            time.sleep(0.5)
            print("消费数据 {}".format(data))


    producer = threading.Thread(target=producer)
    consumer = threading.Thread(target=consumer)

    consumer.start()
    producer.start()
```

```BASH
等待数据...
生产数据 0
消费数据 0
等待数据...
生产数据 1
消费数据 1
等待数据...
生产数据 2
消费数据 2
等待数据...
生产数据 3
消费数据 3
等待数据...
生产数据 4
消费数据 4
等待数据...
生产数据完毕
```

## 其他

在 threading 源码中看到一个小技巧，在 Python 程序退出之前执行一个处理函数

```python
import atexit


def _python_exit():
    print("自定义退出回调函数")


atexit.register(_python_exit)
```
