package com.app.security.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.security.jwt.service.JwtService;
import com.app.security.refreshtoken.dto.LogoutRequest;
import com.app.security.refreshtoken.dto.TokenRefreshRequest;
import com.app.security.refreshtoken.dto.TokenRefreshResponse;
import com.app.security.refreshtoken.model.RefreshToken;
import com.app.security.refreshtoken.service.RefreshTokenService;
import com.app.user.dto.LoginRequest;
import com.app.user.dto.LoginResponse;
import com.app.user.dto.RegisterRequest;
import com.app.user.dto.RegisterResponse;
import com.app.user.model.UserEntity;
import com.app.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.app.security.oauth.service.OAuth2UserService;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final RefreshTokenService refreshTokenService;
	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final AuthenticationManager authenticationManager;
	private final UserService userService;
	private final OAuth2UserService oauth2UserService;

	@PostMapping("/register")
	public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
		log.info("Registration attempt for user: {}", registerRequest.getUsername());

		try {
			UserEntity user = userService.registerUser(registerRequest);

			RegisterResponse response = RegisterResponse.builder()
					.message("User registered successfully")
					.success(true)
					.username(user.getUsername())
					.build();

			log.info("User {} registered successfully", user.getUsername());
			return ResponseEntity.status(HttpStatus.CREATED).body(response);

		} catch (IllegalArgumentException e) {
			log.warn("Registration error: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorResponse(e.getMessage(), false));
		} catch (Exception e) {
			log.error("Registration error", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse("An error occurred during registration", false));
		}
	}

	@PostMapping("/login/token")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
		log.info("Login attempt for user: {}", loginRequest.getUsername());

		Authentication authentication;

		try {
			authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(
							loginRequest.getUsername(),
							loginRequest.getPassword()));
		} catch (AuthenticationException e) {
			log.warn("Authentication failed for user: {}", loginRequest.getUsername());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new ErrorResponse("Invalid username or password", false));
		}

		SecurityContextHolder.getContext().setAuthentication(authentication);
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();

		String jwtToken = jwtService.generateTokenFromUsername(userDetails, "LOCAL");
		List<String> roles = userService.getUserRoles(userDetails);

		RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), "LOCAL");

		LoginResponse response = LoginResponse.builder()
				.username(userDetails.getUsername())
				.jwtToken(jwtToken)
				.refreshToken(refreshToken.getToken())
				.roles(roles)
				.build();

		log.info("User {} authenticated successfully", userDetails.getUsername());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/login/refreshtoken")
	public ResponseEntity<?> refreshtoken(@Valid @RequestBody TokenRefreshRequest request) {
		String requestRefreshToken = request.getRefreshToken();

		return refreshTokenService.findByToken(requestRefreshToken)
				.map(refreshTokenService::verifyExpiration)
				.map(token -> {
					String username = token.getUsername();
					String provider = token.getProvider();

					UserDetails userDetails;
					if ("GOOGLE".equals(provider)) {
						userDetails = oauth2UserService.loadUserByEmail(username);
					} else {
						userDetails = userDetailsService.loadUserByUsername(username);
					}

					String jwtToken = jwtService.generateTokenFromUsername(userDetails, provider);

					return ResponseEntity.ok(new TokenRefreshResponse(jwtToken, requestRefreshToken));
				})
				.orElseThrow(() -> new com.app.security.jwt.exception.TokenRefreshException(requestRefreshToken,
						"Refresh token is not in database!"));
		/*
		 * Simplified explanation of the above functional code:
		 * 
		 * // 1. Attempt to find the token
		 * Optional<RefreshToken> tokenOpt =
		 * refreshTokenService.findByToken(requestRefreshToken);
		 * 
		 * if (tokenOpt.isPresent()) {
		 * RefreshToken token = tokenOpt.get();
		 * 
		 * // 2. Verify it hasn't expired
		 * token = refreshTokenService.verifyExpiration(token);
		 * 
		 * // 3. Get username
		 * String username = token.getUsername();
		 * 
		 * // 4. Generate new token
		 * UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		 * String newJwtToken = jwtService.generateTokenFromUsername(userDetails);
		 * 
		 * // 5. Return success response
		 * return ResponseEntity.ok(new TokenRefreshResponse(newJwtToken,
		 * requestRefreshToken));
		 * 
		 * } else {
		 * // This corresponds to .orElseThrow(...)
		 * throw new TokenRefreshException(requestRefreshToken,
		 * "Refresh token is not in database!");
		 * }
		 */

	}

	@PostMapping("/refresh-token-cookie")
	public ResponseEntity<?> refreshTokenCookie(jakarta.servlet.http.HttpServletRequest request) {
		String refreshToken = null;

		if (request.getCookies() != null) {
			for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
				if ("refresh_token".equals(cookie.getName())) {
					refreshToken = cookie.getValue();
					break;
				}
			}
		}

		if (refreshToken == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorResponse("Refresh Token is empty!", false));
		}

		String requestRefreshToken = refreshToken;

		return refreshTokenService.findByToken(requestRefreshToken)
				.map(refreshTokenService::verifyExpiration)
				.map(token -> {
					String username = token.getUsername();
					String provider = token.getProvider();

					UserDetails userDetails;
					if ("GOOGLE".equals(provider)) {
						userDetails = oauth2UserService.loadUserByEmail(username);
					} else {
						userDetails = userDetailsService.loadUserByUsername(username);
					}

					String jwtToken = jwtService.generateTokenFromUsername(userDetails, provider);

					return ResponseEntity.ok(new TokenRefreshResponse(jwtToken, requestRefreshToken));
				})
				.orElseThrow(() -> new com.app.security.jwt.exception.TokenRefreshException(requestRefreshToken,
						"Refresh token is not in database!"));
	}

	private final com.app.security.jwt.service.TokenBlacklistService tokenBlacklistService;

	@PostMapping("/refreshtoken/revoke")
	public ResponseEntity<?> logout(@RequestBody LogoutRequest logoutRequest,
			jakarta.servlet.http.HttpServletRequest request) {

		// 1. Relies on authenticated context for "User-based" operations
		String username = SecurityContextHolder.getContext().getAuthentication().getName();

		try {
			// Blacklist the current Access Token
			String authHeader = request.getHeader("Authorization");
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				String jwt = authHeader.substring(7);
				// Calculate remaining time for the token
				long expirationTime = jwtService.getExpirationDateFromToken(jwt).getTime();
				long currentTime = System.currentTimeMillis();
				long duration = expirationTime - currentTime;

				if (duration > 0) {
					tokenBlacklistService.blacklistToken(jwt, duration);
				}
			}

			if (logoutRequest.isAllDevices()) {
				// Logout from all devices
				refreshTokenService.deleteByUserId(username);
				return ResponseEntity.ok("Logged out from all devices");
			} else if (logoutRequest.getRefreshToken() != null && !logoutRequest.getRefreshToken().isEmpty()) {
				// Logout from this device (specific token)
				refreshTokenService.deleteByToken(logoutRequest.getRefreshToken());
				return ResponseEntity.ok("Logged out successfully");
			}

			return ResponseEntity.badRequest().body("Invalid logout request");

		} catch (Exception e) {
			return ResponseEntity.badRequest().body("Logout failed: " + e.getMessage());
		}
	}

	@lombok.Data
	@lombok.AllArgsConstructor
	private class ErrorResponse {
		private final String message;
		private final boolean status;
	}
}
