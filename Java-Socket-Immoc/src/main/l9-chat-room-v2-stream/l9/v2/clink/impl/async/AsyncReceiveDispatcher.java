package l9.v2.clink.impl.async;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import l9.v2.clink.core.IoArgs;
import l9.v2.clink.core.Packet;
import l9.v2.clink.core.ReceiveDispatcher;
import l9.v2.clink.core.ReceivePacket;
import l9.v2.clink.core.Receiver;
import l9.v2.clink.utils.CloseUtils;

/**
 * @author Ztiany
 * Email ztiany3@gmail.com
 * Date 2018/11/18 17:00
 */
public class AsyncReceiveDispatcher implements ReceiveDispatcher {

    private final AtomicBoolean mIsClosed = new AtomicBoolean(false);
    private final Receiver mReceiver;
    private final ReceivePacketCallback mReceivePacketCallback;
    private final IoArgs mIoArgs = new IoArgs();

    /**
     * 临时存储当前正在接受的包
     */
    private ReceivePacket<?, ?> mPacketTemp;

    /**
     * 当前正在接受的数据通道
     */
    private WritableByteChannel mWritableByteChannelTemp;

    /**
     * 临时存储当前正在接受的包的总长度
     */
    private int mTotal;

    /**
     * 临时存储当前正在接受的包的已读长度
     */
    private long mPosition;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback receivePacketCallback) {
        mReceiver = receiver;
        mReceiver.setReceiveListener(newIoArgsEventProcessor());
        mReceivePacketCallback = receivePacketCallback;
    }

    @Override
    public void stop() {
    }

    @Override
    public void start() {
        registerReceive();
    }

    private void registerReceive() {
        try {
            //表示希望开始接受数据
            mReceiver.postReceiveAsync();
        } catch (IOException e) {
            e.printStackTrace();
            //失败则关闭自己
            closeAndNotify();
        }
    }

    /*解析包*/
    private void assemblePacket(IoArgs args) {
        //是一条新的消息
        if (mPacketTemp == null) {
            int length = args.readLength();
            //根据数据长度确定包的类型
            byte type = length >= 200 ? Packet.TYPE_STREAM_FILE : Packet.TYPE_MEMORY_STRING;
            //根据包长度需求，创建一个StringReceivePacket
            mPacketTemp = mReceivePacketCallback.onArrivedNewPacket(type, length);
            //使用 packet 的流创建一个 Channel
            mWritableByteChannelTemp = Channels.newChannel(mPacketTemp.open());
            //初始化容器和位置标识
            mTotal = length;
            mPosition = 0;
        }

        try {
            //写入到我们的 mWritableByteChannel 中
            int readCount = args.writeTo(mWritableByteChannelTemp);
            mPosition += readCount;
            // 检查是否已完成一份Packet接收
            if (mPosition == mTotal) {
                completePacket(true);
                mPacketTemp = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 完成数据接收操作
     *
     * @param isSuccess 是否成功
     */
    private void completePacket(@SuppressWarnings("unused") boolean isSuccess) {
        ReceivePacket receivePacket = mPacketTemp;
        CloseUtils.close(mPacketTemp);
        mPacketTemp = null;

        CloseUtils.close(mWritableByteChannelTemp);
        mWritableByteChannelTemp = null;

        if (receivePacket != null) {
            mReceivePacketCallback.onReceivePacketCompleted(receivePacket);
        }
    }


    private IoArgs.IoArgsEventProcessor newIoArgsEventProcessor() {

        return new IoArgs.IoArgsEventProcessor() {

            @Override
            public IoArgs provideIoArgs() {
                IoArgs args = mIoArgs;
                int receiveSize;
                if (mPacketTemp == null) {//说明是一个新的消息的读取，先获取长度
                    receiveSize = 4;//按照约定，用4个字节表示长度
                } else {//说明还是读取之前没有读完的消息，则接收的长度应该是，总长度-以读取的长度，同时还要考虑 args 的容量
                    receiveSize = (int) Math.min(mTotal - mPosition, args.capacity());
                }
                //设置本次接收数据的长度
                args.limit(receiveSize);
                return args;
            }

            @Override
            public void consumeFailed(IoArgs ioArgs, Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onConsumeCompleted(IoArgs args) {
                //完成了单次（非阻塞）接收，则解析包
                assemblePacket(args);
                //然后继续接收数据
                registerReceive();
            }
        };

    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void close() {
        if (mIsClosed.compareAndSet(false, true)) {
            completePacket(false);
        }
    }

}