package reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  单线程Reactor 和handler 公用一个线程和一个selector.
 *  如果handler线程阻塞， Reactor处理i/o的就会阻塞。
 *
 *  现在改进为每个handler 单独使用线程池完成，和Reactor 相互隔离
 */
public class MultiThreadEchoServerReactor {
    ServerSocketChannel serverSocket;
    AtomicInteger next = new AtomicInteger(0);
    // Selectors集合 引入多个selector 选择器
    Selector[] selectors = new Selector[2];
    // 引入对过子反应器
    SubReactor[] subReactors = null;

    MultiThreadEchoServerReactor() throws IOException {
        // 初始化多个选择器
        selectors[0] = Selector.open();
        selectors[1] = Selector.open();
        serverSocket = ServerSocketChannel.open();

        InetSocketAddress address =
                new InetSocketAddress("localhost",
                        9090);
        serverSocket.socket().bind(address);
        //非阻塞
        serverSocket.configureBlocking(false);

        //第一个selector,负责监控新连接事件
        SelectionKey sk =
                serverSocket.register(selectors[0], SelectionKey.OP_ACCEPT);

        //附加新连接处理handler处理器到SelectionKey（选择键）
        sk.attach(new AcceptorHandler());

        //第一个子反应器，一子反应器负责一个选择器
        SubReactor subReactor1 = new SubReactor(selectors[0]);
        //第二个子反应器，一子反应器负责一个选择器
        SubReactor subReactor2 = new SubReactor(selectors[1]);
        subReactors = new SubReactor[]{subReactor1, subReactor2};

    }

    private void startService() {
        // 一子反应器对应一条线程
        new Thread(subReactors[0]).start();
        new Thread(subReactors[1]).start();
    }

    class SubReactor  implements  Runnable {
        // 每个线程负责一个选择器查询
        final Selector selector;

        public SubReactor(Selector selector) {
            this.selector  = selector;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    // 这里会阻塞等待通道就绪事件
                    selector.select();
                    Set<SelectionKey> keySet = selector.selectedKeys();
                    Iterator<SelectionKey> it = keySet.iterator();
                    while(it.hasNext()) {
                        //Reactor负责dispatch收到的事件
                        SelectionKey sk = it.next();
                        dispatch(sk);
                    }
                    keySet.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void dispatch(SelectionKey sk) {
            Runnable handler = (Runnable) sk.attachment();
            // 调用之前attach绑定到选择键的handler处理器对象
            if (handler != null) {
                handler.run();
            }
        }
    }

    // Handler:新连接处理器
    class AcceptorHandler implements Runnable {

        @Override
        public void run() {

            try{
                // 获取服务的通道
                SocketChannel channel = serverSocket.accept();
                if (channel != null) {
                    // 交替注册到对应到选择器中
                    new MultiThreadEchoHandler(selectors[next.get()], channel);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            // 这边是干嘛的
            if (next.incrementAndGet() == selectors.length) {
                next.set(0);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        MultiThreadEchoServerReactor server =
                new MultiThreadEchoServerReactor();
        server.startService();
    }
}
