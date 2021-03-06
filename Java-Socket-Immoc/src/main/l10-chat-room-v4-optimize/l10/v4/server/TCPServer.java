package l10.v4.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import l10.v4.clink.box.StringReceivePacket;
import l10.v4.clink.core.Connector;
import l10.v4.clink.core.ScheduleJob;
import l10.v4.clink.core.schedule.IdleTimeoutScheduleJob;
import l10.v4.clink.utils.CloseUtils;
import l10.v4.foo.Foo;
import l10.v4.foo.handler.ConnectorCloseChain;
import l10.v4.foo.handler.ConnectorHandler;
import l10.v4.foo.handler.ConnectorStringPacketChain;


/**
 * TCP 服务器，用于获取客户端连接，创建 ClientHandler 来处理客户端连接。
 *
 * @author Ztiany
 * Email ztiany3@gmail.com
 * Date 2018/11/1 21:17
 */
class TCPServer {

    private final int mPortServer;
    private final List<ConnectorHandler> mConnectorHandlers = new ArrayList<>();
    private final File mCachePath;//文件缓存路径
    private ServerAcceptor mAcceptor;//用于处理客户端连接
    private ServerSocketChannel mServerSocketChannel;
    private final Map<String, Group> groups = new HashMap<>();

    private final ServerStatistics mStatistics = new ServerStatistics();

    private Group.GroupMessageAdapter mGroupMessageAdapter = (handler, msg) -> {
        handler.send(msg);
        mStatistics.sendSize++;
    };

    private ServerAcceptor.AcceptListener mAcceptListener = new ServerAcceptor.AcceptListener() {
        @Override
        public void onNewSocketArrived(SocketChannel channel) {
            try {
                ConnectorHandler connectorHandler = new ConnectorHandler(channel, mCachePath);
                System.out.println(connectorHandler.getClientInfo() + ":Connected!");

                // 添加收到消息的处理责任链
                connectorHandler.getStringPacketChain()
                        .appendLast(mStatistics.statisticsChain())
                        .appendLast(new ParseCommandConnectorStringPacketChain());

                // 添加关闭链接时的责任链
                connectorHandler.getCloseChain().appendLast(new RemoveQueueOnConnectorClosedChain());

                /*客户端和服务器，谁的超时时间短谁就能发送心跳*/
                ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(10, TimeUnit.SECONDS, connectorHandler);
                connectorHandler.schedule(scheduleJob);

                //添加到连接管理中
                synchronized (mConnectorHandlers) {
                    mConnectorHandlers.add(connectorHandler);
                    System.out.println("当前客户端数量：" + mConnectorHandlers.size());
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("客户端链接异常：" + e.getMessage());
            }
        }
    };

    TCPServer(int portServer, File cachePath) {
        mPortServer = portServer;
        mCachePath = cachePath;
        //创建一个群
        this.groups.put(Foo.DEFAULT_GROUP_NAME, new Group(Foo.DEFAULT_GROUP_NAME, mGroupMessageAdapter));
    }

    /*启动服务器*/
    boolean start() {
        try {
            ServerAcceptor clientListener = new ServerAcceptor(mAcceptListener);

            mServerSocketChannel = ServerSocketChannel.open();
            mServerSocketChannel.configureBlocking(false);//配置非阻塞
            mServerSocketChannel.bind(new InetSocketAddress(mPortServer));
            mServerSocketChannel.register(clientListener.getSelector(), SelectionKey.OP_ACCEPT);

            System.out.println("服务器信息：" + mServerSocketChannel.getLocalAddress());

            clientListener.start();
            mAcceptor = clientListener;

            if (mAcceptor.awaitRunning()) {
                System.out.println("服务器准备就绪～");
                System.out.println("服务器信息：" + mServerSocketChannel.getLocalAddress().toString());
                return true;
            } else {
                System.out.println("启动异常！");
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /*给所有客户端发送消息*/
    void broadcast(String line) {
        line = "系统通知：" + line;
        ConnectorHandler[] connectorHandlers;
        synchronized (mConnectorHandlers) {
            connectorHandlers = mConnectorHandlers.toArray(new ConnectorHandler[0]);
        }
        for (ConnectorHandler connectorHandler : connectorHandlers) {
            connectorHandler.send(line);
        }
    }

    /*停止服务器*/
    void stop() {
        if (mAcceptor != null) {
            mAcceptor.exit();
        }

        ConnectorHandler[] connectorHandlers;

        synchronized (mConnectorHandlers) {
            connectorHandlers = mConnectorHandlers.toArray(new ConnectorHandler[0]);
            mConnectorHandlers.clear();
        }

        for (ConnectorHandler connectorHandler : connectorHandlers) {
            connectorHandler.exit();
        }

        CloseUtils.close(mServerSocketChannel);
    }

    /**
     * 获取当前的状态信息
     */
    Object[] getStatusString() {
        return new String[]{
                "客户端数量：" + mConnectorHandlers.size(),
                "发送数量：" + mStatistics.sendSize,
                "接收数量：" + mStatistics.receiveSize
        };
    }

    /*用于处理连接关闭*/
    private class RemoveQueueOnConnectorClosedChain extends ConnectorCloseChain {

        @Override
        protected boolean consume(ConnectorHandler handler, Connector connector) {
            synchronized (mConnectorHandlers) {
                mConnectorHandlers.remove(handler);
            }
            // 移除群聊的客户端
            Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
            group.removeMember(handler);
            return true;
        }

    }

    /*用于处理通过 String 发送的命令，如果不是命令，则返回给客户端*/
    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            String entity = stringReceivePacket.getEntity();
            switch (entity) {
                case Foo.COMMAND_GROUP_JOIN: {
                    Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                    if (group.addMember(handler)) {
                        mGroupMessageAdapter.sendMessageToClient(handler, "Join Group:" + group.getName());
                    }
                    return true;
                }
                case Foo.COMMAND_GROUP_LEAVE: {
                    Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                    if (group.removeMember(handler)) {
                        mGroupMessageAdapter.sendMessageToClient(handler, "Leave Group:" + group.getName());
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        protected boolean consumeAgain(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            // 捡漏的模式，当我们第一遍未消费，然后又没有加入到群，自然没有后续的节点消费，此时我们进行二次消费，返回发送过来的消息
            mGroupMessageAdapter.sendMessageToClient(handler, "server replay：" + stringReceivePacket.getEntity());
            return true;
        }

    }

}