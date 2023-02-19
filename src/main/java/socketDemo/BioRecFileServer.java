package socketDemo;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *  当前的处理方式是一个请求来了用一个线程处理。
 *  当单个线程处理当时候遇到io 仍然是阻塞的。
 *  IO 交互流程：
 *  1用户发起系统调用进行IO.
 *  2. 内核将数据移动的内核缓冲区
 *  3.内核缓冲区将数据传输到用户缓冲区。
 *  4。 用户程序进行业务逻辑
 *
 *  bio 用户在执行第一步后， 在2，3两步会阻塞住。
 */
public class BioRecFileServer extends ServerSocket {

    public BioRecFileServer() throws Exception {
        super(9090);
    }

    /**
     * 启动服务端
     * 使用线程处理每个客户端传输的文件
     *
     * @throws Exception
     */
    public void startServer() throws Exception {
        while (true) {
            // server尝试接收其他Socket的连接请求，server的accept方法是阻塞式的
            System.out.println("server listen on " + 9090);
            Socket socket = this.accept();
            /**
             * 我们的服务端处理客户端的连接请求是同步进行的， 每次接收到来自客户端的连接请求后，
             * 都要先跟当前的客户端通信完之后才能再处理下一个连接请求。 这在并发比较多的情况下会严重影响程序的性能，
             * 为此，我们可以把它改为如下这种异步处理与客户端通信的方式。来一个请求就开启一个线程处理
             */
            // 每接收到一个Socket就建立一个新的线程来处理它
            new Thread(new Task(socket)).start();
        }
    }

    /**
     * 处理客户端传输过来的文件线程类
     */
    class Task implements Runnable {

        private Socket socket;

        private FileOutputStream fos;

        public Task(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                // 文件名和长度
                String fileName = dis.readUTF();
                long fileLength = dis.readLong();
                File directory = new File("/Users/sherk/Documents/repository/NioDemo");
                File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
                fos = new FileOutputStream(file);
                long startTime = System.currentTimeMillis();
                // 开始接收文件
                System.out.println("----开始接收文件-----");
                byte[] bytes = new byte[1024];
                int length = 0;
                while ((length = dis.read(bytes, 0, bytes.length)) != -1)
                {
                    fos.write(bytes, 0, length);
                    fos.flush();
                }
                long endTime = System.currentTimeMillis();
                System.out.println("----结束接收文件-----");
            } catch (Exception e)  {
                e.printStackTrace();
            } finally {
                try {
                    fos.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
    /**
     * 入口
     *
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            // 启动服务端
            BioRecFileServer server = new BioRecFileServer();
            server.startServer();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}


