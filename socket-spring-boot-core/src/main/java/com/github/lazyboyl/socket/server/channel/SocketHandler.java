package com.github.lazyboyl.socket.server.channel;

import com.github.lazyboyl.socket.beans.NettyBeanDefinition;
import com.github.lazyboyl.socket.beans.NettyMethodDefinition;
import com.github.lazyboyl.socket.entity.SocketRequest;
import com.github.lazyboyl.socket.entity.SocketResponse;
import com.github.lazyboyl.socket.server.NettySocketServer;
import com.github.lazyboyl.socket.util.ClassUtil;
import com.github.lazyboyl.socket.util.JsonUtils;
import com.github.lazyboyl.socket.util.SocketUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

/**
 * @author linzf
 * @since 2020/7/20
 * 类描述：
 */
public class SocketHandler extends SimpleChannelInboundHandler<String> {


    /**
     * 日志对象
     */
    private static Logger log = LoggerFactory.getLogger(SocketHandler.class);

    /**
     * 功能描述： 关闭通道的监听
     *
     * @param ctx 通道对象
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("与客户端断开连接，通道关闭！通道ID是：{}", ctx.channel().id().asLongText());
        List<NettyBeanDefinition> nettyBeanDefinitions = NettySocketServer.socketHandlerListenerBeanFactory.getNettyBeanDefinitionList();
        doHandlerListener(ctx, nettyBeanDefinitions, "channelInactive");
    }

    /**
     * 功能描述： 连接通道的监听
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("连接开启！通道ID是：{}", ctx.channel().id().asLongText());
        List<NettyBeanDefinition> nettyBeanDefinitions = NettySocketServer.socketHandlerListenerBeanFactory.getNettyBeanDefinitionList();
        doHandlerListener(ctx, nettyBeanDefinitions, "handleShake");
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        log.debug("请求的数据是：{}", msg);
        try {
            SocketRequest socketRequest = JsonUtils.jsonToPojo(msg, SocketRequest.class);
            socketRequest.setSocketId(ctx.channel().id().asLongText());
            // 实现鉴权的拦截
            List<NettyBeanDefinition> nettyBeanDefinitions = NettySocketServer.socketSecurityBeanFactory.getNettyBeanDefinitionList();
            if (!doAuthentication(ctx, nettyBeanDefinitions, socketRequest)) {
                return;
            }
            String uri = socketRequest.getUrl();
            uri = parseUri(uri);
            NettyMethodDefinition nettyMethodDefinition = NettySocketServer.socketControllerBeanFactory.getNettyMethodDefinition(uri);
            if (nettyMethodDefinition == null) {
                SocketUtil.writeAndFlush(ctx.channel(), new SocketResponse(HttpResponseStatus.NOT_FOUND.code(), "无此方法！"));
            } else {
                NettyBeanDefinition nettyBeanDefinition = NettySocketServer.socketControllerBeanFactory.getNettyBeanDefinition(nettyMethodDefinition.getBeanName());
                if (nettyBeanDefinition == null) {
                    SocketUtil.writeAndFlush(ctx.channel(), new SocketResponse(HttpResponseStatus.NOT_FOUND.code(), "无此方法！"));
                } else {
                    invokeMethod(ctx, nettyMethodDefinition, nettyBeanDefinition, socketRequest.getParams());
                }
            }
        } catch (Exception e) {
            List<NettyBeanDefinition> nettyBeanDefinitions = NettySocketServer.socketGolbalExceptionBeanFactory.getNettyBeanDefinitionList();
            doExceptionHandler(ctx, nettyBeanDefinitions, "errorHandler", e);
        }
    }


    /**
     * 功能描述： 全局异常的处理的实现
     *
     * @param ctx                  当前通道对象
     * @param nettyBeanDefinitions 相应的bean的集合
     * @param action               全局异常的方法
     * @param exception            异常信息
     */
    protected void doExceptionHandler(ChannelHandlerContext ctx, List<NettyBeanDefinition> nettyBeanDefinitions, String action, Exception exception) {
        Object resultObj = null;
        for (NettyBeanDefinition nbd : nettyBeanDefinitions) {
            for (Map.Entry<String, NettyMethodDefinition> entry : nbd.getMethodMap().entrySet()) {
                String[] k1s = entry.getKey().split("\\.");
                Object[] obj = new Object[]{exception};
                if (k1s[k1s.length - 1].equals(action)) {
                    try {
                        resultObj = entry.getValue().getMethod().invoke(nbd.getObject(), obj);
                        ctx.channel().writeAndFlush(JsonUtils.objToJson(resultObj));
                        return;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (resultObj == null) {
            exception.printStackTrace();
        }
    }

    /**
     * 功能描述：通道关闭/开启的时候的监听事件
     *
     * @param ctx                  当前关闭的通道对象
     * @param nettyBeanDefinitions 待响应的方法
     * @param action               当前请求的操作类型
     */
    protected void doHandlerListener(ChannelHandlerContext ctx, List<NettyBeanDefinition> nettyBeanDefinitions, String action) {
        for (NettyBeanDefinition nbd : nettyBeanDefinitions) {
            for (Map.Entry<String, NettyMethodDefinition> entry : nbd.getMethodMap().entrySet()) {
                String[] k1s = entry.getKey().split("\\.");
                Object[] obj = new Object[]{ctx};
                if (k1s[k1s.length - 1].equals(action)) {
                    try {
                        entry.getValue().getMethod().invoke(nbd.getObject(), obj);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * 功能描述： 实现前置的鉴权的逻辑
     *
     * @param ctx                  请求对象
     * @param nettyBeanDefinitions 定义的鉴权的实现类
     * @param socketRequest        请求的对象
     * @return true：鉴权通过，false：鉴权不通过
     */
    protected Boolean doAuthentication(ChannelHandlerContext ctx, List<NettyBeanDefinition> nettyBeanDefinitions, SocketRequest socketRequest) {
        for (NettyBeanDefinition nbd : nettyBeanDefinitions) {
            for (Map.Entry<String, NettyMethodDefinition> entry : nbd.getMethodMap().entrySet()) {
                String[] k1s = entry.getKey().split("\\.");
                Object[] obj = new Object[]{ctx, socketRequest};
                if (k1s[k1s.length - 1].equals("authentication")) {
                    try {
                        Boolean isContinue = (Boolean) entry.getValue().getMethod().invoke(nbd.getObject(), obj);
                        if (!isContinue) {
                            return false;
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }

    /**
     * 功能描述： 重新处理uri使其符合map的映射标准
     *
     * @param uri 请求地址
     * @return 处理以后的请求地址
     */
    protected String parseUri(String uri) {
        if ("/".equals(uri.substring(0, 1))) {
            uri = uri.substring(1);
        }
        if (!"/".equals(uri.substring(uri.length() - 1))) {
            uri = uri + "/";
        }
        return uri;
    }

    /**
     * 功能描述： 实现反射调用方法
     *
     * @param ctx                   netty通道镀锡
     * @param nettyMethodDefinition 方法对象
     * @param nettyBeanDefinition   类对象
     * @param paramMap              请求参数
     */
    protected void invokeMethod(ChannelHandlerContext ctx, NettyMethodDefinition nettyMethodDefinition, NettyBeanDefinition nettyBeanDefinition, Map<String, Object> paramMap) {
        Object object = null;
        if (nettyMethodDefinition.getParameters().length == 0) {
            try {
                object = nettyMethodDefinition.getMethod().invoke(nettyBeanDefinition.getObject());
            } catch (Exception e) {
                List<NettyBeanDefinition> nettyBeanDefinitions = NettySocketServer.socketGolbalExceptionBeanFactory.getNettyBeanDefinitionList();
                doExceptionHandler(ctx, nettyBeanDefinitions, "errorHandler", e);
                return;
            }
        } else {
            Parameter[] ps = nettyMethodDefinition.getParameters();
            Object[] obj = new Object[ps.length];
            Class[] parameterTypesClass = nettyMethodDefinition.getParameterTypesClass();
            String[] requestParamName = nettyMethodDefinition.getRequestParamName();
            for (int i = 0; i < ps.length; i++) {
                Parameter p = ps[i];
                if (isMyClass(parameterTypesClass[i])) {
                    if (parameterTypesClass[i].getName().equals("java.util.Map")) {
                        obj[i] = paramMap;
                    } else if (parameterTypesClass[i].getName().equals("java.util.List")) {
                        obj[i] = JsonUtils.objToList(paramMap.get(p.getName()), ClassUtil.getClass((p.getParameterizedType().getTypeName().replace("java.util.List<", "").replace(">", ""))));
                    } else if ("java.util.".indexOf(parameterTypesClass[i].getName()) != -1) {
                        SocketUtil.writeAndFlush(ctx.channel(), new SocketResponse(HttpResponseStatus.BAD_REQUEST.code(), "java.util系列暂时只支持map和list，其他类型暂不支持。"));
                    } else {
                        if ("".equals(requestParamName[i])) {
                            obj[i] = paramMap.get(p.getName());
                        } else {
                            obj[i] = paramMap.get(requestParamName[i]);
                        }
                    }
                } else {
                    if (parameterTypesClass[i].getName().equals(HttpHeaders.class.getName())) {
                        obj[i] = paramMap.get("headers");
                    } else if (parameterTypesClass[i].getName().equals(ChannelHandlerContext.class.getName())) {
                        obj[i] = ctx;
                    } else {
                        obj[i] = JsonUtils.map2object(paramMap, parameterTypesClass[i]);
                    }
                }
            }
            try {
                object = nettyMethodDefinition.getMethod().invoke(nettyBeanDefinition.getObject(), obj);
            } catch (Exception e) {
                List<NettyBeanDefinition> nettyBeanDefinitions = NettySocketServer.socketGolbalExceptionBeanFactory.getNettyBeanDefinitionList();
                doExceptionHandler(ctx, nettyBeanDefinitions, "errorHandler", e);
                return;
            }
        }
        if (!"void".equals(nettyMethodDefinition.getReturnClass().getName())) {
            SocketUtil.writeAndFlush(ctx.channel(), new SocketResponse(HttpResponseStatus.OK.code(), "", object));
        }
    }

    /**
     * 功能描述： 判断当前的class是否是自己定义的class
     *
     * @param s 需要判断的class对象
     * @return true: JDK本身的类；false：自己定义的类
     */
    protected Boolean isMyClass(Class s) {
        if (s.getClassLoader() == null) {
            return true;
        } else {
            return false;
        }
    }

}
