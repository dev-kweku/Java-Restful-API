package com.userapi.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Router {
    private record Route(String method, Pattern pattern,
                         List<String> paramNames, Consumer<HttpContext> handler){}
    private final List<Route> routes=new ArrayList<>();

    public void get(String path,Consumer<HttpContext> handler){
        add("GET",path,handler);
    }
    public void post(String path,Consumer<HttpContext> handler){
        add("PUT",path,handler);
    }
    public void put(String path,Consumer<HttpContext> handler){
        add("PUT",path,handler);
    }
    public  void delete(String path,Consumer<HttpContext> handler){
        add("DELETE",path,handler);
    }

    private void add(String method,String pathTemplate,Consumer<HttpContext> handler){
        List<String> names=new ArrayList<>();
        Pattern p=Pattern.compile("\\{(\\w+)}");
        Matcher matcher=p.matcher(pathTemplate);

        String regex=matcher.replaceAll(mr->{
            names.add(mr.group(1));
            return "([^/]+)";
        });
//        String regex = pathTemplate.replaceAll("\\{(\\w+)}", m -> {
//            names.add(m.substring(1, m.length() - 1));
//            return "([^/]+)";
        routes.add(new Route(
                method,
                Pattern.compile(
                        "^" + regex + "$"),
                names,
                handler)
        );

    }

    public void dispatch(HttpContext ctx){
        for(var route:routes){
            if(!route.method().equals(ctx.method)) continue;
            Matcher m=route.pattern().matcher(ctx.path);
            if(m.matches()){
                for(int i=0;i<route.paramNames().size();i++){
                    ctx.pathParams.put(route.paramNames().get(i),m.group(i+1));
                }
                route.handler().accept(ctx);
                return;
            }
        }
        ctx.status(404);
        ctx.json("{\"error\":\"Route noy found\"}");
    }
}
