package com.lazyboyl.socket.client.demo.controller;

import com.lazyboyl.socket.client.demo.client.SocketClientInfo;
import com.lazyboyl.socket.client.demo.util.JsonUtils;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author linzf
 * @since 2021/4/26
 * 类描述：
 */
@RestController
@RequestMapping("client")
public class ClientController {

    @GetMapping("login")
    public void login() {
        Map<String,Object> map = new HashMap<>();
        map.put("url","/user/login/");
        Map<String,Object> params = new HashMap<>();
        params.put("userId","测试数据");
        map.put("params",params);
        for (Map.Entry<String, ChannelHandlerContext> entry : SocketClientInfo.clientMap.entrySet()) {
            entry.getValue().writeAndFlush(JsonUtils.objToJson(map));
        }
    }

}
