package com.lxj.rapid.server;

import com.lxj.rapid.codec.RpcRequest;
import com.lxj.rapid.codec.RpcResponse;
import io.netty.channel.*;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Xingjing.Li
 * @since 2022/1/27
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private Map<String, Object> handlerMap;

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16,  600, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1024));

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                RpcResponse response = new RpcResponse();
                response.setRequestId(rpcRequest.getRequestId());
                try {
                    //1解析RpcRequest
                    //2从handlerMap中找到具体接口key所绑定的bean实例
                    //3通过cglib反射调用，具体方法传递相关执行参数执行逻辑即即可
                    //4返回响应信息给调用方
                    Object result = handle(rpcRequest);
                    response.setResult(result);
                }catch (Throwable e){
                    response.setResult(e);
                    log.error("rpc server handle request error", e);
                }
                ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()){
                            log.info("result execute successfully");
                        }
                    }
                });
            }
        });
    }

    /**
     * 解析request请求通过反射调用具体的本地服务
     * @param request
     * @return
     * @throws InvocationTargetException
     */
    private Object handle(RpcRequest request) throws InvocationTargetException {
        String className = request.getClassName();
        Object serviceRef = handlerMap.get(className);
        Class<?> serviceClass = serviceRef.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();
        // JDK reflection
        // Object o = serviceClass.newInstance();
        //CGLIB
        FastClass serviceFastClass = FastClass.create(serviceClass);
        FastMethod method = serviceFastClass.getMethod(methodName, parameterTypes);
        return method.invoke(serviceRef, parameters);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        log.error("server caught throwable ", cause);
    }

    public RpcServerHandler(Map<String, Object> handlerMap){
        this.handlerMap = handlerMap;
    }
}
