package com.supai.app.services.common;

public class LogBuffer {
    private static final ThreadLocal<StringBuilder> threadLogBuffer = ThreadLocal.withInitial(StringBuilder::new);

    public static String get() {
        return threadLogBuffer.get().toString();
    }

    public static void append(String log) {
        threadLogBuffer.get().append(log);
    }

    public static void clear() {
        threadLogBuffer.remove(); // Prevent memory leak
    }
}