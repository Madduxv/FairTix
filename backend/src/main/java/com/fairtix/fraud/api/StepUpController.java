package com.fairtix.fraud.api;

import com.fairtix.auth.application.RecaptchaService;
import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.fraud.application.StepUpGateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth/step-up")
public class StepUpController {

    private final StepUpGateService stepUpGateService;
    private final RecaptchaService recaptchaService;

    public StepUpController(StepUpGateService stepUpGateService, RecaptchaService recaptchaService) {
        this.stepUpGateService = stepUpGateService;
        this.recaptchaService = recaptchaService;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody Map<String, String> body) {
        String captchaToken = body.get("captchaToken");
        recaptchaService.assertValidToken(captchaToken);
        stepUpGateService.markVerified(principal.getUserId());
        return ResponseEntity.ok(Map.of("status", "verified"));
    }
}
