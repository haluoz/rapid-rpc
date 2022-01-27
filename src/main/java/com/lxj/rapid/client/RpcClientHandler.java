package com.lxj.rapid.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketAddress;


/**
 * 实际的业务处理器
 * @author Xingjing.Li
 * @since 2022/1/22
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<Object> {
    private Channel channel;
    private SocketAddress remotePeer;

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remotePeer = channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {

    }

    public SocketAddress getRemotePeer() {
        return remotePeer;
    }

    public void setRemotePeer(SocketAddress remotePeer) {
        this.remotePeer = remotePeer;
    }

    //netty提供了一种主动连接的方式，发送一个Unpooled.EMPTY_BUFFER
    //这样我们的ChannelFutureListener的close事件就会监听到并关闭通道
    public void close(){
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
