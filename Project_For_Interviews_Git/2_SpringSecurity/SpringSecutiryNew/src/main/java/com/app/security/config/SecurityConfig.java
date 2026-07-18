package com.app.security.config;

import java.util.Collections;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;

import com.app.security.handler.AuthEntryPoint;
import com.app.security.jwt.filter.JwtAuthFilter;
import com.app.user.model.UserEntity;
import com.app.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security Configuration Configures authentication, authorization, and
 * security filters
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final AuthEntryPoint unauthorizedHandler;
	private final Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginCustomizer;

	/**
	 * Configures the security filter chain Sets up JWT authentication, public
	 * endpoints, and security policies
	 */
	@Bean
	SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {

		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/auth/login/token", "/auth/login/refreshtoken", "/auth/register").permitAll()
				.requestMatchers("/api/common").authenticated()
				.requestMatchers("/api/user/**").hasRole("USER")
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.requestMatchers("/api/security/jwt/**").authenticated()
				.requestMatchers("/api/security/oauth2/**")
				.permitAll().anyRequest().authenticated());

		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		http.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler));

		http.csrf(csrf -> csrf.disable());
		http.cors(cors -> cors.configurationSource(corsConfigurationSource())); // Enable CORS

		http.oauth2Login(oauth2LoginCustomizer);

		// Add JWT filter before username/password authentication filter
		http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * Password encoder bean for encoding passwords
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Authentication manager bean
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	/**
	 * Initializes default users in the database Runs after application context is
	 * fully loaded and database schema is ready
	 */
	@Bean
	@Profile("dev") // This bean will only be created when the 'dev' profile is active
	public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {

			// Create admin user if it doesn't exist
			if (!userRepository.existsByUsername("admin")) {
				UserEntity admin = UserEntity.builder().username("admin").password(passwordEncoder.encode("admin"))
						.enabled(true).authorities(Collections.singletonList("ROLE_ADMIN")).build();
				userRepository.save(admin);
			}

			if (!userRepository.existsByUsername("user1")) {
				UserEntity user1 = UserEntity.builder().username("user1").password(passwordEncoder.encode("user1"))
						.enabled(true).authorities(Collections.singletonList("ROLE_USER")).build();
				userRepository.save(user1);
			}
		};
	}

	/**
	 * Configures CORS to allow requests from frontend (VS Code Live Server etc.)
	 */
	@Bean
	public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
		org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
		// Allow all origins for development (including file:// which comes as "null"
		// sometimes, or 127.0.0.1:5500)
		configuration.setAllowedOriginPatterns(java.util.Collections.singletonList("*"));
		configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(java.util.Collections.singletonList("*"));
		configuration.setAllowCredentials(true);

		org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
