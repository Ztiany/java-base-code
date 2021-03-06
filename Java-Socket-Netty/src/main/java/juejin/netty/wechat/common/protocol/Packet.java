package juejin.netty.wechat.common.protocol;

import lombok.Data;

@Data
public abstract class Packet {

    private transient Byte version = 1;

    public abstract Byte getCommand();

}
