package socketDemo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * JAVA NIO 主要由3个核心组件组成
 *   1.channel  （应用程序与通道进行交互操作， 双向）。
 *   2. Buffer
 *   3. Selector: 查询多个通道的IO事件就绪状态。 （不过需要先注册通道到选择器）
 *   一个选择器只需要一个线程
 *
 *   原本阻塞是线程等待数据准备完成前不能做任何事。
 *   NIO 将这个等待交给了select。， 线程可以做其他事，等需要等时候
 *
 *   特点：
 *       1。。从通道中读取数据到缓冲区， 从缓冲区中写数据到通道。
 *       2.可以随意读取Buffer中任意位置到数据。
 *
 *    /**
 *  *  当前的处理方式是一个请求来了用一个线程处理。
 *  *  当单个线程处理当时候遇到io 仍然是阻塞的。
 *  *  IO 交互流程：
 *  *  1用户发起系统调用进行IO.
 *  *  2. 内核将数据移动的内核缓冲区
 *  *  3.内核缓冲区将数据传输到用户缓冲区。
 *  *  4。 用户程序进行业务逻EchoServerReactor辑
 *
 *     NIO在第3步会阻塞。
 *     不阻塞到原因是将内核准备数据是否完成到过程交接给了select。进行监听
 *     将对应到通道注册到select中。
 *     一个选择器可以监听多个通道。 多路IO服用。
 *
 *     参考https://zhuanlan.zhihu.com/p/93903230
 */
public class NioFileRecServer {

    private Charset charset = Charset.forName("UTF-8");

    /**
     * 服务器端保存的客户端对象，对应一个客户端文件
     */
    static class Client {
        //文件名称
        String fileName;
        //长度
        long fileLength;

        //开始传输的时间
        long startTime;

        //客户端的地址
        InetSocketAddress remoteAddress;

        //输出的文件通道
        FileChannel outChannel;

    }

    // 通道将数据写入缓存， 从缓存中读数据到通道
    private ByteBuffer buffer = ByteBuffer.allocate(2048);
    // 使用Map保存每个客户端传输，当OP_READ通道可读时，根据channel找到对应的对象
    Map<SelectableChannel, Client> clientMap = new HashMap<SelectableChannel, Client>();

    public void startServer() throws IOException {
        // 1获取select 选择器
        Selector selector = Selector.open();

        // 2.获取通道
        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        ServerSocket serverSocket = serverChannel.socket();

        // 3.设置为非阻塞
        serverChannel.configureBlocking(false);

        //4.绑定链接
        InetSocketAddress address = new InetSocketAddress(9090);
        serverSocket.bind(address);

        // 将通道注册到选择器上。 注册到i/o事件为 "接收新链接"
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("server channel is listening");

        // 6.轮询感兴趣的I/O就绪事件 （选择键集合） 这个阻塞的方法
        while(selector.select() > 0) {
            // 7、获取选择键集合
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();

            while (it.hasNext()) {
                // 8.获取单个键并处理
                SelectionKey key = it.next();

                // 9.判断key 具体是什么事件， 是否为新链接事件
                if (key.isAcceptable()) {
                    // 10 若为新链接事件，就获取客户端新链接
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = server.accept();

                    if (socketChannel == null) {
                        continue;
                    }

                    // 11 客户端链接转换为非阻塞模式
                    socketChannel.configureBlocking(false);
                    // 12 客户端新连接通道 注册到selector 选择器上
                    SelectionKey selectionKey =
                            socketChannel.register(selector, SelectionKey.OP_READ);

                    // 余下为业务处理
                    Client client = new Client();
                    client.remoteAddress
                            = (InetSocketAddress) socketChannel.getRemoteAddress();
                    clientMap.put(socketChannel, client);

                } else if (key.isReadable()) { // 处理I/O读写事件

                    processData(key);
                }
                // NIO的特点只会累加，已选择的键的集合不会删除
                // 如果不删除，下一次又会被select函数选中
                it.remove();
            }

        }
    }

    /**
     * 处理客户端传输过来的数据
     */
    private void processData(SelectionKey key) throws IOException {
        Client client = clientMap.get(key.channel());

        SocketChannel socketChannel = (SocketChannel) key.channel();
        int num = 0;
        try {
            buffer.clear();
            while ((num = socketChannel.read(buffer)) > 0) {
                buffer.flip();
                //客户端发送过来的，首先是文件名
                if (null == client.fileName) {

                    // 文件名
                    String fileName = charset.decode(buffer).toString();

                    String destPath = "/Users/sherk/Documents/repository/NioDemo";;
                    File directory = new File(destPath);
                    if (!directory.exists()) {
                        directory.mkdir();
                    }
                    client.fileName = fileName;
                    String fullName = directory.getAbsolutePath()
                            + File.separatorChar + fileName;
                    System.out.println("NIO  传输目标文件：" + fullName);

                    File file = new File(fullName);
                    FileChannel fileChannel = new FileOutputStream(file).getChannel();
                    client.outChannel = fileChannel;


                }
                //客户端发送过来的，其次是文件长度
                else if (0 == client.fileLength) {
                    // 文件长度
                    long fileLength = buffer.getLong();
                    client.fileLength = fileLength;
                    client.startTime = System.currentTimeMillis();
                    System.out.println("NIO  传输开始：");
                }
                //客户端发送过来的，最后是文件内容
                else {
                    // 写入文件
                    client.outChannel.write(buffer);
                }
                buffer.clear();
            }
            key.cancel();
        } catch (IOException e) {
            key.cancel();
            e.printStackTrace();
            return;
        }
        // 调用close为-1 到达末尾
        if (num == -1) {
            client.outChannel.close();
            System.out.println("上传完毕");
            key.cancel();
            System.out.println("文件接收成功,File Name：" + client.fileName);
            System.out.println(" Size：" + client.fileLength);
            long endTime = System.currentTimeMillis();
            System.out.println("NIO IO 传输毫秒数：" + (endTime - client.startTime));
        }
    }


    /**
     * 入口
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        NioFileRecServer server = new NioFileRecServer();
        server.startServer();
    }

}
