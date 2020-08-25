package com.github.lazyboyl.socket.util;

import com.github.lazyboyl.socket.entity.SocketResponse;
import io.netty.channel.Channel;

/**
 * @author linzf
 * @since 2020/7/14
 * 类描述：
 */
public class SocketUtil {

    /**
     * 功能描述： 像前端推送信息
     * @param channel 当前的通道
     * @param socketResponse 返回的结果
     */
    public static void writeAndFlush(Channel channel, SocketResponse socketResponse) {
        channel.writeAndFlush(JsonUtils.objToJson(socketResponse));
    }

}
