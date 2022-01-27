package com.lxj.rapid.codec;

import lombok.Data;

import java.io.Serializable;

/**
 * rpc请求
 * @author Xingjing.Li
 * @since 2022/1/26
 */
@Data
public class RpcRequest implements Serializable {
    private String requestId;
    private String className;
    private String methodName;
    private Class<?> [] parameterTypes;
    private Object[] parameters;
}
