package com.app.security.jwt.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.security.jwt.service.JwtService;
import com.app.security.oauth.service.OAuth2UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT Authentication Filter
 * Intercepts requests and validates JWT tokens
 * This filter runs once per request before Spring Security's authentication
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final com.app.security.jwt.service.TokenBlacklistService tokenBlacklistService;
    private final OAuth2UserService oauth2UserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("JwtAuthFilter processing request: {}", request.getRequestURI());

        try {
            String jwt = jwtService.getJwtFromHeader(request);

            if (jwt != null && jwtService.validateJwtToken(jwt)) {

                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    log.warn("Request rejected: Token is blacklisted");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is blacklisted (Logged out)");
                    return;
                }

                String username = jwtService.getUsernameFromJwtToken(jwt);
                String provider = jwtService.getProviderFromJwtToken(jwt);

                UserDetails userDetails;

                if ("GOOGLE".equals(provider)) {
                    userDetails = oauth2UserService.loadUserByEmail(username);
                } else {
                    userDetails = userDetailsService.loadUserByUsername(username);
                }

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                usernamePasswordAuthenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                log.debug("User {} authenticated via JWT", username);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
