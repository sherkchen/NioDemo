# NioDemo
java NIO学习


网络IO的过程：
1.等待数据从网络中到达网卡，当所有分组到达后它将会被复制到内核的缓冲区。这个操作系统内核层面的。

2.服务器通过read系统调用将内核缓冲区数据，读入到自己的进程缓冲区中。

3.读入完成后服务器处理相关业务逻辑。


IO类型
1. 同步阻塞IO(Blocking IO)

同步阻塞IO是指在上述IO的两个阶段即 数据copy到内核缓冲区和数据拷贝到用户缓冲区都会发生阻塞。
同步指用户空间主动与内核发起到IO方式， 内核空间是被动接受方。

2.同步非阻塞IO（Non-blocking IO）

非阻塞IO，指的是用户空间的程序不需要等待内核IO操作彻底完成（第一步内核准备数据到内核缓冲区）。但当内核缓冲
