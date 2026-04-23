package com.fairtix.audit.api;

import com.fairtix.audit.domain.AuditLog;
import com.fairtix.audit.infrastructure.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class AdminAuditControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private MockMvc mockMvc;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        targetUserId = UUID.randomUUID();
        auditLogRepository.save(new AuditLog(targetUserId, "FRAUD_FLAG_CREATED", "FLAG", UUID.randomUUID(), "suspicious activity"));
        auditLogRepository.save(new AuditLog(targetUserId, "STEP_UP_VERIFY_SUCCESS", "STEP_UP", null, "step-up passed"));
        auditLogRepository.save(new AuditLog(targetUserId, "REFUND_AUTO_APPROVED", "REFUND", UUID.randomUUID(), "below threshold"));
    }

    @Test
    void adminCanQueryAllLogsForUser() throws Exception {
        mockMvc.perform(get("/api/admin/audit")
                        .param("userId", targetUserId.toString())
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].action", notNullValue()))
                .andExpect(jsonPath("$.content[0].userId", is(targetUserId.toString())));
    }

    @Test
    void adminCanFilterByActionPrefix() throws Exception {
        mockMvc.perform(get("/api/admin/audit")
                        .param("userId", targetUserId.toString())
                        .param("action", "FRAUD_")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action", is("FRAUD_FLAG_CREATED")));
    }

    @Test
    void adminCanFilterByStepUpPrefix() throws Exception {
        mockMvc.perform(get("/api/admin/audit")
                        .param("userId", targetUserId.toString())
                        .param("action", "STEP_UP_")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action", is("STEP_UP_VERIFY_SUCCESS")));
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/audit")
                        .param("userId", targetUserId.toString())
                        .with(user("user@test.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/audit")
                        .param("userId", targetUserId.toString()))
                .andExpect(status().isUnauthorized());
    }
}
