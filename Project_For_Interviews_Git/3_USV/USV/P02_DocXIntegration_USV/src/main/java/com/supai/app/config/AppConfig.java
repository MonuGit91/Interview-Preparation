package com.supai.app.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import okhttp3.OkHttpClient;

@Configuration
public class AppConfig {
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public OkHttpClient okHttpClient() {
		return new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS) // wait up to 30s to connect
				.readTimeout(60, TimeUnit.SECONDS) // wait up to 60s for server response
				.writeTimeout(60, TimeUnit.SECONDS) // wait up to 60s for request body write
				.build();
	}
}
