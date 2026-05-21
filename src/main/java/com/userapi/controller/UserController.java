package com.userapi.controller;

import com.userapi.model.User;
import com.userapi.server.HttpContext;
import com.userapi.service.UserService;
import com.userapi.util.JsonUtil;
import com.userapi.util.QueryParams;

import java.util.Map;

public class UserController {
    private final UserService service;
    public UserController(UserService service){this.service=service;}


    public void getAll(HttpContext ctx){
        try{
            var params= QueryParams.parse(ctx.rawQuery);
            int page=Integer.parseInt(params.getOrDefault("page","0"));
            int size=Integer.parseInt(params.getOrDefault("size","20"));
            String country=params.get("country");

            var result=service.getAll(page,size,country);

            ctx.json(JsonUtil.toJson(Map.of(
                    "data",result.users(),
                    "total",result.total(),
                    "page",result.page(),
                    "size",result.size()
            )));

        }catch(Exception e){
            ctx.status(500);
            ctx.json(JsonUtil.error(e.getMessage()));
        }
    }

    public void getById(HttpContext ctx){
        try{
            long id=Long.parseLong(ctx.pathParams.get("id"));
            service.getById(id).ifPresentOrElse(
                    user -> ctx.json(JsonUtil.toJson(user)),
                    ()->{ctx.status(404); ctx.json(JsonUtil.error("User not found"));}
            );
        }catch(Exception e){
            ctx.status(500);
            ctx.json(JsonUtil.error(e.getMessage()));
        }
    }

    public void create(HttpContext ctx){
        try{
            User user=JsonUtil.fromJson(ctx.body,User.class);
            User created=service.create(user);
            ctx.status(201);
            ctx.json(JsonUtil.toJson(created));

        }catch(IllegalArgumentException e){
            ctx.status(400);
            ctx.json(JsonUtil.error(e.getMessage()));
        }catch (Exception e){
            ctx.status(500);
            ctx.json(JsonUtil.error(e.getMessage()));
        }
    }

    public void update(HttpContext ctx){
        try{
            long id=Long.parseLong(ctx.pathParams.get("id"));
            User user=JsonUtil.fromJson(ctx.body,User.class);
            service.update(id,user).ifPresentOrElse(
                    updated->
                            ctx.json(JsonUtil.toJson(updated)),
                            ()->{ctx.status(404);
                            ctx.json(JsonUtil.error("User not found"));}
            );

        }catch (IllegalArgumentException e){
            ctx.status(400);
            ctx.json(JsonUtil.error(e.getMessage()));
        }catch (Exception e){
            ctx.status(500);
            ctx.json(JsonUtil.error(e.getMessage()));
        }
    }

    public void delete(HttpContext ctx){
        try{
            long id=Long.parseLong(ctx.pathParams.get("id"));
            if(service.delete(id)){
                ctx.status(204);
                ctx.json("");
            }else{
                ctx.status(404);
                ctx.json(JsonUtil.error("User not found"));
            }
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(JsonUtil.error(e.getMessage()));
        }
    }
}
