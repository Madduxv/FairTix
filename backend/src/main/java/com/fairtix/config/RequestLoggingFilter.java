package com.fairtix.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    String requestId = UUID.randomUUID().toString();
    MDC.put("requestId", requestId);

    long start = System.currentTimeMillis();

    try {

      log.info("[{}] <- {} {}", requestId,
              request.getMethod(),
              request.getRequestURI());

      filterChain.doFilter(request, response);
    } finally {

      long duration = System.currentTimeMillis() - start;

      log.info("[{}] -> {} ({} ms)", requestId,
              response.getStatus(),
              duration);
      MDC.clear();

    }
  }
}
