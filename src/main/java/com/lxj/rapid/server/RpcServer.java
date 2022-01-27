package com.lxj.rapid.server;

import com.lxj.rapid.codec.RpcDecoder;
import com.lxj.rapid.codec.RpcEncoder;
import com.lxj.rapid.codec.RpcRequest;
import com.lxj.rapid.codec.RpcResponse;
import com.lxj.rapid.config.provider.ProviderConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Xingjing.Li
 * @since 2022/1/27
 */
@Slf4j
public class RpcServer {
    private String serverAddress;
    private EventLoopGroup bossGroup = new NioEventLoopGroup();
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private volatile Map<String, Object> handlerMap = new HashMap<>();

    public void start() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new RpcEncoder(RpcResponse.class));
                        pipeline.addLast(new RpcDecoder(RpcRequest.class));
                        pipeline.addLast(new RpcServerHandler(handlerMap));
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 0));
                    }
                });
        String[] address = serverAddress.split(":");
        String ip = address[0];
        int port = Integer.parseInt(address[1]);
        //异步
        ChannelFuture channelFuture = serverBootstrap.bind(ip, port).sync();
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()){
                    log.info("server success binding to {}", serverAddress);
                }else{
                    log.info("server start fail");
                }
            }
        });
        // 同步
//        try {
//            channelFuture.await(5000, TimeUnit.MILLISECONDS);
//            if(channelFuture.isSuccess()){
//                log.info("start rpc successfully {}", serverAddress);
//            }
//        } catch (InterruptedException e) {
//            log.error("start rpc fail", e);
//        }
    }

    public void registerProcessor(ProviderConfig providerConfig){
        //key service接口命名
        //value service下的具体实现类
        handlerMap.put(providerConfig.getInterfaceClass(), providerConfig.getRef());
    }

    public void close(){
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public RpcServer(String serverAddress) throws InterruptedException {
        this.serverAddress = serverAddress;
        start();
    }
}
