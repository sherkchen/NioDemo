package reactor;

import apple.laf.JRSUIConstants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 *   Reactor 反应器模式  广泛的用于Nginx、Redis、Netty中间件技术实现高并发
 *   传统的服务器处理请求方式。
 *     当服务器接收到请求后就开启一个线程处理对应的事件。Connection Per Thread（一个线程处理一个连接
 *     对应于大量的连接，需要耗费大量的线程资源，对线程资源要求太高。在系统中，
 *     线程是比较昂贵的系统资源。如果线程数太多，系统无法承受。
 *     而且，线程的反复创建、销毁、线程的切换也需要代价。因此，在高并发的应用场景下，
 *     多线程OIO的缺陷是致命的。
 *
 *     因此出现了Reactor  反应器模式
 *     这个模式中主要有两个重要组件
 *     1. Reactor反应器，负责查询IO事件， 当检测到IO事件时， 将其发给对应的handler 处理器区处理。
 *     这里的IO事件，就是NIO中选择器监控的IO事件。
 *
 *     2.Handler处理器，与IO事件绑定，负责对应事件处理。 完成真正的连接建立， 通道读取，处理业务逻辑，
 *     负责将结果写出到通道。
 *
 *     下面到demo 是单线程的reactor 反应器（即 Reactor 和相关的 handler都在一个线程中执行）
 *
 *     1.AcceptorHandler: 接受客户端的连接；   为新连节创键一个输入输出的handler 处理器 ECHOHanler 用于将客户端的写入数据， 打印出来
 */

/**
 *  Reactor 是基于NIO实现的。只不过运用了设计模式， 将NIO 分成了
 *    1.Reactor反应器, 监听O事件时， 将其发给对应的handler
 *    2. handler  处理对应的事件
 *
 *    这个demo中Reactor反应器 和handler 都在一个线程中完成
 *    且共同使用同一个选择器。
 *
 *    缺点：
 *    Reactor反应器和Handler处理器，都执行在同一条线程上。这样，带来了一个问题：
 *    当其中某个Handler阻塞时，会导致其他所有的Handler都得不到执行。在这种场景下，
 *    如果被阻塞的Handler不仅仅负责输入和输出处理的业务，还包括负责连接监听的AcceptorHandler处理器。
 *    这个是非常严重的问题。为什么？
 *    一旦AcceptorHandler处理器阻塞，
 *    会导致整个服务不能接收新的连接，使得服务器变得不可用。因为这个缺陷，因此单线程反应器模型用得比较少。
 */
public class EchoServerReactor implements  Runnable {
    //  网络IO通道， 具体负责进行的读写操作。
    Selector selector;
    //  在服务器监听新的客户端socket连接
    ServerSocketChannel serverSocket;

    // 负责查询IO事件， 当检测到IO事件时，发送给对应的handler
    EchoServerReactor() throws IOException {
        // Reactor 初始化
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        InetSocketAddress address = new InetSocketAddress("localhost",
                9090);
        // 监听对应的端口地址， 等待服务器的请求
        serverSocket.socket().bind(address);
        // 非阻塞
        serverSocket.configureBlocking(false);
        // 分步处理， 第一步接接收accept 事件， 将客户端连接通道注册到选择器，绑定对应 socket 接受事件
        SelectionKey sk = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        // 绑定对应到接受对应事件的handler事件，处理新连接
        sk.attach(new AcceptorHandler());
    }

    // 处理新来的连接
    class AcceptorHandler implements  Runnable {
        @Override
        public void run() {
            try {
                // handler的处理逻辑， 当接受到注册事件时， 获取这个监听的通道
                // 什么时候会执行run
                SocketChannel channel = serverSocket.accept();
                if (channel != null) {
                    // 将handler 注册到select中
                    // 这个select 复用的是reactor的select。 再把对应的处理通道注册到这个同一线程的selector
                    new EchoHandler(selector, channel);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    @Override
    public void run() {
        try {
            // EchoServerReactor 启动 后 等待通道监听， 这时只有一个sector 监听了多个通道
            while (!Thread.interrupted()) {
                // Selects a set of keys whose corresponding channels are ready for I/O operations.
                // 这是一个阻塞的操作，直到有通道准备好
                selector.select();
                // 获取准备就绪的通道。
                Set<SelectionKey> selected = selector.selectedKeys();
                Iterator<SelectionKey> it = selected.iterator();
                while (it.hasNext()) {
                    //Reactor负责dispatch收到的事件
                    SelectionKey sk = it.next();
                    // 分发到对应到即拿出对应的handler， 进行处理
                    dispatch(sk);
                }
                selected.clear();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    void dispatch(SelectionKey sk) {
        Runnable handler = (Runnable)  sk.attachment();
        // 调用之前attach绑定到选择键的handler处理器对象
        if (handler !=  null) {
            // 执行了handler的逻辑
            handler.run();
        }
    }

    public static void main(String[] args) throws IOException {
        // 启动单线程的reactor：  负责查询IO事件，当检测到一个IO事件，将其发送给相应的Handler处理器去处理
        // 构造函数里面已经设置了服务端绑定的端口和监听的事件， 并设置的了通道和select
        new Thread(new EchoServerReactor()).start();
    }

}
