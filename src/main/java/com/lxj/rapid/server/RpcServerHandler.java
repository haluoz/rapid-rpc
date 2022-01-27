package com.lxj.rapid.server;

import com.lxj.rapid.codec.RpcRequest;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Xingjing.Li
 * @since 2022/1/27
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private Map<String, Object> handlerMap;
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        //1解析RpcRequest
        //2从handlerMap中找到具体接口key所绑定的bean实例
        //3通过cglib反射调用，具体方法传递相关执行参数执行逻辑即即可
        //4返回响应信息给调用方
    }

    public RpcServerHandler(Map<String, Object> handlerMap){
        this.handlerMap = handlerMap;
    }
}
