package com.fairtix.fraud.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.fraud.application.StepUpGateService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class StepUpFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final List<String[]> GUARDED = List.of(
            new String[]{"POST", "/api/payments/checkout", "CHECKOUT"},
            new String[]{"POST", "/api/events/*/holds", "SEAT_HOLD"}
    );

    private final StepUpGateService stepUpGateService;

    public StepUpFilter(StepUpGateService stepUpGateService) {
        this.stepUpGateService = stepUpGateService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();

        String action = resolveAction(method, path);
        if (action != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserPrincipal principal) {
                UUID userId = principal.getUserId();
                if (stepUpGateService.requiresStepUp(userId, action)
                        && !stepUpGateService.isVerified(userId)) {
                    writeStepUpRequired(response, action);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveAction(String method, String path) {
        for (String[] rule : GUARDED) {
            if (rule[0].equalsIgnoreCase(method) && MATCHER.match(rule[1], path)) {
                return rule[2];
            }
        }
        return null;
    }

    private void writeStepUpRequired(HttpServletResponse response, String action) throws IOException {
        response.setStatus(428);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":428,\"code\":\"STEP_UP_REQUIRED\",\"action\":\"" + action
                + "\",\"message\":\"Step-up verification required\"}");
    }
}
