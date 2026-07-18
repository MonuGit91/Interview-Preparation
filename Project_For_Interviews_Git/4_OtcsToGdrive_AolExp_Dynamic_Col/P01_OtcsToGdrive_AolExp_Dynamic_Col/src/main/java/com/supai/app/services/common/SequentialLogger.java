package com.supai.app.services.common;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.supai.app.services.OtcsToGDriveService;
@Component
public class SequentialLogger {
	private static final Logger log = LoggerFactory.getLogger(OtcsToGDriveService.class);
    private final ExecutorService loggingExecutor = Executors.newSingleThreadExecutor();
    private final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger docNumber = new AtomicInteger(0);
    
    public void start() {
        loggingExecutor.submit(() -> {
            try {
                while (true) {
                	String entry = logQueue.take();
                    if ("__STOP__".equals(entry)) break;
//                    log.info("Doc Number: " + docNumber.addAndGet(1) + entry);
                    log.info("===> Doc Number: " + (docNumber.addAndGet(1)) + entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public boolean log(String message) {
        return logQueue.offer(message);
    }

    public void stop() {
        logQueue.offer("__STOP__");
        loggingExecutor.shutdown();
    }
}
