package com.example.server;

import ratpack.server.RatpackServer;

/**
 * RatPack Server.
 */
public class App {
    public static void main(String[] args) throws Exception {
        Integer serverPort = Integer.parseInt(System.getProperty("port", "8080"));
        RatpackServer.start(server ->
                server
                        .serverConfig(config -> config.port(serverPort))
                        .handlers(hander ->
                        hander.all(ctx -> ctx.render("Hello world"))
                )
        ).start();
    }
}
