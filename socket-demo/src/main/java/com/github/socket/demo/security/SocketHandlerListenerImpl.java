package com.github.socket.demo.security;

import com.github.lazyboyl.socket.listen.SocketHandlerListener;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author linzf
 * @since 2020/8/25
 * 类描述：
 */
public class SocketHandlerListenerImpl implements SocketHandlerListener {

    @Override
    public int level() {
        return 0;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("失去连接" + ctx.channel().id().asLongText());
    }

    @Override
    public void handleShake(ChannelHandlerContext ctx) {
        System.out.println("获取连接" + ctx.channel().id().asLongText());
    }
}
