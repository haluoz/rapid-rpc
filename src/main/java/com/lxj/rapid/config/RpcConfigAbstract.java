package com.lxj.rapid.config;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Xingjing.Li
 * @since 2022/1/27
 */
public class RpcConfigAbstract {
    private AtomicInteger generator = new AtomicInteger();

    protected String id;

    protected String interfaceClass = null;
    //服务的调用方（consumer端特有的属性）
    protected Class<?> proxyClass = null;

    public String getId(){
        if (StringUtils.isEmpty(id)){
            id = "rapid-rpc-gen-"+ generator.getAndIncrement();
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(String interfaceClass) {
        this.interfaceClass = interfaceClass;
    }
}
