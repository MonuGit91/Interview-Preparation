package com.supai.app.services.gdrive;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.supai.app.config.GdriveCredentials;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CallingGDriveApi {
	private static final Logger log = LoggerFactory.getLogger(CallingGDriveApi.class);
	private final AccessTokenGDrive accessTokenGDrive;
	private final GdriveCredentials gdriveCredentials;
	private final RefreshTokenGDrive refreshTokenGDrive;
	private final JsonObj jsonObj;
	public static boolean isGdriveTokenRevoked = false;

	@SuppressWarnings("unchecked")
	public <T> T retryIfUnauthorizedGdrive(Callable<T> action, String errorMessage, String successMessage)
			throws Exception {

		try {
			// first set refresh token if not present
			if (gdriveCredentials.getRefreshToken() == null) {
				attemptAction(() -> refreshTokenGDrive.setRefreshToken(), "X: No refresh token was returned.", null);
				if (gdriveCredentials.getRefreshToken() != null) {
					log.info("Got Refresh Token.");
				}
			}
			// Attempt the provided action and log success message if provided
			T result = attemptAction(action, errorMessage, successMessage);
			return result;
		} catch (Exception e) {
			String error = null;
			if (isUnauthorizedError(e)) {
				// If unauthorized, update access token and retry the action
				try {
					log.info("Updating Gdrive Access Token...");
					accessTokenGDrive.updateAccessToken();
					log.info("Gdrive Access Tokeb Updated.");
					return attemptAction(action, errorMessage, successMessage);
				} catch (Exception retryException) {
					if (retryException.getMessage().contains("Token has been expired or revoked")) {
						log.error("GDrive Refresh token has been expired or revoked - {}", retryException.getMessage());

						isGdriveTokenRevoked = true;
						error = JsonObj.getJson("error", "Token has been expired or revoked").toString();
//						HttpHeaders headers = new HttpHeaders();

//						return (T) ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//								.contentType(MediaType.APPLICATION_JSON).body(error);

					} else if (isUnauthorizedError(e)) {
						error = jsonObj.getJson("error",
								"401 Unauthorized - Please Try Again After Deleteing 'StoredCredential' file from 'tokens' Folder.",
								e.getMessage()).toString();
						log.error(error);
//						System.exit(0);
					} else {
						error = jsonObj.getJson("error", "GDrive Refresh token has been expired or revoked - {}",
								e.getMessage()).toString();
						log.error(error);
					}
//					return (T) ResponseEntity.status(HttpStatus.BAD_REQUEST)
//							.contentType(MediaType.APPLICATION_JSON).body(error);
				}
			} else {
				if (e.getMessage().equalsIgnoreCase("Address already in use: bind")) {
					error = jsonObj.getJson("error",
							"either change the gdrive app-port or refer https://docs.google.com/document/d/1asjf7jrPuD-WS5u1RrbUUXUcjGPtfK2KixSXHAw6Muo/edit?usp=sharing",
							e.getMessage()).toString();
					log.error(error);
				} else {
					error = jsonObj.getJson("error", errorMessage, e.getMessage()).toString();
					log.error(error);
				}
			}
			throw new IllegalAccessException(error);
		}
	}

	private <T> T attemptAction(Callable<T> action, String errorMessage, String successMessage) throws Exception {

		T response = action.call();
		ResponseEntity<?> genericResponse = (ResponseEntity<?>) response;
		// Check if the response is successful (HTTP status 200 OK)
		if (genericResponse.getStatusCode() == HttpStatus.OK) {
			if (successMessage != null)
				log.info(successMessage);
			return response;
		} else {
			if (errorMessage != null)
				log.info(errorMessage + " - {}", genericResponse.getStatusCode().toString());
			return null;
		}
	}

	private boolean isUnauthorizedError(Exception e) {
		// Check if the exception message contains "401 Unauthorized"
		return e.getMessage().contains("401 Unauthorized");
	}
}
