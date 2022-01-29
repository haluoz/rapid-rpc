package com.lxj.rapid.client.proxy;

import com.lxj.rapid.client.RpcClientHandler;
import com.lxj.rapid.client.RpcConnectManager;
import com.lxj.rapid.client.RpcFuture;
import com.lxj.rapid.codec.RpcRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Xingjing.Li
 * @since 2022/1/28
 */
public class RpcProxyImpl<T> implements InvocationHandler, RpcAsyncProxy {
    private Class<T> clazz;
    private long timeout;

    public RpcProxyImpl(Class<T> interfaceClass, long timeout) {
        this.clazz = interfaceClass;
        this.timeout = timeout;
    }

    /**
     * 同步阻塞式
     * invoke代理接口调用方式
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //1设置请求对象
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(UUID.randomUUID().toString());
        rpcRequest.setMethodName(method.getDeclaringClass().getName());
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParameterTypes(method.getParameterTypes());
        rpcRequest.setParameters(args);
        //2选择一个合适的client任务处理器
        RpcClientHandler handler = RpcConnectManager.getInstance().chooseHandler();
        //3发送真正的客户端请求 返回结果
        RpcFuture future = handler.sendRequest(rpcRequest);
        return future.get(1, TimeUnit.SECONDS);
    }

    /**
     * 异步调用
     * @param funcName
     * @param args
     * @return
     */
    @Override
    public RpcFuture call(String funcName, Object... args) {
        //1设置请求对象
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(UUID.randomUUID().toString());
        rpcRequest.setMethodName(this.clazz.getName());
        rpcRequest.setMethodName(funcName);
        // TODO 对应的参数类型应该 类类型 + 方法名称 用反射得到parameterTypes
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = getClassType(args[i]);
        }
        rpcRequest.setParameterTypes(parameterTypes);
        rpcRequest.setParameters(args);
        RpcClientHandler handler = RpcConnectManager.getInstance().chooseHandler();
        RpcFuture rpcFuture = handler.sendRequest(rpcRequest);
        return rpcFuture;
    }

    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        if (typeName.equals("java.lang.Integer")) {
            return Integer.TYPE;
        } else if (typeName.equals("java.lang.Long")) {
            return Long.TYPE;
        } else if (typeName.equals("java.lang.Float")) {
            return Float.TYPE;
        } else if (typeName.equals("java.lang.Double")) {
            return Double.TYPE;
        } else if (typeName.equals("java.lang.Character")) {
            return Character.TYPE;
        } else if (typeName.equals("java.lang.Boolean")) {
            return Boolean.TYPE;
        } else if (typeName.equals("java.lang.Short")) {
            return Short.TYPE;
        } else if (typeName.equals("java.lang.Byte")) {
            return Byte.TYPE;
        }
        return classType;
    }
}
