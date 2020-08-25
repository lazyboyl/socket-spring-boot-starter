package com.github.lazyboyl.socket.server;

import com.github.lazyboyl.socket.annotation.SocketController;
import com.github.lazyboyl.socket.factory.NettyDefaultBeanFactory;
import com.github.lazyboyl.socket.factory.SocketControllerBeanFactory;
import com.github.lazyboyl.socket.factory.SocketInterfaceBeanFactory;
import com.github.lazyboyl.socket.listen.SocketHandlerListener;
import com.github.lazyboyl.socket.security.SocketSecurity;
import com.github.lazyboyl.socket.server.channel.SocketHandler;
import com.github.lazyboyl.socket.util.NettyScanner;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.logging.LogLevel;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

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
     * 注入spring的环境对象
     */
    @Autowired
    private Environment environment;

    /**
     * websocket扫描包的位置
     */
    @Value("${socket.scan.package}")
    private String[] socketScanPackage;

    /**
     * websocket的端口
     */
    @Value("${socket.port}")
    private Integer port;

    /**
     * netty的boss线程数
     */
    @Value("${socket.thread.boss}")
    private Integer bossThread;

    /**
     * netty的work线程数
     */
    @Value("${socket.thread.work}")
    private Integer workThread;

    /**
     * socket的controller的工厂扫描类
     */
    public static SocketControllerBeanFactory socketControllerBeanFactory;

    /**
     * socketSecurityBeanFactory的鉴权的工厂扫描类
     */
    public static SocketInterfaceBeanFactory socketSecurityBeanFactory;

    /**
     * socketHandlerListenerBeanFactory的监听的工厂扫描类
     */
    public static SocketInterfaceBeanFactory socketHandlerListenerBeanFactory;

    /**
     * 日志对象
     */
    private static Logger log = LoggerFactory.getLogger(NettySocketServer.class);

    /**
     * 功能描述： 为了保证和tomcat集成的时候启动netty导致线程卡住而导致启动失败
     *
     * @throws Exception
     */
    @PostConstruct
    public void init() throws Exception {
        initSocketConfig();
        ExecutorService socketSinglePool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        socketSinglePool.execute(() -> {
            try {
                start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 功能描述： 主入口方法
     *
     * @throws InterruptedException 端口绑定错误
     */
    public void start() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup boss = new NioEventLoopGroup(bossThread);
        EventLoopGroup work = new NioEventLoopGroup(workThread);
        bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        bootstrap.group(boss, work)
                .handler(new LoggingHandler(String.valueOf(LogLevel.DEBUG)))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) {
                        ChannelPipeline p = channel.pipeline();
                        channel.pipeline().addLast(new StringDecoder());
                        channel.pipeline().addLast(new StringEncoder());
                        p.addLast(new SocketHandler());
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
     * 功能描述： 初始化socket的类的扫描
     *
     * @throws Exception
     */
    protected void initSocketConfig() throws Exception {
        NettyScanner nettyScanner = new NettyScanner();
        for (String pack : socketScanPackage) {
            nettyScanner.initClasses(pack);
        }
        socketControllerBeanFactory = SocketControllerBeanFactory.getInstance();
        injectionBean(SocketController.class, socketControllerBeanFactory, nettyScanner);
        socketSecurityBeanFactory = SocketInterfaceBeanFactory.getInstance();
        injectionBean(SocketSecurity.class, socketSecurityBeanFactory, nettyScanner);
        socketHandlerListenerBeanFactory = SocketInterfaceBeanFactory.getInstance();
        injectionBean(SocketHandlerListener.class, socketHandlerListenerBeanFactory, nettyScanner);
    }

    /**
     * 功能描述： 实现相应的bean的注入
     *
     * @param cls          待注入的类型的class
     * @param factory      相应的工程
     * @param nettyScanner 扫描对象
     * @throws Exception 出错信息
     */
    protected void injectionBean(Class cls, NettyDefaultBeanFactory factory, NettyScanner nettyScanner) throws Exception {
        if (cls.isAnnotation()) {
            for (Class c : nettyScanner.getAnnotationClasses(cls)) {
                factory.registerBean(c, environment);
            }
        } else if (cls.isInterface()) {
            for (Class c : nettyScanner.getInterfaceClasses(cls)) {
                factory.registerBean(c, environment);
            }
        }

    }


    /**
     * 功能描述： 根据bean的名称来获取相应的bean
     *
     * @param beanName bean的名称
     * @return 返回相应的实例化的bean
     */
    public static Object getBean(String beanName) {
        return ac.getBean(beanName);
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
