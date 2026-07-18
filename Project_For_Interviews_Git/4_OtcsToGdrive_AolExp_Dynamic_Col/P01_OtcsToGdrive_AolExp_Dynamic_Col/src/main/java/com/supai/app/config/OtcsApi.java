package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "otcs.api")
@Data
public class OtcsApi {
	private String baseUrl;
	private String getDoc;
	private String searchDocs;
	private String categoryInfo;
	private String nodeProperty;
	private String childNodeProperty;
	private String userInfo;
}

