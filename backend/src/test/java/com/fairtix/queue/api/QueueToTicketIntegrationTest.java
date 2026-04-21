package com.fairtix.queue.api;

import com.fairtix.auth.WithMockPrincipal;
import com.fairtix.events.domain.Event;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.infrastructure.SeatRepository;
import com.fairtix.queue.domain.QueueEntry;
import com.fairtix.queue.domain.QueueStatus;
import com.fairtix.queue.infrastructure.QueueRepository;
import com.fairtix.queue.scheduler.QueueAdmissionScheduler;
import com.fairtix.users.domain.User;
import com.fairtix.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class QueueToTicketIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private QueueRepository queueRepository;
    @Autowired private QueueAdmissionScheduler queueAdmissionScheduler;

    private MockMvc mockMvc;
    private User testUser;
    private Event testEvent;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testUser = new User();
        testUser.setEmail("queueflow@test.com");
        testUser.setPassword("$2a$10$dummyhashfortest");
        testUser.setEmailVerified(true);
        testUser = userRepository.save(testUser);

        testEvent = eventRepository.save(
                new Event("Queue Flow Concert", null, Instant.now().plusSeconds(86400), (UUID) null));
        testEvent.updateQueueSettings(true, null);
        testEvent = eventRepository.save(testEvent);

        testSeat = seatRepository.save(
                new Seat(testEvent, "GA", "1", "1", new BigDecimal("50.00")));
    }

    @Test
    void fullQueueToTicketFlow_success() throws Exception {
        // 1. User joins the queue
        mockMvc.perform(post("/api/events/{eventId}/queue/join", testEvent.getId())
                        .with(WithMockPrincipal.user(testUser.getId(), testUser.getEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"));

        QueueEntry entry = queueRepository
                .findByEventIdAndUserId(testEvent.getId(), testUser.getId()).orElseThrow();
        assertThat(entry.getStatus()).isEqualTo(QueueStatus.WAITING);

        // 2. Trigger admission directly — bypasses @Scheduled timer
        queueAdmissionScheduler.admitWaitingUsers();

        // 3. Verify queue entry is ADMITTED with a future expiry
        QueueEntry admitted = queueRepository
                .findByEventIdAndUserId(testEvent.getId(), testUser.getId()).orElseThrow();
        assertThat(admitted.getStatus()).isEqualTo(QueueStatus.ADMITTED);
        assertThat(admitted.getExpiresAt()).isAfter(Instant.now());

        // 4. Admitted user creates a seat hold
        String holdBody = """
                { "seatIds": ["%s"], "durationMinutes": 15 }
                """.formatted(testSeat.getId());

        MvcResult holdResult = mockMvc.perform(
                        post("/api/events/{eventId}/holds", testEvent.getId())
                                .with(WithMockPrincipal.user(testUser.getId(), testUser.getEmail()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(holdBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn();

        String holdId = com.jayway.jsonpath.JsonPath.read(
                holdResult.getResponse().getContentAsString(), "$[0].id");

        assertThat(seatRepository.findById(testSeat.getId()).get().getStatus())
                .isEqualTo(SeatStatus.HELD);

        // 5. Confirm the hold
        mockMvc.perform(post("/api/holds/{holdId}/confirm", holdId)
                        .with(WithMockPrincipal.user(testUser.getId(), testUser.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertThat(seatRepository.findById(testSeat.getId()).get().getStatus())
                .isEqualTo(SeatStatus.BOOKED);

        // 6. Checkout
        String checkoutBody = """
                { "holdIds": ["%s"], "simulatedOutcome": "SUCCESS" }
                """.formatted(holdId);

        mockMvc.perform(post("/api/payments/checkout")
                        .with(WithMockPrincipal.user(testUser.getId(), testUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.orderStatus").value("COMPLETED"));

        // 7. Verify seat sold and ticket issued
        assertThat(seatRepository.findById(testSeat.getId()).get().getStatus())
                .isEqualTo(SeatStatus.SOLD);

        mockMvc.perform(get("/api/tickets")
                        .with(WithMockPrincipal.user(testUser.getId(), testUser.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("VALID"))
                .andExpect(jsonPath("$[0].price").value(50.00));
    }

    @Test
    void queueJoin_unadmittedUser_blockedFromCreatingHold() throws Exception {
        // User joins queue but admission scheduler has NOT run
        mockMvc.perform(post("/api/events/{eventId}/queue/join", testEvent.getId())
                        .with(WithMockPrincipal.user(testUser.getId(), testUser.getEmail())))
                .andExpect(status().isCreated());

        // Attempt to create a hold before being admitted — must be rejected with 409
        String holdBody = """
                { "seatIds": ["%s"], "durationMinutes": 15 }
                """.formatted(testSeat.getId());

        mockMvc.perform(post("/api/events/{eventId}/holds", testEvent.getId())
                        .with(WithMockPrincipal.user(testUser.getId(), testUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdBody))
                .andExpect(status().isConflict());
    }
}
