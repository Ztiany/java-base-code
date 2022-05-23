package juejin.netty.wechat.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import juejin.netty.wechat.client.handler.LoginResponseHandler;
import juejin.netty.wechat.client.handler.MessageResponseHandler;
import juejin.netty.wechat.codec.PacketDecoder;
import juejin.netty.wechat.codec.PacketEncoder;
import juejin.netty.wechat.protocol.PacketCodec;
import juejin.netty.wechat.protocol.request.MessageRequestPacket;
import juejin.netty.wechat.utils.LoginUtil;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static juejin.netty.wechat.Constant.*;

public class NettyClient {

    public static void main(String[] args) {
        new NettyClient().start();
    }

    private void start() {
        // 创建事件循环
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        // 创建启动器
        Bootstrap bootstrap = new Bootstrap();
        // 执行配置
        bootstrap
                // 1.指定线程模型
                .group(workerGroup)
                // 2.指定 IO 类型为 NIO
                .channel(NioSocketChannel.class)
                // 3.IO 处理逻辑
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(@NotNull SocketChannel ch) {
                        ch.pipeline()
                                //in-bound
                                .addLast(PacketCodec.newProtocolDecoder())//拆包
                                .addLast(new PacketDecoder())//解码
                                .addLast(new MessageResponseHandler())//消息响应处理
                                .addLast(new LoginResponseHandler())//登录响应处理
                                //out-bound
                                .addLast(new PacketEncoder());
                    }
                });

        /*
        attr() 方法可以给客户端 Channel，也就是 NioSocketChannel 绑定自定义属性，然后我们可以通过 channel.attr() 取出这个属性，
        下面码我们指定我们客户端 Channel 的一个 clientName 属性，属性值为 nettyClient，其实说白了就是给 NioSocketChannel 维护一个 map 而已，
        后续在这个 NioSocketChannel 通过参数传来传去的时候，就可以通过他来取出设置的属性，非常方便。
         */
        bootstrap.attr(AttributeKey.newInstance("clientName"), "nettyClient");
        // option() 方法可以给连接设置一些 TCP 底层相关的属性
        bootstrap
                // ChannelOption.CONNECT_TIMEOUT_MILLIS 表示连接的超时时间，超过这个时间还是建立不上的话则代表连接失败
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                // ChannelOption.SO_KEEPALIVE 表示是否开启 TCP 底层心跳机制，true 为开启
                .option(ChannelOption.SO_KEEPALIVE, true)
                // ChannelOption.TCP_NODELAY 表示是否开始 Nagle 算法，true 表示关闭，false 表示开启，通俗地说，如果要求高实时性，有数据发送时就马上发送，就设置为 true 关闭，
                // 如果需要减少发送次数减少网络交互，就设置为 false 开启
                .option(ChannelOption.TCP_NODELAY, true);

        // 4.建立连接
        doConnect(bootstrap, HOST, PORT, 5);
    }

    /**
     * 通常情况下，连接建立失败不会立即重新连接，而是会通过一个指数退避的方式，比如每隔 1 秒、2 秒、4 秒、8 秒，以 2 的幂次来建立连接，然后到达一定次数之后就放弃连接
     */
    private void doConnect(Bootstrap bootstrap, String host, int port, int retry) {
        // 4.建立连接
        bootstrap.connect(host, port).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("连接成功!");
                Channel channel = ((ChannelFuture) future).channel();
                // 连接成功之后，启动控制台线程
                startConsoleThread(channel);
            } else if (retry == 0) {
                System.err.println("重试次数已用完，放弃连接！");
            } else {
                // 第几次重连
                int order = (MAX_RETRY - retry) + 1;
                // 本次重连的间隔
                int delay = 1 << order;
                System.err.println(new Date() + ": 连接失败，第" + order + "次重连……");
                /*
                    定时任务是调用 bootstrap.config().group().schedule(), 其中 bootstrap.config() 这个方法返回的是 BootstrapConfig，其是对 Bootstrap 配置参数的抽象，
                    然后 bootstrap.config().group() 返回的就是我们在一开始的时候配置的线程模型 workerGroup，调 workerGroup 的 schedule 方法即可实现定时任务逻辑。
                 */
                bootstrap.config().group().schedule(() -> doConnect(bootstrap, host, port, retry - 1), delay, TimeUnit.SECONDS);
            }
        });
    }

    private void startConsoleThread(Channel channel) {
        new Thread(() -> readConsoleAndSend(channel)).start();
    }

    private void readConsoleAndSend(Channel channel) {
        Scanner scanner = new Scanner(System.in);
        String line;
        System.out.println("输入消息发送至服务端: ");
        while (!(line = scanner.nextLine()).equals("quit")) {
            System.out.printf("read form console: %s\r\n", line);
            if (LoginUtil.hasLogin(channel)) {
                MessageRequestPacket packet = new MessageRequestPacket();
                packet.setMessage(line);
                channel.writeAndFlush(packet);
            } else {
                System.out.println("客户端还未登录，请稍等...");
            }
        }
        scanner.close();
    }

    private void writeLotsData(Channel channel) {
        for (int i = 0; i < 100; i++) {
            MessageRequestPacket packet = new MessageRequestPacket();
            packet.setMessage("你好，我是你的谁啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊");
            channel.writeAndFlush(packet);
        }
    }

}