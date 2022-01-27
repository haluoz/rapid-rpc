package com.lxj.rapid.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RPC连接管理器
 * @author Xingjing.Li
 * @since 2022/1/22
 */
@Slf4j
public class RpcConnectManager {
    private long connectTimeoutMills = 6000;
    private volatile boolean isRunning = true;
    private volatile AtomicInteger handlerIndex = new AtomicInteger(0);
    private static volatile RpcConnectManager rpcConnectManager = new RpcConnectManager();
    private Map<InetSocketAddress, RpcClientHandler> connectedHandlerMap = new ConcurrentHashMap();
    //用于异步提交连接方法
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1024));
    private NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
    //所有连接成功的地址所对应任务执行器
    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlerList = new CopyOnWriteArrayList<>();
    private ReentrantLock connectedLock = new ReentrantLock();
    private Condition condition = connectedLock.newCondition();
    public static RpcConnectManager getInstance(){
        return rpcConnectManager;
    }
    private RpcConnectManager(){}

    //1.异步创建连接 线程池真正的发起连接，连接成功的监听，连接失败的监听
    //2.对于连接进来的资源做一个缓存（做一个管理）updateConnectedServer
    public void connect(final String serverAddress){
        List<String> addresses = Arrays.asList(serverAddress.split(","));
        updateConnectedServer(addresses);
    }

    //更新缓存信息并异步发起连接
    public void updateConnectedServer(List<String> addresses){
        if (CollectionUtils.isNotEmpty(addresses)){
            //1解析addresses地址并临时存储到newAllServerNodeSet
            HashSet<InetSocketAddress> newAllServerNodeSet = new HashSet<InetSocketAddress>();
            for (int i = 0; i < addresses.size(); i++) {
                String[] array = addresses.get(i).split(":");
                if (array.length == 2){
                    String ip = array[0];
                    Integer port = Integer.parseInt(array[1]);
                    final InetSocketAddress remotePeer = new InetSocketAddress(ip, port);
                    newAllServerNodeSet.add(remotePeer);
                }
            }
            //2调用建立连接方法发起远程连接
            for (InetSocketAddress serverNodeAddress : newAllServerNodeSet) {
                if (!connectedHandlerMap.keySet().contains(serverNodeAddress)) {
                    connectAsync(serverNodeAddress);
                }
            }
            //3如果列表中不存在的连接地址，需要从缓存中移除
            for (int i = 0; i < connectedHandlerList.size(); i++) {
                RpcClientHandler rpcClientHandler = connectedHandlerList.get(i);
                SocketAddress remotePeer = rpcClientHandler.getRemotePeer();
                if (!newAllServerNodeSet.contains(remotePeer)){
                    log.info("remove invalid  server node {}", remotePeer);
                    RpcClientHandler handler = connectedHandlerMap.get(remotePeer);
                    if (handler != null){
                        handler.close();
                        connectedHandlerMap.remove(remotePeer);
                    }
                    connectedHandlerList.remove(rpcClientHandler);
                }
            }
        }else {
            log.error("no available server address");
            //清楚所有的缓存信息
            clearConnected();
        }
    }

    private void connectAsync(final InetSocketAddress remotePeer) {
        threadPoolExecutor.submit(new Runnable(){
            public void run() {
                Bootstrap b = new Bootstrap();
                b
                .group(nioEventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new RpcClientInitializer());
                connect(b, remotePeer);
            }
        });
    }

    private void connect(Bootstrap b, final InetSocketAddress remotePeer) {
        //1真正建立连接
        final ChannelFuture channelFuture = b.connect(remotePeer);
        //2连接失败的时候添加监听，移除缓存发起重连的操作
        channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("channelFuture.channel().closeFuture operation completed, remotePeer{}", remotePeer);
                future.channel().eventLoop().schedule(new Runnable() {
                    public void run() {
                        log.warn("connect fail, to reconnect");
                        clearConnected();
                        connect(b, remotePeer);
                    }
                }, 3, TimeUnit.SECONDS);
            }
        });
        //3连接成功的时候添加监听，把新的连接放入缓存中
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("connect to remote server successfully, remotePeer {}", remotePeer);
                    RpcClientHandler rpcClientHandler = future.channel().pipeline().get(RpcClientHandler.class);
                    addHandler(rpcClientHandler);
                }
            }
        });
    }

    /**
     * 添加rpcClientHandler到缓存中
     * connectedHandlerList connectedHandlerMap
     * @param rpcClientHandler
     */
    private void addHandler(RpcClientHandler rpcClientHandler) {
        connectedHandlerList.add(rpcClientHandler);
        InetSocketAddress remoteAddress = (InetSocketAddress) rpcClientHandler.getRemotePeer();
        connectedHandlerMap.put(remoteAddress, rpcClientHandler);
        //signalAvailableHandler 唤醒可用的业务执行器
        signalAvailableHandler();
    }

    //唤醒另外一端的线程（阻塞）告知有新线程进入
    private void signalAvailableHandler() {
        connectedLock.lock();
        try{
            condition.signalAll();
        }finally {
            connectedLock.unlock();
        }
    }

    /**
     * 连接失败时及时释放资源，清空缓存
     * 先删除connectedHandlerMap中的数据
     * 再删除connectedHandlerList
     */
    private void clearConnected() {
        for (RpcClientHandler rpcClientHandler : connectedHandlerList) {
            // rpcClientHandler找到InetSocketAddress，从connectedHandlerMap指定的
            SocketAddress remotePeer = rpcClientHandler.getRemotePeer();
            RpcClientHandler handler = connectedHandlerMap.get(remotePeer);
            if (handler!= null) {
                handler.close();
                connectedHandlerMap.remove(remotePeer);
            }
        }
        connectedHandlerList.clear();
    }

    /**
     * 等待新连接
     * @return
     * @throws InterruptedException
     */
    private boolean waitingForAvailableHandler() throws InterruptedException {
        connectedLock.lock();
        try {
            return condition.await(this.connectTimeoutMills, TimeUnit.MILLISECONDS);
        } finally {
            connectedLock.unlock();
        }
    }

    /**
     * 选择一个实际的业务处理器
     * @return
     */
    public RpcClientHandler chooseHandler(){
        CopyOnWriteArrayList<RpcClientHandler> handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlerList.clone();
        int size = handlers.size();
        while ( isRunning && size <= 0){
            try {
                boolean available = waitingForAvailableHandler();
                if (available){
                    handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlerList.clone();
                    size = handlers.size();

                }
            } catch (InterruptedException e) {
                log.error("waiting for available node is interrupted");
                throw new RuntimeException("", e);
            }
        }
        if (!isRunning){
            return null;
        }
        //最终使用取模的方式取得其中一个业务处理器进行实际的业务处理
        int index = (handlerIndex.getAndAdd(1) + size ) % size;
        return  handlers.get(index);
    }

    /**
     * 关闭的方法
     */
    public void stop(){
        isRunning = false;
        for (int i = 0; i < connectedHandlerList.size(); i++) {
            RpcClientHandler handler = connectedHandlerList.get(i);
            handler.close();
            connectedHandlerMap.remove(handler);
        }
        //这里要调用一下唤醒操作
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        nioEventLoopGroup.shutdownGracefully();
    }

    /**
     * 发起重连的方法
     * @param handler
     * @param remotePeer
     */
    public void reconnect(final RpcClientHandler handler, final SocketAddress remotePeer){
        if (handler != null){
            handler.close();
            connectedHandlerList.remove(handler);
            connectedHandlerMap.remove(remotePeer);
        }
        connectAsync((InetSocketAddress) remotePeer);
    }
}
