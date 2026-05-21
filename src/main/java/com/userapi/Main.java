package com.userapi;

import com.userapi.server.HttpServer;
import com.userapi.server.Router;

public class Main {
    public static void main(String[] args) throws Exception {

        var router=new Router();
        new HttpServer(router,8080).start();
        System.out.println("Server running on port http://localhost:8080");
    }
}
