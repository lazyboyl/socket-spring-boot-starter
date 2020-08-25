package com.github.lazyboyl.socket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类描述：socket的请求匹配
 *
 * @author linzef
 * @since 2020-08-25
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SocketRequestMapping {

    String[] value() default {};

}
