package com.userapi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppLogger {

    public static final Logger METRICS= LoggerFactory.getLogger("METRICS");
    private AppLogger(){}

    public static Logger get(Class<?> clazz){
        return LoggerFactory.getLogger(clazz);
    };

}
