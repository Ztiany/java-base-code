package l9.v2.clink.box;

import java.io.ByteArrayInputStream;

import l9.v2.clink.core.SendPacket;

/**
 * 以字节形式发送的包，可以应对大部分数据类型。
 *
 * @author Ztiany
 * Email ztiany3@gmail.com
 * Date 2018/11/24 0:11
 */
public class BytesSendPacket extends SendPacket<ByteArrayInputStream> {

    private final byte[] mBytes;

    public BytesSendPacket(byte[] bytes) {
        this.mLength = bytes.length;
        mBytes = bytes;
    }

    @Override
    public byte getType() {
        return TYPE_MEMORY_BYTES;
    }

    @Override
    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(mBytes);
    }

}
