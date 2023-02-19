package socketDemo;

import sun.awt.windows.ThemeReader;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Time;
import java.util.concurrent.*;

/**
 * 文件传输客户端
 */
public class BioSendFileClient extends Socket {

    private Socket socket;

    public BioSendFileClient() throws Exception {
        super("127.0.0.1", 9090);
        this.socket = this;
        System.out.println("client port: " + socket.getLocalPort() + " connect server successfully");
    }

    public void sendFile(int i) throws Exception {
        File file = new File("/Users/sherk/Documents/repository/NioDemo/text.txt");
        try (FileInputStream fis = new FileInputStream(file);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
            dos.writeUTF(i+"_copy_" + file.getName());
            dos.writeLong(file.length());
            dos.flush();
            System.out.println("开始传输文件");
            byte[] bytes = new byte[1024];
            int len = 0;
            long progress = 0;
            while ((len = fis.read(bytes, 0, bytes.length)) != -1) {
                dos.write(bytes, 0, len);
                dos.flush();
                progress += len;
                System.out.println("| " + (100 * progress / file.length()) + "% |");
            }

        }

        System.out.println("----文件传输完成----");
        socket.close();
    }

    /**
     * 入口
     *
     * @param args
     */
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = new ThreadPoolExecutor(
                10,
                10,
                1,
                TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(5, true),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        try {


            BioSendFileClient client = new BioSendFileClient(); // 启动客户端连接
            long startTime = System.currentTimeMillis() / 1000L;
            System.out.println("开始时间：" + startTime);
            for (int i = 0; i < 10; i++) {
                final int  num= i;
                executorService.execute(() -> {
                            try {
                                client.sendFile(num);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        while (!executorService.isTerminated()) {
            Thread.sleep(1000);
        }
        long endTime = System.currentTimeMillis() / 1000L;
        System.out.println("结束时间：" + endTime);


    }



}

