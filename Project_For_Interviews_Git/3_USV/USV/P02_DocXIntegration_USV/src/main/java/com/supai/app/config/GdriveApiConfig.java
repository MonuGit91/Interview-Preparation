package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "gdrive.api")
@Data
public class GdriveApiConfig {
    private String accessTokenUri;
    private String resumableUploadUri;
    private String listSubfoldersUri;
    private String createFolderUri;
    private String downloadDriveDocUri;
    private String deleteDocUrl;
}