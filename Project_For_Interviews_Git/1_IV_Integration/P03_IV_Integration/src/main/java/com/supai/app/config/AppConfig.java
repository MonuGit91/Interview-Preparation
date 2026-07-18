package com.supai.app.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.springframework.context.annotation.Primary;

import okhttp3.OkHttpClient;

@Configuration
public class AppConfig {
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public OkHttpClient okHttpClient() {
		return new OkHttpClient.Builder()
				// Your Timeout Settings
				.connectTimeout(120, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS)
				.writeTimeout(120, TimeUnit.SECONDS)

				// Your Redirect Setting
				.followRedirects(true).followSslRedirects(true)

				// Optional: Industry standard to handle flaky connections
				.retryOnConnectionFailure(true)

				.build();
	}

	@Bean
	@Primary
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public XmlMapper xmlMapper() {
		XmlMapper mapper = new XmlMapper();
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		return mapper;
	}
}
