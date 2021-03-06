package com.github.lazyboyl.socket.factory;


import com.github.lazyboyl.socket.annotation.SocketRequestMapping;
import com.github.lazyboyl.socket.annotation.SocketRequestParam;
import com.github.lazyboyl.socket.beans.NettyBeanDefinition;
import com.github.lazyboyl.socket.beans.NettyMethodDefinition;
import org.springframework.core.env.Environment;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linzf
 * @since 2020/7/13
 * 类描述： socket请求的工厂类
 */
public class SocketControllerBeanFactory extends NettyDefaultBeanFactory {

    /**
     * 定义webSocketControllerBeanFactory对象
     */
    private static SocketControllerBeanFactory webSocketControllerBeanFactory;

    /**
     * 使得WebSocketControllerBeanFactory无法在外部new对象
     */
    private SocketControllerBeanFactory() {
        super();
    }

    /**
     * 功能描述： 获取webSocketControllerBeanFactory的单例
     *
     * @return 返回webSocketControllerBeanFactory的单例对象
     */
    public static SocketControllerBeanFactory getInstance() {
        if (webSocketControllerBeanFactory == null) {
            webSocketControllerBeanFactory = new SocketControllerBeanFactory();
        }
        return webSocketControllerBeanFactory;
    }

    /**
     * 功能描述： 注册netty的bean的类的扫描
     *
     * @param c           需要进行处理的类的对象
     * @param environment 环境对象
     */
    @Override
    protected void registerNettyBeanDefinition(Class c, Environment environment) {
        try {
            NettyBeanDefinition nettyBeanDefinition = new NettyBeanDefinition();
            nettyBeanDefinition.setClassName(c.getName());
            // 获取类上的注解的集合
            nettyBeanDefinition.setClassAnnotation(c.getAnnotations());
            // 初始化对象，包括构造函数的对象的初始化
            Object o = doInstance(c, environment);
            // 注册对象的响应的路径
            registerNettyBeanDefinitionMappingPath(c, nettyBeanDefinition);
            // 初始化类上的方法
            registerNettyMethodDefinition(c, nettyBeanDefinition);
            // 初始化类上的属性
            registerNettyFieldDefinition(c, o, nettyBeanDefinition, environment);
            // 实例化类
            nettyBeanDefinition.setObject(o);
            nettyBeanDefinitionSetAdd(c.getName());
            nettyBeanDefinitionMapPut(c.getName(), nettyBeanDefinition);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 功能描述： 注册对象的响应的路径
     *
     * @param c                   class对象
     * @param nettyBeanDefinition netty的bean的信息
     */
    protected void registerNettyBeanDefinitionMappingPath(Class c, NettyBeanDefinition nettyBeanDefinition) {
        Annotation a = c.getAnnotation(SocketRequestMapping.class);
        if (a != null) {
            SocketRequestMapping nrm = (SocketRequestMapping) a;
            String[] value = nrm.value();
            if (value.length > 0) {
                nettyBeanDefinition.setMappingPath(value);
            }
        }
    }

    /**
     * 功能描述： 注册对象的方法信息
     *
     * @param c                   class对象
     * @param nettyBeanDefinition netty的bean的信息
     */
    protected void registerNettyMethodDefinition(Class c, NettyBeanDefinition nettyBeanDefinition) {
        Method[] methods = c.getDeclaredMethods();
        Map<String, NettyMethodDefinition> methodMap = new HashMap<>(methods.length);
        for (Method m : methods) {
            NettyMethodDefinition nettyMethodDefinition = new NettyMethodDefinition();
            nettyMethodDefinition.setMethod(m);
            nettyMethodDefinition.setMethodAnnotation(m.getAnnotations());
            nettyMethodDefinition.setParameterTypesClass(m.getParameterTypes());
            nettyMethodDefinition.setReturnClass(m.getReturnType());
            nettyMethodDefinition.setMethodName(m.getName());
            nettyMethodDefinition.setBeanName(c.getName());
            nettyMethodDefinition.setParameters(m.getParameters());
            Annotation[][] parameterAnnotations = m.getParameterAnnotations();
            String[] requestParamName = new String[parameterAnnotations.length];
            for (int i = 0; i < parameterAnnotations.length; i++) {
                if (parameterAnnotations[i].length == 0) {
                    requestParamName[i] = "";
                } else {
                    for (int j = 0; j < parameterAnnotations[i].length; j++) {
                        if (parameterAnnotations[i][j] instanceof SocketRequestParam) {
                            SocketRequestParam wsrp = (SocketRequestParam) parameterAnnotations[i][j];
                            requestParamName[i] = wsrp.name();
                        }
                    }
                }
            }
            nettyMethodDefinition.setRequestParamName(requestParamName);
            methodMap.put(c.getName() + "." + m.getName(), nettyMethodDefinition);
            SocketRequestMapping a = m.getAnnotation(SocketRequestMapping.class);
            if (a == null) {
                continue;
            }
            parseNettyRequestMapping(c, nettyMethodDefinition, a, nettyBeanDefinition);
        }
        nettyBeanDefinition.setMethodMap(methodMap);
    }

    /**
     * 功能描述： 解析响应的地址
     *
     * @param c                     class对象
     * @param nettyMethodDefinition bean的方法的实体对象
     * @param a                     WebSocketRequestMapping对象
     * @param nettyBeanDefinition   bean的保存实体对象
     */
    protected void parseNettyRequestMapping(Class c, NettyMethodDefinition nettyMethodDefinition, SocketRequestMapping a, NettyBeanDefinition nettyBeanDefinition) {
        String[] value = a.value();
        String[] actionPath = new String[0];
        if (value.length > 0) {
            actionPath = value;
        }
        parseUrl(actionPath);
        String[] mappingPath = nettyBeanDefinition.getMappingPath();
        if (mappingPath != null && mappingPath.length > 0) {
            parseUrl(mappingPath);
            for (String s : mappingPath) {
                for (String p : actionPath) {
                    if (methodIsInit(s + p)) {
                        throw new RuntimeException(c.getName() + "类的" + nettyMethodDefinition.getMethod().getName() + "方法上存在重复定义的响应地址！");
                    }
                    nettyMethodDefinitionSetAdd(s + p);
                    nettyMethodDefinitionMapPut(s + p, nettyMethodDefinition);
                }
            }
        } else {
            for (String p : actionPath) {
                if (methodIsInit(p)) {
                    throw new RuntimeException(c.getName() + "类的" + nettyMethodDefinition.getMethod().getName() + "方法上存在重复定义的响应地址！");
                }
                nettyMethodDefinitionSetAdd(p);
                nettyMethodDefinitionMapPut(p, nettyMethodDefinition);
            }
        }
    }

    /**
     * 功能描述： 重新处理响应的url地址，例如 /dict/add处理成为 dict/add
     *
     * @param url 需重新处理的地址
     */
    protected void parseUrl(String[] url) {
        for (int i = 0; i < url.length; i++) {
            if (url[i].substring(0, 1).equals("/")) {
                url[i] = url[i].substring(1);
            }
            if (!url[i].substring(url[i].length() - 1).equals("/")) {
                url[i] = url[i] + "/";
            }
        }
    }


}
