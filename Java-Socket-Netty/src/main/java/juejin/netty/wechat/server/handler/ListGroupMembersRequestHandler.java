package juejin.netty.wechat.server.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import juejin.netty.wechat.common.protocol.request.ListGroupMembersRequestPacket;
import juejin.netty.wechat.common.protocol.response.ListGroupMembersResponsePacket;
import juejin.netty.wechat.common.session.Session;
import juejin.netty.wechat.utils.SessionUtil;

import java.util.ArrayList;
import java.util.List;

@ChannelHandler.Sharable
public class ListGroupMembersRequestHandler extends SimpleChannelInboundHandler<ListGroupMembersRequestPacket> {

    public static final ListGroupMembersRequestHandler INSTANCE = new ListGroupMembersRequestHandler();

    private ListGroupMembersRequestHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ListGroupMembersRequestPacket requestPacket) {
        ListGroupMembersResponsePacket responsePacket = new ListGroupMembersResponsePacket();

        // 1. 获取群的 ChannelGroup
        String groupId = requestPacket.getGroupId();
        ChannelGroup channelGroup = SessionUtil.getChannelGroup(groupId);

        // 2. 遍历群成员的 channel，对应的 session，构造群成员的信息
        if (channelGroup != null) {
            List<Session> sessionList = new ArrayList<>();
            for (Channel channel : channelGroup) {
                Session session = SessionUtil.getSession(channel);
                sessionList.add(session);
            }
            responsePacket.setGroupId(groupId);
            responsePacket.setSessionList(sessionList);
        } else {
            responsePacket.setSuccess(false);
            responsePacket.setReason("该群不存在");
        }

        // 3. 构建获取成员列表响应写回到客户端
        ctx.writeAndFlush(responsePacket);
    }

}