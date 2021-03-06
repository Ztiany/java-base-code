package l11.v4.clink.box;

import java.io.ByteArrayInputStream;

import l11.v4.clink.core.SendPacket;

/**
 * @author Ztiany
 * Email ztiany3@gmail.com
 * Date 2018/11/24 0:11
 */
public class BytesSendPacket extends SendPacket<ByteArrayInputStream> {

    private final byte[] mBytes;

    public BytesSendPacket( byte[] bytes) {
        this.length = bytes.length;
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
