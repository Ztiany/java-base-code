package foo.handler;

import clink.box.StringReceivePacket;

/**
 * 默认 String 接收节点，不做任何事情。
 */
class DefaultNonConnectorStringPacketChain extends ConnectorStringPacketChain {

    @Override
    protected boolean consume(ConnectorHandler handler, StringReceivePacket packet) {
        return false;
    }

}
