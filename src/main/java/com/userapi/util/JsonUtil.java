package com.userapi.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jaxrs.Jaxrs2TypesModule;

import java.util.Map;

public class JsonUtil {
    private static final ObjectMapper mapper=new ObjectMapper()
            .registerModule(new Jaxrs2TypesModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);



    public static String toJson(Object obj){
        try{return mapper.writeValueAsString(obj);}catch(Exception e){
            return "{\"error\":\"Serialization failed\"}"
                    ;}
    }

    public static <T> T fromJson(String json,Class<T> clazz) throws Exception{
        return mapper.readValue(json,clazz);
    }

    public static  String error(String message){
        return toJson(Map.of("error",message));
    }
}
