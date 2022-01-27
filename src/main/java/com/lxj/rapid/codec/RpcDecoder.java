package com.lxj.rapid.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author Xingjing.Li
 * @since 2022/1/27
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;

    public RpcDecoder(Class<?> rpcResponseClass) {
        this.genericClass = rpcResponseClass;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //数据包不足4个字节直接返回
        if (in.readableBytes() < 4){
            return;
        }
        //记录一下当前的字节
        in.markReaderIndex();
        //当前请求数据包的大小读出来
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength){
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        //
        Object obj = Serialization.deserialize(data, genericClass);
        //填充到buffer中
        out.add(obj);
    }
}
