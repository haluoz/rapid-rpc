package com.lxj.rapid.client;

import com.lxj.rapid.codec.RpcDecoder;
import com.lxj.rapid.codec.RpcEncoder;
import com.lxj.rapid.codec.RpcRequest;
import com.lxj.rapid.codec.RpcResponse;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Rpc编解码
 * @author Xingjing.Li
 * @since 2022/1/25
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        //编解码的handler
        pipeline.addLast(new RpcEncoder(RpcRequest.class));
        pipeline.addLast(new RpcDecoder(RpcResponse.class));
        //实际的业务处理器
        pipeline.addLast(new RpcClientHandler());
        pipeline.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 0));
    }
}
