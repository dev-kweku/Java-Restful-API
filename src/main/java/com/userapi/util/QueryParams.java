package com.userapi.util;

import java.util.HashMap;
import java.util.Map;

public class QueryParams {

    public static Map<String,String> parse(String rawQuery){
        Map<String,String> params=new HashMap<>();
        if(rawQuery == null || rawQuery.isBlank()) return params;
        for(String pair:rawQuery.split("&")){
            String[] kv= pair.split("=",2);
            if(kv.length==2) params.put(kv[0],kv[1]);
        }
        return params;
    }
}
