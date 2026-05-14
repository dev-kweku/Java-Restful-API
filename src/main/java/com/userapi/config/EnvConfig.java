package com.userapi.config;

import com.userapi.util.AppLogger;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;

public class EnvConfig {
    private static final Logger log= AppLogger.get(EnvConfig.class);
    private static Dotenv dotenv;

    public static synchronized Dotenv get(){
        if(dotenv != null) return dotenv;

        dotenv=Dotenv.configure()
                .directory("./")
                .filename(".env")
                .ignoreIfMissing()
                .load();

        String env=dotenv.get("APP_ENV","production");
        log.info("Environment loaded - APP_ENV={}",env);

        warnIfInsecure();

        return dotenv;
    }

    public static String require(String key){
        String value=get().get(key);
        if(value == null || value.isBlank()){
            throw new IllegalStateException("Required environment variable '"+ key + "'is not set. " + "Add it to your " +
                    ".env file or set it as a system environment variable ");
        }

        return value;
    }

    public static String optional(String key,String defaultValue){
        return get().get(key, defaultValue);
    }

    public static int optionalInt(String key,int defaultValue){
        String val=get().get(key);
        if(val==null || val.isBlank()) return defaultValue;
        try{
            return Integer.parseInt(val.trim());

        }catch(NumberFormatException e){
            log.warn("Env var '{}' has non-integer value '{}', using default {}",key,val,defaultValue);
                return  defaultValue;

        }
    }

    private static void warnIfInsecure(){
        String pass=get().get("DB_PASS","");
        String env=get().get("APP_ENV","production");


        if(pass.isBlank()){
            log.warn("DB_PASS is not set - Database has no password!");
        }

        var weakPasswords=java.util.Set.of(
                "password","postgres","admin",
                "root","123456","secret",
                "changeme","userapi_pass"
        );
        if(weakPasswords.contains(pass.toLowerCase())&& env.equals("production")){
            log.error("X-SECURITY: DB_PASS is a weak default password in production");
            throw new IllegalStateException(
                    "Refusing to start in production with a weak DB_PASS ." +
                            "Set a strong password in your environment variables"
            );
        }

        if(weakPasswords.contains(pass.toLowerCase()) && !env.equals("production")){
            log.warn("DB_PASS is a weak password - OK for local dev, not for production");
        }

    }
}
