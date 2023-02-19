package reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class EchoHandler implements Runnable {
    final SocketChannel channel;
    final SelectionKey sk;
    final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    static final int RECIEVING = 0, SENDING = 1;
    int state = RECIEVING;

    EchoHandler(Selector selector, SocketChannel c) throws IOException {
        channel = c;
        c.configureBlocking(false);
        // 取的选择键后，设置感兴趣的ip事件。
        sk = channel.register(selector, 0);

        // 将handler设置为选择键的附件

        sk.attach(this);
        // 第二步， 注册Read就绪事件
        sk.interestOps(SelectionKey.OP_READ);
        selector.wakeup();

    }

    public void run() {
        try {
            if (state == SENDING) {
                // 写入通道
                channel.write(byteBuffer);
                // 写完后准备开始从通道读，byteBuffer 切换成写模式
                byteBuffer.clear();
                // 写完后，注册read 就绪事件
                sk.interestOps(SelectionKey.OP_READ);
                // 写完后， 进入接收状态
                state = RECIEVING;
            } else if (state == RECIEVING) {
                int len = 0;
                while ((len = channel.read(byteBuffer)) > 0) {
                    System.out.println(new String(byteBuffer.array(), 0, len));
                }
                // 读完后准备开始写入通道， byteBuffer 切换成读模式
                byteBuffer.flip();
                //读完后，注册write就绪事件
                sk.interestOps(SelectionKey.OP_WRITE);
                //读完后,进入发送的状态
                state = SENDING;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
