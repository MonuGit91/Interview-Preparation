package com.supai.app.services.common;

import org.springframework.stereotype.Component;

import com.supai.app.services.otcs.CatogeryApi;

@Component
public class LogUtils {
    public static int getLineNumber() {
        return new Throwable().getStackTrace()[1].getLineNumber();
    }
}

// below line will print class name with line number
// log.info("Unable to get catogery details [{}:{}]", CatogeryApi.class, logUtils.getLineNumber());