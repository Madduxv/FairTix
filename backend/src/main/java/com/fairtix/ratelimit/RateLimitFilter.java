package com.fairtix.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;

import java.io.IOException;


/**
 * Filter that intercepts incoming HTTP requests and applies rate limiting based on the client's IP address and the requested URL.
 * Uses RateLimitService to check if the request is allowed and responds with HTTP 429 Too Many Requests if the limit is exceeded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    
    /**
     * Constructor injection of the RateLimitService.
     * @param rateLimitService responsible for managing rate limits
     */
    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
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
        String ip = request.getRemoteAddr();
        String url = request.getRequestURI();

        if (!rateLimitService.isAllowed(ip, url)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
