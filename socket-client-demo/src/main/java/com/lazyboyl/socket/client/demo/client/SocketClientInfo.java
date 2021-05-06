package com.lazyboyl.socket.client.demo.client;

import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author linzf
 * @since 2021/4/26
 * 类描述：
 */
public class SocketClientInfo {

    public static Map<String, ChannelHandlerContext> clientMap = new HashMap<>();

}
