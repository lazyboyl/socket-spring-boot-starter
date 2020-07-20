package com.github.lazyboyl.socket.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.logging.LogLevel;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;

/**
 * @author linzf
 * @since 2020/7/20
 * 类描述：
 */
@Component
public class NettySocketServer implements ApplicationContextAware {

    /**
     * 定义spring的ApplicationContext对象
     */
    private static ApplicationContext ac = null;

    /**
     * 日志对象
     */
    private static Logger log = LoggerFactory.getLogger(NettySocketServer.class);

    /**
     * 功能描述： 主入口方法
     *
     * @throws InterruptedException 端口绑定错误
     */
    @PostConstruct
    public void start() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup work = new NioEventLoopGroup();
        bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        bootstrap.group(boss, work)
                .handler(new LoggingHandler(String.valueOf(LogLevel.DEBUG)))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) {
                        ChannelPipeline p = channel.pipeline();
                        p.addLast("compressor", new HttpContentCompressor());
                    }
                });
        ChannelFuture f = bootstrap.bind(new InetSocketAddress(8499)).sync();
        f.addListener(future -> {
            if (future.isSuccess()) {
                log.info("------------------------端口" + 8499 + "的服务启动成功------------------------");
            } else {
                log.info("------------------------端口" + 8499 + "的服务启动失败------------------------");
            }
        });
        f.channel().closeFuture().sync();
    }

    /**
     * @param ac applicationContext对象
     * @throws BeansException bean的出错信息
     */
    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        ac = ac;
    }
}
