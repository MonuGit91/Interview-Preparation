package com.supai.app.ivapis;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.IvConfig;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.ivapis.dto.response.IvTicketResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Component
@RequiredArgsConstructor
@Slf4j
public class DownloadArtifact {
	private final ObjectMapper objectMapper;

	// Reuse a single client instance to save system resources
	private final OkHttpClient client;// = new OkHttpClient().newBuilder().build();
	private final IvConfig ivConfig;

	private byte[] callDownload(String url, IvTicketResponse ivTicketResponse) throws IOException { // Changed signature
		log.info("Start : {}", url);
		Request request = new Request.Builder().url(url).get()
				.addHeader("Authorization", "Bearer " + ivTicketResponse.getToken())
				.addHeader("Accept", "*/*").build();

		try (Response response = client.newCall(request).execute()) {
			log.info("End : {}", url);
			if (!response.isSuccessful()) {
				// Parse error body if possible, or create a simple error node
				String errorDetails = response.body() != null ? response.body().string() : "{}";
				JsonNode errorNode;
				try {
					errorNode = objectMapper.readTree(errorDetails);
				} catch (Exception e) {
					errorNode = objectMapper.createObjectNode().put("message", errorDetails);
				}
				throw new ExternalApiException(response.code(), errorNode, "IV Download Artifact", url);
			}

			ResponseBody body = response.body();
			return (body != null) ? body.bytes() : new byte[0];
		}
	}

	public byte[] downloadDoc(String publicationId, IvTicketResponse ivTicketResponse) {
		String url = ivConfig.getApi().getDownload().replace("{id}", publicationId);
		int maxAttempts = 3;
		for (int i = 1; i <= maxAttempts; i++) {
			try {
				return callDownload(url, ivTicketResponse);
			} catch (Exception e) {
				if (i == maxAttempts) {
					log.error("Failed to download doc after {} attempts: {}", maxAttempts, e.getMessage());
					if (e instanceof ExternalApiException)
						throw (ExternalApiException) e;
					// Create a JSON error structure for network/IO failures
					JsonNode errorNode = objectMapper.createObjectNode().put("Error", e.getMessage());
					throw new ExternalApiException(502, errorNode, "IV Download Artifact", url);
				}
				log.warn("Attempt {} failed: {}. Retrying in 2 seconds...", i, e.getMessage());
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Retry interrupted", ie);
				}
			}
		}
		return new byte[0];
	}

	// public ResponseBody getUpdatedDoc(String publicationId, String bearerToken)
	// throws Exception {
	// String url =
	// "http://in-hodevotds1.glenmark.com:3350/artifacts/fs-0/publications/{id}/pdf/new.pdf"
	// .replace("{id}", publicationId);
	//
	// Request request = new
	// Request.Builder().url(url).get().addHeader("Authorization", "Bearer " +
	// bearerToken)
	// .addHeader("Accept", "*/*").build();
	//
	// // Execute synchronously (this thread will wait until the server responds)
	// try (Response response = client.newCall(request).execute()) {
	// // Check if the server returned an error (4xx or 5xx)
	// if (!response.isSuccessful()) {
	// String errorDetails = response.body() != null ? response.body().string() :
	// "No error body";
	// // return "Error " + response.code() + ": " + errorDetails;
	// return null;
	// }
	//
	// // Get the actual data
	// ResponseBody body = response.body();
	// return body;
	// }
	// }
}