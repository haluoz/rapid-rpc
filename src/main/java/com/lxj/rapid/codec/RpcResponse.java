package com.lxj.rapid.codec;

import lombok.Data;

import java.io.Serializable;

/**
 * rpc响应
 * @author Xingjing.Li
 * @since 2022/1/26
 */
@Data
public class RpcResponse implements Serializable {
    private String requestId;
    private Object result;
    private Throwable throwable;
}
