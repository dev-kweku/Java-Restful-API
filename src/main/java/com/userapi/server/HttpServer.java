package com.userapi.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
        private final Router router;
        private final int port;
        private final ExecutorService threadPool;


        public HttpServer(Router router, int port){
            this.router=router;
            this.port=port;
            this.threadPool= Executors.newFixedThreadPool(50);
        }

        public void start()throws Exception{
            try(ServerSocket serverSocket=new ServerSocket(port)){
                serverSocket.setReuseAddress(true);

                while (true){
                    Socket clientSocket=serverSocket.accept();
                    threadPool.submit(()->{
                        try(clientSocket){
                            handleRequest(clientSocket);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    });

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
                System.out.println("Error handling request :" + e.getMessage());
            }
        }
}
