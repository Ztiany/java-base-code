package l8.q4.clink.core;

/**
 * 接收包的定义，不同的数据类型对应不同的 ReceivePack 实现。
 *
 * @author Ztiany
 * Email ztiany3@gmail.com
 * Date 2018/11/18 16:38
 */
public abstract class ReceivePacket extends Packet {

    /**
     * 使用该包接收数据。
     *
     * @param bytes 数据源
     * @param count 需要 save 的长度
     */
    public abstract void save(byte[] bytes, int count);

}
