package com.supai.app.services.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class LogUtils {
	private static String logFormat = "\n%s : [%s] : %-4d : %s - %s";
	public static <T> String info(String template, String... values) {
	    StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
	    String className = caller.getClassName();
	    int lineNumber = caller.getLineNumber();
	    String paddedClassName = String.format("%-55s", className);
	    paddedClassName = trimClass(paddedClassName);
	    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

//	     Replace all `{}` with `%s`
	    String formattedTemplate = template.replace("{}", "%s");
	    String message = String.format(formattedTemplate, (Object[]) values);

	    return String.format(logFormat , timestamp, paddedClassName, lineNumber, "INFO ", message);
//	    return String.format("\n : %-4d - %s", lineNumber, message);
	}
	public static String error(String template, String... values) {
	    StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
	    String className = caller.getClassName();
	    int lineNumber = caller.getLineNumber();
	    String paddedClassName = String.format("%-55s", className);
	    paddedClassName = trimClass(paddedClassName);
	    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

	    // Replace all `{}` with `%s`
	    String formattedTemplate = template.replace("{}", "%s");
	    String message = String.format(formattedTemplate, (Object[]) values);

	    return String.format(logFormat, timestamp, paddedClassName, lineNumber, "ERROR", message);
	}
	public static String warn(String template, String... values) {
	    StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
	    String className = caller.getClassName();
	    int lineNumber = caller.getLineNumber();
	    String paddedClassName = String.format("%-55s", className);
	    paddedClassName = trimClass(paddedClassName);
	    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

	    // Replace all `{}` with `%s`
	    String formattedTemplate = template.replace("{}", "%s");
	    String message = String.format(formattedTemplate, (Object[]) values);

	    return String.format(logFormat, timestamp, paddedClassName, lineNumber, "WARN", message);
	}
	
	private static String trimClass(String className) {
		int maxLength = 55;
//		List<String> list = List.of(className.split("\\.")); // immutable list
		List<String> list = new ArrayList<>(List.of(className.split("\\."))); // mutable list
		for(int i = 0; i < list.size()-1 && String.join(".", list).length() > maxLength; i++) {
			list.set(i, String.valueOf(list.get(i).charAt(0)));
		}
		return String.join(".", list);
	}
}
