package com.fairtix.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import io.jsonwebtoken.Claims;
import com.fairtix.auth.application.JwtService;

import java.io.IOException;


/**
 * Filter that intercepts incoming HTTP requests and applies rate limiting based on the client's IP address and the requested URL.
 * Uses RateLimitService to check if the request is allowed and responds with HTTP 429 Too Many Requests if the limit is exceeded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final RateLimitService rateLimitService;
    
    /**
     * Constructor injection of the RateLimitService.
     * @param rateLimitService responsible for managing rate limits
     */
    public RateLimitFilter(RateLimitService rateLimitService, JwtService jwtService) {
        this.rateLimitService = rateLimitService;
        this.jwtService = jwtService;
    }

    /**
     * Intercepts each HTTP request, extracts the client's IP and requested URL, and enforces rate limiting.
     * If the request exceeds the rate limit, responds with HTTP 429 Too Many Requests; 
     * otherwise, continues processing the request.
     * @param request the incoming HTTP request
     * @param response the HTTP response to be sent
     * @param filterChain the filter chain to continue processing the request if allowed
     * 
     * @throws ServletException if something goes wrong inside the filter while handling the request
     * @throws IOException if there is an error reading the request or writing the response
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String url = request.getRequestURI().replaceAll("/[0-9a-fA-F-]{36}", "/{id}");
        String key;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            // try to extract userId from JWT token
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Claims claims = jwtService.extractAllClaims(token);
                    String userId = claims.get("userId", String.class);
                    if (userId != null) {
                        key = "user:" + userId;
                    } else {
                        // fallback to IP if userId not found
                        String ip = request.getHeader("X-Forwarded-For");
                        if (ip == null || ip.isEmpty()) {
                            ip = request.getRemoteAddr();
                        }
                        key = "ip:" + ip;
                    }
                } catch (Exception e) {
                    // fallback to IP if token invalid
                    String ip = request.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty()) {
                        ip = request.getRemoteAddr();
                    }
                    key = "ip:" + ip;
                }
            } else {
                // fallback to IP if no token
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                key = "ip:" + ip;
            }
        } else {
            // not authenticated, use IP
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            key = "ip:" + ip;
        }

        if (!rateLimitService.isAllowed(key, url)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
