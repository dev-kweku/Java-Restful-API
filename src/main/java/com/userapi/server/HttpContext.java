package com.userapi.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpContext {
    public final String method;
    public final String path;
    public final String rawQuery;
    public final Map<String,String> pathParams=new HashMap<>();
    public final String body;

    private int statusCode=200;
    private String responseBody="";
    private final OutputStream out;


    public HttpContext(String method, String path, String rawQuery,
                       String body, OutputStream out) {
        this.method = method;
        this.path = path;
        this.rawQuery = rawQuery;
        this.body = body;
        this.out = out;
    }

    public static HttpContext parse(Socket socket) throws IOException{
        var in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        var out=socket.getOutputStream();

        String requestLine=in.readLine();
        if(requestLine==null || requestLine.isBlank()) return null;

        String[] parts=requestLine.split(" ");
        String method=parts[0];
        String fullPath= URLDecoder.decode(parts[1], StandardCharsets.UTF_8);

        String path=fullPath,rawQuery="";
        if(fullPath.contains("?")){
            path=fullPath.substring(0,fullPath.indexOf("?"));
            rawQuery=fullPath.substring(fullPath.indexOf("?") + 1);
        }

        int contentLength=0;
        String line;
        while((line=in.readLine()) != null && !line.isBlank()){
            if(line.toLowerCase().startsWith("content-length: ")){
                contentLength=Integer.parseInt(line.split(":")[1].trim());
            }
        }

        String body="";
        if(contentLength>0){
            char[] buf=new char[contentLength];
            in.read(buf,0,contentLength);
            body=new String(buf);
        }
        return new HttpContext(method,path,body,rawQuery,out);


    }

    public void status(int code){this.statusCode=code;}
    public void json(String json){this.responseBody=json;}

    public void flush() throws IOException{
        String response="HTTP/1.1" +statusCode + " " + statusText() + "\r\n" + "Content-Type: application/json\r\n" + "Content-Length: " +
                responseBody.getBytes(StandardCharsets.UTF_8).length +  " \r\n" + "Connection: close\r\n" + "\r\n" + responseBody;
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private String statusText(){
        return switch (statusCode){
            case 200->"OK";
            case 201->"Created";
            case 204->"No Content";
            case 400->"Bad Request";
            case 404->"Not Found";
            case 409->"Conflict";
            case 500->"Internal Server Error";
            default -> "Unknown";
        };
    }
}
