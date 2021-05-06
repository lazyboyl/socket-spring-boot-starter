package com.github.socket.demo;

import com.github.lazyboyl.socket.integrate.EnableSocketServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableSocketServer(socketScanPackage = {"com.github.socket.demo"})
public class SocketDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocketDemoApplication.class, args);
    }

}
