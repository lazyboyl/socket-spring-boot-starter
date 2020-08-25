package com.github.lazyboyl.socket.constant;

/**
 * 类描述： socket的配置枚举类
 *
 * @author linzef
 * @since 2020-08-25
 */
public enum SocketProperties {

    /**
     * 服务端口号
     */
    PORT("socket.port", "8499"),
    /**
     * boss的线程数
     */
    BOSSTHREAD("socket.thread.boss", "12"),
    /**
     * 工作线程数
     */
    WORKTHREAD("socket.thread.work", "12");

    private String key;

    private String defaultValue;

    SocketProperties(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
