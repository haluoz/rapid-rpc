package com.lxj.rapid.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 编码器
 * @author Xingjing.Li
 * @since 2022/1/27
 */
public class RpcEncoder extends MessageToByteEncoder<Object> {

    private Class<?> genericClass;

    public RpcEncoder(Class<RpcRequest> rpcRequestClass) {
        this.genericClass = rpcRequestClass;
    }

    /**
     * 编码器要做的事情
     * 1把对应的java对象进行编码
     * 2把内容填充到buffer
     * 3写出到server端
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (genericClass.isInstance(msg)){
            byte[] data = Serialization.serialize(msg);
            //消息分为：1包头数据包长度 2包体(数据包内容)
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }


}
