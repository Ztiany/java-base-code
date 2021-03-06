package l9.v2.clink.core;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

import l9.v2.clink.box.BytesReceivePacket;
import l9.v2.clink.box.FileReceivePacket;
import l9.v2.clink.box.StringReceivePacket;
import l9.v2.clink.box.StringSendPacket;
import l9.v2.clink.impl.SocketChannelAdapter;
import l9.v2.clink.impl.async.AsyncReceiveDispatcher;
import l9.v2.clink.impl.async.AsyncSendDispatcher;

/**
 * 代表一个 SocketChannel 连接，用于调用 Sender 和  Receiver 执行读写操作。
 */
public abstract class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    /**
     * 该连接的唯一标识
     */
    protected final UUID key = UUID.randomUUID();

    /**
     * 实际的连接
     */
    private SocketChannel channel;

    /**
     * 数据发送者
     */
    private Sender sender;

    /**
     * 数据接收者
     */
    private Receiver receiver;

    /**
     * 数据发送调度者
     */
    private SendDispatcher sendDispatcher;

    /**
     * 数据接收调度者
     */
    private ReceiveDispatcher receiveDispatcher;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;

        IoContext ioContext = IoContext.get();
        SocketChannelAdapter socketChannelAdapter = new SocketChannelAdapter(channel, ioContext.getIoProvider(), this);

        this.sender = socketChannelAdapter;
        this.receiver = socketChannelAdapter;

        sendDispatcher = new AsyncSendDispatcher(sender);
        receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);

        // 启动接收
        receiveDispatcher.start();
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        //no op
    }

    public void send(String message) {
        sendDispatcher.send(new StringSendPacket(message));
    }

    public void send(SendPacket packet) {
        sendDispatcher.send(packet);
    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        channel.close();
    }

    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            onReceiveNewPacket(packet);
        }

        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, int length) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile());
                case Packet.TYPE_STREAM_DIRECT:
                    return new BytesReceivePacket(length);
                default:
                    throw new UnsupportedOperationException("Unsupported packet type:" + type);
            }
        }
    };

    protected abstract File createNewReceiveFile();

    protected void onReceiveNewPacket(ReceivePacket packet) {
        System.out.println(key.toString() + " : [New Packet]-Type : " + packet.getType() + ", Length:" + packet.getLength());
    }

}