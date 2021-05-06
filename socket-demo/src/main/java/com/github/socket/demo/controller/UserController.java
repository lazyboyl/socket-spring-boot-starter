package com.github.socket.demo.controller;

import com.github.lazyboyl.socket.annotation.SocketController;
import com.github.lazyboyl.socket.annotation.SocketRequestMapping;

import java.util.Map;

/**
 * @author linzf
 * @since 2020/8/25
 * 类描述：
 */
@SocketController
@SocketRequestMapping("/user/")
public class UserController {

    @SocketRequestMapping("login")
    public Map login(Map<String, Object> user) {
        System.out.println(user);
        user.put("care","abc");
        throw new RuntimeException("出错了");
//        return user;
    }


}
