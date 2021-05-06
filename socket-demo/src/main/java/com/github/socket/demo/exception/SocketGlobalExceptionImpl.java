package com.github.socket.demo.exception;

import com.github.lazyboyl.socket.exception.SocketGlobalException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author linzf
 * @since 2021/5/6
 * 类描述：
 */
public class SocketGlobalExceptionImpl implements SocketGlobalException {
    @Override
    public int level() {
        return 0;
    }

    @Override
    public Object errorHandler(Exception e) {
        Map<String,Object> r = new HashMap<>();
        r.put("code",200);
        r.put("result","失败了");
        return r;
    }
}
