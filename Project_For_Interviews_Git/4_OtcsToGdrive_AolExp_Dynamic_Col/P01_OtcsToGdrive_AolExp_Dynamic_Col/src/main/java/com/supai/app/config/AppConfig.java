package com.supai.app.config;

import java.io.FileInputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

@Configuration
public class AppConfig {
	@Value("${gdrive-service.credentials.path}")
	String credential;
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	@Bean
    public Drive drive() throws Exception {
        // Path to your service account key or client_secret.json
        FileInputStream serviceAccountStream = new FileInputStream(credential);

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(serviceAccountStream)
                .createScoped(List.of(DriveScopes.DRIVE));

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("My GDrive App")
                .build();
    }
}
