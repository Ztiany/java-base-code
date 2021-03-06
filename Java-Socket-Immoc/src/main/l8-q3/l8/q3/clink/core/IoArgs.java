package l8.q3.clink.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * IO参数，用于执行实际的异步读写操作，读写操作状态将会以异步回调的形式通知。
 *
 * @author Ztiany
 * Email ztiany3@gmail.com
 * Date 2018/11/8 22:34
 */
public class IoArgs {

    //TODO，容量改为 4 个字节，模拟单消息不完整问题
    private byte[] byteBuffer = new byte[4];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    public int read(SocketChannel socketChannel) throws IOException {
        buffer.clear();
        return socketChannel.read(buffer);
    }

    public int write(SocketChannel socketChannel) throws IOException {
        return socketChannel.write(buffer);
    }

    //TODO，去掉减去换行符的逻辑
    public String bufferString() {
        return new String(byteBuffer, 0, buffer.position() /*- CharUtils.LINE_BREAK_LENGTH*/ /*丢弃换行符*/);
    }

    public interface IoArgsEventListener {
        void onStarted(IoArgs args);

        void onCompleted(IoArgs args);
    }

}
