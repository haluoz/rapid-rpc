package com.lxj.rapid.client;

/**
 * @author Xingjing.Li
 * @since 2022/1/26
 */
public class RpcClient {
    private String serverAddress;
    private long timeout;

    public RpcClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        connect();
    }

    private void connect() {
        RpcConnectManager.getInstance().connect(serverAddress);
    }

    private void close(){
        RpcConnectManager.getInstance().stop();
    }
}
