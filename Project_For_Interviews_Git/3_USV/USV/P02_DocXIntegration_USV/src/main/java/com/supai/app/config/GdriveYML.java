package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data // Lombok: automatically generates getters, setters, toString, equals, hashCode
@Component // Spring Boot: registers this class as a Spring Bean
@ConfigurationProperties(prefix = "gdrive") // Spring Boot: binds all properties starting with `gdrive` in YAML to this
											// class
public class GdriveYML {
	private SharedDrive sharedDrive;
	private Folder folder;
	private String docx;
	private String other;

	@Data
	public static class SharedDrive {
		private String id; // Binds gdrive.sharedDrive.id
		private String name; // Binds gdrive.sharedDrive.name
	}

	@Data
	public static class Folder {
		private String root; // Binds gdrive.folder.root
	}

	// Method to safely get root
	public String getGdriveRoot() {
		return folder.getRoot();
	}
}
