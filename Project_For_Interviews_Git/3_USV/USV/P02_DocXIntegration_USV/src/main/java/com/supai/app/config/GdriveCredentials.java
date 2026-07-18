package com.supai.app.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Component
@Slf4j
@ConfigurationProperties(prefix = "gdrive.credentials")
public class GdriveCredentials {
	private String grantType;
	private String credentialsFilePath;
	private Map<String, String> scopes = new LinkedHashMap<>();

	// these values will set in PostConstructor
	private String clientId;
	private String clientSecret;

	// these values will see later
	private String refreshToken;
	private String accessToken;

	public List<String> getScopes() {
		return new ArrayList<>(scopes.values());
	}

	@PostConstruct
	public void init() {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode root = objectMapper.readTree(new File(credentialsFilePath));

			// Recursively search for keys and assign values
			findAndSetKeys(root);

			// Validate all required fields
			if (isBlank(clientId) || isBlank(clientSecret)) {
				String messingFeild = "Missing required GDrive credentials: " + (clientId == null ? "clientId " : "")
						+ (clientSecret == null ? "clientSecret " : "");
				log.error(messingFeild);
				System.exit(0);
			}
		} catch (Exception e) {
			log.error("Error loading Gdrive Credentials - {}", e.getMessage());
			System.exit(0);
		}
	}

	private void findAndSetKeys(JsonNode node) {
		if (node.isObject()) {
			Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				String key = entry.getKey();
				JsonNode value = entry.getValue();

				if (value.isValueNode()) {
					switch (key) {
					case "client_id" -> this.clientId = value.asText();
					case "client_secret" -> this.clientSecret = value.asText();
					}
				} else {
					findAndSetKeys(value); // recurse into nested object or array
				}
			}
		} else if (node.isArray()) {
			for (JsonNode element : node) {
				findAndSetKeys(element); // recurse into each array element
			}
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}
