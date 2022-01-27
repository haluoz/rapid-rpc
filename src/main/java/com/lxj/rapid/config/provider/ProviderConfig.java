package com.lxj.rapid.config.provider;

import com.lxj.rapid.config.RpcConfigAbstract;

/**
 * 接口名称
 * 程序对象
 * @author Xingjing.Li
 * @since 2022/1/27
 */
public class ProviderConfig extends RpcConfigAbstract {
    protected Object ref;

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }
}
