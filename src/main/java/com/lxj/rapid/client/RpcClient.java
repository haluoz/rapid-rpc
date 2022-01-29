package com.lxj.rapid.client;

import com.lxj.rapid.client.proxy.RpcAsyncProxy;
import com.lxj.rapid.client.proxy.RpcProxyImpl;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Xingjing.Li
 * @since 2022/1/26
 */
public class RpcClient {
    private String serverAddress;
    private long timeout;
    private final Map<Class<?>, Object> syncProxyInstanceMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> asyncProxyInstanceMap = new ConcurrentHashMap<>();

    public RpcClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        connect();
    }

    public RpcClient() {

    }

    private void connect() {
        RpcConnectManager.getInstance().connect(serverAddress);
    }

    private void close(){
        RpcConnectManager.getInstance().stop();
    }

    /**
     * 同步调用方式
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public <T> T invokeSync(Class<T> interfaceClass){
        if (syncProxyInstanceMap.containsKey(interfaceClass)){
            return (T)syncProxyInstanceMap.get(interfaceClass);
        }
        Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, new RpcProxyImpl<>(interfaceClass, timeout));
        syncProxyInstanceMap.put(interfaceClass, proxy);
        return (T)proxy;
    }

    /**
     * 异步调用方式
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public <T> T invokeAsync(Class<T> interfaceClass){
        if (asyncProxyInstanceMap.containsKey(interfaceClass)){
            return (T)asyncProxyInstanceMap.get(interfaceClass);
        }
        RpcProxyImpl<?> rpcProxy = new RpcProxyImpl<>(interfaceClass, timeout);
        syncProxyInstanceMap.put(interfaceClass, rpcProxy);
        return (T)rpcProxy;
    }


    public void initClient(String serverAddress, int timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        connect();
    }
}
