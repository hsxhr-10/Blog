# 同步 IO

1. [IO 类型](https://github.com/zongzhenh/Blog/blob/master/Python/IO/%E5%90%8C%E6%AD%A5IO.md#io-%E7%B1%BB%E5%9E%8B)
2. [IO 类继承关系](https://github.com/zongzhenh/Blog/blob/master/Python/IO/%E5%90%8C%E6%AD%A5IO.md#io-%E7%B1%BB%E7%BB%A7%E6%89%BF%E5%85%B3%E7%B3%BB)
3. [open() 函数](https://github.com/zongzhenh/Blog/blob/master/Python/IO/%E5%90%8C%E6%AD%A5IO.md#open-%E5%87%BD%E6%95%B0)
4. [读和写](https://github.com/zongzhenh/Blog/blob/master/Python/IO/%E5%90%8C%E6%AD%A5IO.md#%E8%AF%BB%E5%92%8C%E5%86%99)
5. [性能和线程安全](https://github.com/zongzhenh/Blog/blob/master/Python/IO/%E5%90%8C%E6%AD%A5IO.md#%E6%80%A7%E8%83%BD%E5%92%8C%E7%BA%BF%E7%A8%8B%E5%AE%89%E5%85%A8)

## IO 类型

Python 支持三种 IO 类型：Text IO（文本 IO）、Binary IO（带缓冲的二进制 IO）、Raw IO（不带缓冲的二进制 IO）

- Text IO 作用于字符串对象，必须带缓冲，可以是行缓冲、系统默认缓冲大小或者自定义的缓冲大小
- Binary IO 作用于二进制对象，必须带缓冲，可以系统默认缓冲大小或者自定义缓冲大小
- Raw IO 作用于二进制对象，不能带缓冲

## IO 类继承关系

![](https://raw.githubusercontent.com/hsxhr-10/Blog/master/image/pythonio-2.png)

IOBase 中大量的方法是基于 Mixin 模式继承过来的，IOBase 没有实现读写方法，而是它的子类们去实现。一般常用的 IO 类有
TextIOWrapper、StringIO、BufferedReader、BufferedWriter、BytesIO

## open() 函数

核心的 IO 操作有 open、read、write、close、seek，open 是基础

### 重要参数说明

**open(file, mode='r', buffering=-1, encoding=None, errors=None, newline=None, closefd=True, opener=None)**

- file：要打开的文件对象，一般是字符串形式的文件名
- mode：文件打开模式（读、写、读写、追加、二进制）
- encoding：指定编码方式，只用于 Text IO
- newline：自定义换行符，只用于 Text IO
- closefd：关闭文件的时候，关闭底层的文件描述符
- buffering
    - -1：使用默认缓冲规则。如果是交互式 Text IO，则使用行缓冲；如果是 Binary IO、普通 Text IO，
      则使用系统默认的缓冲 `io.DEFAULT_BUFFER_SIZE`，或者自定义后的系统缓冲
    - 0：关闭缓冲，只用于 Raw IO
    - 1：使用行缓冲，只用于交互式 Text IO
    - 大于 1：使用自定义缓冲

### open() 和 IO 类

实际使用中一般不需要主动初始化 IO 类，而是通过 `open()` 函数去决定初始化哪种 IO 类

```python
import io


# Text IO
with open("...", "r", encoding="utf8") as f:
    print(type(f))

with io.StringIO("Hello Python IO") as f:
    print(type(f))

# Binary IO
with open("...", "rb") as f:
    print(type(f))

with open("...", "wb") as f:
    print(type(f))

with io.BytesIO(b"Hello Python IO") as f:
    print(type(f))
```

输出如下：

```BASH
<class '_io.TextIOWrapper'>
<class '_io.StringIO'>
<class '_io.BufferedReader'>
<class '_io.BufferedWriter'>
<class '_io.BytesIO'>
```

## 读和写

### TextIOWrapper

TextIOWrapper 的读写方法都是继承于 TextIOBase

#### read(size=-1)

从打开的文件流中读取指定字符数。如果 size 为 `None` 或者负数，则底层调用 Raw IO 的 `readall()` 方法读取整个文件的内容。
如果 size 为正数，而且缓冲区的字符数不足 size 时，底层会多次调用 syscall read，直到读到指定的字符数或者遇到 EOF 为止

- 案例1，通过 for 循环加 `read(size)` 读取整个文件：

```python
with open("...", "r", encoding="utf8") as f:
    chunk = 8192
    result = []

    tmp = f.read(chunk)
    while tmp != "":
        result.append(tmp)
        tmp = f.read(chunk)

print("".join(result))
```

- 案例2，直接 `read()`

```python
with open("...", "r", encoding="utf8") as f:
    print(f.read())
```

#### 一行一行读

按行读取不推荐 `readlines()` 方法，因为需要将整个文件的内容预先放到内存中，浪费内存。推荐下面的做法：

```python
with open("...", "r", encoding="utf8") as f:
    for line in f:
        print(line)
```

#### write(str)

降指定字符串写入打开的文件中，返回写入的字符个数，不保证一次就能写入全部字符

案例：

```python
with open("...", "w", encoding="utf8") as f:
    content = "djsafklhsdfhdjksfhsdbnfm,asdfklsdhioehjwiojweofin"
    total = len(content)
    ready = 0

    while ready < total:
        size = f.write(content[ready:])
        ready += size
```

#### flush()

强制将缓冲区的内容写入目标文件中

### StringIO

当文本内容已经在内存中，可以用 StringIO 对其进行读写

案例：

```python
import io


with io.StringIO("Hello Python IO") as f:
    print(f.getvalue())
```

### BufferedReader

BufferedReader 的读操作和 TextIOWrapper 基本一致，只是把字符换成了字节

需要关注 `read1([size])` 方法，相比 `read(size=-1)`，如果缓冲区的字节数不足时，该方法底层只会执行一次 syscall read，
也就是说不保证一定能读到 size 个字节数

### BufferedWriter

BufferedWriter 的写操作和 TextIOWrapper 基本一致，只是把字符换成了字节

### BytesIO

BytesIO 的读写操作和 StringIO 基本一致，只是把字符换成了字节

## 性能和线程安全

根据官网的描述：

- 在很多现代的操作中，无缓冲 IO 和缓冲 IO 差不多一样快
- `TextIOWrapper.tell()` 和 `TextIOWrapper.seek()` 重写过，而且性并能不好
- FileIO、BufferedReader, BufferedWriter, BufferedRandom、BufferedRWPair 是线程安全的
- TextIOWrapper 线程不安全

## 参考

- [open()](https://docs.python.org/3/library/functions.html#open)
- [io — Core tools for working with streams](https://docs.python.org/3/library/io.html#module-io)
- [What the difference between read() and read1() in Python?](https://stackoverflow.com/questions/57726771/what-the-difference-between-read-and-read1-in-python)
