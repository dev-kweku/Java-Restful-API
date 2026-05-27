package com.userapi.server;

import com.github.javafaker.App;
import com.userapi.util.AppLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpServer {
        private final Router router;
        private final int port;
        private final ExecutorService threadPool;
        private static final Logger log= (Logger) AppLogger.get(HttpServer.class);
        private volatile boolean running=true;

        public HttpServer(Router router, int port){
            this.router=router;
            this.port=port;
            this.threadPool= Executors.newFixedThreadPool(50);
        }

        public void start()throws IOException {
            try(ServerSocket serverSocket=new ServerSocket(port)){
                serverSocket.setReuseAddress(true);
                log.info("HTTP Server listening on port {} ${port}");

                while(running){
                    Socket clientSocket=serverSocket.accept();
                    threadPool.submit(()->handleRequest(clientSocket));
                }
            }
        }

        public void handleRequest(Socket socket){
            try(socket){
                var ctx=HttpContext.parse(socket);
                if(ctx==null) return;
                router.dispatch(ctx);
                ctx.flush();
            }catch(Exception e){
//                System.out.println("Error handling request :" + e.getMessage());
                log.info("Socket handling error: {}");
            }
        }

        public void stop(){
            running=false;
            threadPool.shutdown();
        }
}
