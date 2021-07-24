## 日常

查找：

- find 和常用参数：https://www.cnblogs.com/liuhedong/p/10813372.html
- find 和 xargs：`find ~ -name "*.pyc" | xargs ls -l {}`

归档和压缩：

- 归档压缩（忽略隐藏文件）：`tar --exclude=".*" -zcvf $NAME.tar.gz $NAME`
- 解归档解压缩：`tar -zxvf $NAME.tar.gz`

上传和下载：

- scp 上传：`scp $LOCAL_PATH $USER@$HOST:$REMOTE_PATH`
- scp 下载：`scp $USER@$HOST:$REMOTE_PATH $LOCAL_PATH`
- rz 上传：`rz`，然后在弹出的窗口选择文件
- sz 下载：`sz $NAME`

挂载和卸载：

- 挂载：`mount 设备名称 挂载点`（设备名一般可以在设备插入时通过 `tail -f /var/log/syslog` 确定）
- 卸载：`umount 设备名称`
- 设备命名规范
    - /dev/hd*：老式磁盘
    - /dev/sd*：新式磁盘
    - /dev/sr*：光驱

设备之间大量数据复制：

- `dd if=设备a名称 of=设备b名称`（设备a的数据要复制到设备b）

文件权限：

- 修改文件权限：`chmod 权限值 文件名称`、`chmod -R 权限值 文件名称`
- 递归修改目录权限：`chmod -R 权限值 文件名称`、`chown -R 所有者名称:组名称 文件名称`

awk：

- 强制终止某个服务：
  `sudo ps aux | grep gunicorn | grep -v color | awk  'BEGIN {FS=" "} {print $2 "\t"}' | xargs kill -9`

---

## 系统信息

系统版本信息：

- 查看系统发行版本号：`lsb_release -a`
- 查看系统内核版本、位信息：`uname -a`

CPU 信息：

- 查看 CPU 信息：`cat /proc/cpuinfo`
- 查看 CPU 使用情况：`top`，然后 1

内存信息：

- 查看 MEM 信息：`cat /proc/meminfo`
- 查看 MEM 使用情况：`free -h`

磁盘信息：

- 查看磁盘信息：`df -h`
- 查看当前目录下占用空间最大的前十个文件：`du -hsx * | sort -hr | head -10`
- 按进程号监控磁盘使用情况：`iotop`

网络：

- 查看网卡信息：`ifconfig`（RX errors、TX errors 等字段可以了解网络情况健康的基本情况）
- 测试网络连通性、延迟等：`ping`
- 根据关键字查找某个 TCP 服务的网络信息：`netstat -antp | grep $KEY`（UDP 服务是 `-anup`）
- 查看端口占用：`lsof -i :$PORT`
- 路由跟踪：`mtr $HOST`
- 按照网卡接口监控网络流量：`ifstat`
- 按照进程监控网络流量：`nethogs -a`

进程和线程：

- 根据关键字查找进程相关信息：`ps -aux | grep $KEY`
- 查僵尸进程：`ps -A -ostat,ppid,pid,cmd | grep -e '^[Zz]'`
- 让进程后台执行：`nohup` + `&`
- 让进程前台执行：`bg %$PID`
- 向进程发送信号：`kill + -数字`（19停止/18继续、9强制终止）
- 根据 PID 找对应线程：`ps -mp $PID -o THREAD,tid,time`
- 查看进程可以打开的最大文件数：`ulimit -u`
- 优化进程可以打开的最大文件数：`ulimit -HSn $NUMBER`
- 查看进程占用的真实内存：`pmap -x $PID`
- top 相关
    - 根据 PID 查找单个进程信息：`top -p $PID`
    - 进程按照 CPU 使用量排序：`top`，然后 Shift+p
    - 进程按照 MEM 使用量排序：`top`，然后 Shift+m

文件：

- 查看进程的文件使用情况：`lsof -p $PID`

---

## 系统性能

性能分析相关：

- 查看服务器可以打开的最大文件数：`cat /proc/sys/fs/file-max`
- 查看进程的资源限制：`ulimit -a`
- 查看服务器 CPU、内存、虚拟内存、磁盘 IO、进程等资源的整体使用情况：`vmstat 2`
    - CPU 相关指标
        - in：每秒的中断次数
        - cs：每秒的上下文切换次数（系统调用、线程切换、进程切换），值太大说明可能有地方进程数、线程数等数量设置不合理
        - us：用户进程所占用的 CPU 时间
        - sy：系统进程所占用的 CPU 时间，值太大说明可能存在长时间的系统调用，比如频繁执行阻塞的 IO 操作
        - id：空闲的 CPU 时间
        - wa：等待 IO 的 CPU 时间，值太大说明 IO 操作比较频繁，往往会严重影响系统的性能
    - 内存、虚拟内存相关指标
        - free：空闲的物理内存大小
        - buff：用于磁盘块的缓存大小
        - cache：用于文件的缓存大小
        - swpd：已使用的虚拟内存大小，如果值太大，可能存在内存瓶颈或者内存泄漏问题
        - si：每秒从磁盘读入虚拟内存的大小，同上
        - so：每秒从虚拟内存写入磁盘的大小，同上
    - 磁盘 IO 相关指标
        - bi：每秒写入磁盘的块数量（块大小默认 1024 byte）
        - bo：每秒从磁盘读取的块数量
    - 进程相关指标
        - r：有多少个进程被分配到 CPU 资源，当值超过 CPU 个数时，就可能存在 CPU 瓶颈
        - b：阻塞的进程个数
- 查看磁盘 IO 情况：`iostat -dkx 2`
    - util：设备的繁忙程度
    - r/s：设备每秒的读请求个数
    - w/s：设备每秒的写请求个数
    - rkB/s：设备每秒的读数据大小
    - wkB/s：设备每秒的写数据大小
    - r_await：设备每次读 IO 所需的时间（包括等待和实际处理，单位 ms，一般小于 5ms）
    - w_await：设备每次写 IO 所需的时间（同上）
    - svctm：设备每次 IO 请求的实际处理时间，如果 r_await、w_await 远大于 svctm 的值，说明等待执行 IO 等待队列很长

---

## 参考

- Linux
  vmstat命令实战详解：https://www.cnblogs.com/ggjucheng/archive/2012/01/05/2312625.html
- What is the difference between buffer and cache memory in
  Linux?：https://stackoverflow.com/questions/6345020/what-is-the-difference-between-buffer-and-cache-memory-in-linux
- Linux
  IO实时监控iostat命令详解：https://www.cnblogs.com/ggjucheng/archive/2013/01/13/2858810.html  




