package com.lxj.rapid.client.proxy;

import com.lxj.rapid.client.RpcFuture;

/**
 * 异步代理接口
 * @author Xingjing.Li
 * @since 2022/1/28
 */
public interface RpcAsyncProxy {
    RpcFuture call(String funcName, Object... args);
}
