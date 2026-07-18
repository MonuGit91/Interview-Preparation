package com.supai.app.services.common;

import org.springframework.stereotype.Component;

@Component
public class MyUtil {
	public String getFileExtension(String filename) {
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex != -1 && dotIndex < filename.length() - 1) {
			return filename.substring(dotIndex + 1);
		} else {
			return ""; // No extension found
		}
	}
}
