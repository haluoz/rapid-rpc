package com.lxj.rapid.config.provider;

import com.lxj.rapid.server.RpcServer;

import java.util.List;

/**
 * 服务器端启动配置类
 * @author Xingjing.Li
 * @since 2022/1/29
 */
public class RpcServerConfig {
    private final String host = "127.0.0.1";
    protected int port;
    private List<ProviderConfig> providerConfigs;
    private RpcServer rpcServer = null;

    public RpcServerConfig(List<ProviderConfig> providerConfigs) {
        this.providerConfigs = providerConfigs;
    }

    public void exporter(){
        if (rpcServer == null){
            try {
                rpcServer = new RpcServer(host+":"+port);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (ProviderConfig pc : providerConfigs) {
                rpcServer.registerProcessor(pc);
            }
        }
        //zookeeper
    }

    public List<ProviderConfig> getProviderConfigs() {
        return providerConfigs;
    }

    public void setProviderConfigs(List<ProviderConfig> providerConfigs) {
        this.providerConfigs = providerConfigs;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
