package com.lxj.rapid.client;

/**
 * @author Xingjing.Li
 * @since 2022/1/28
 */
public interface RpcCallback {
    void success(Object result);
    void failure(Throwable cause);
}
