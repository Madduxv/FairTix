package com.fairtix.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.redisson.api.RateType;
import org.redisson.api.RateIntervalUnit;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Unit tests for RateLimitService to verify that rate limiting is enforced correctly
 * for different endpoint types. Uses Mockito to mock Redisson rate limiter behavior.
 */
public class RateLimitServiceTest {

    private RedissonClient redissonClient;
    private RRateLimiter rateLimiter;
    private RateLimitService rateLimitService;

    @BeforeEach
    public void setUp() {
        redissonClient = mock(RedissonClient.class);
        rateLimiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        rateLimitService = new RateLimitService(redissonClient);
        // Inject @Value defaults
        ReflectionTestUtils.setField(rateLimitService, "authLimit", 10);
        ReflectionTestUtils.setField(rateLimitService, "ordersLimit", 20);
        ReflectionTestUtils.setField(rateLimitService, "seatsLimit", 60);
        ReflectionTestUtils.setField(rateLimitService, "defaultLimit", 100);
    }

    @Test
    public void testAuthEndpointRateLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(10L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true, false
        );
        String ip = "1.2.3.4";
        String url = "/api/auth/login";
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.isAllowed(ip, url));
        }
        assertFalse(rateLimitService.isAllowed(ip, url));
    }

    @Test
    public void testAuthRegisterEndpointUsesAuthLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(10L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true, false
        );
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.isAllowed("1.2.3.4", "/auth/register"));
        }
        assertFalse(rateLimitService.isAllowed("1.2.3.4", "/auth/register"));
    }

    @Test
    public void testAdminEndpointRateLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(20L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, false
        );
        String url = "/api/admin/users/123/promote";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.isAllowed("1.2.3.4", url));
        }
        assertFalse(rateLimitService.isAllowed("1.2.3.4", url));
    }

    @Test
    public void testPaymentsEndpointUsesOrdersLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(20L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, false
        );
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.isAllowed("1.2.3.4", "/api/payments/checkout"));
        }
        assertFalse(rateLimitService.isAllowed("1.2.3.4", "/api/payments/checkout"));
    }

    @Test
    public void testEventsHoldsEndpointRateLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(20L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, false
        );
        String url = "/api/events/456/holds";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.isAllowed("1.2.3.4", url));
        }
        assertFalse(rateLimitService.isAllowed("1.2.3.4", url));
    }

    @Test
    public void testEventsSeatsEndpointRateLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(60L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            false
        );
        String url = "/api/events/789/seats";
        for (int i = 0; i < 60; i++) {
            assertTrue(rateLimitService.isAllowed("1.2.3.4", url));
        }
        assertFalse(rateLimitService.isAllowed("1.2.3.4", url));
    }

    @Test
    public void testEventsDefaultEndpointRateLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(100L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            false
        );
        String url = "/api/events";
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimitService.isAllowed("1.2.3.4", url));
        }
        assertFalse(rateLimitService.isAllowed("1.2.3.4", url));
    }

    @Test
    public void testOrdersAndHoldsEndpointRateLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(20L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, false
        );
        String url = "/api/orders/create";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.isAllowed("1.2.3.4", url));
        }
        assertFalse(rateLimitService.isAllowed("1.2.3.4", url));

        // Also test /api/holds
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(20L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, false
        );
        url = "/api/holds/abc";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.isAllowed("1.2.3.4", url));
        }
        assertFalse(rateLimitService.isAllowed("1.2.3.4", url));
    }

    @Test
    public void testInventoryAndSeatsEndpointRateLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(60L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            false
        );
        for (int i = 0; i < 60; i++) {
            assertTrue(rateLimitService.isAllowed("1.2.3.4", "/api/inventory/list"));
        }
        assertFalse(rateLimitService.isAllowed("1.2.3.4", "/api/inventory/list"));
    }

    @Test
    public void testDefaultEndpointRateLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(100L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true,
            false
        );
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimitService.isAllowed("1.2.3.4", "/api/other"));
        }
        assertFalse(rateLimitService.isAllowed("1.2.3.4", "/api/other"));
    }

    @Test
    public void testRateLimitByUserIdAndIp() {
        String userKey = "user:11111111-1111-1111-1111-111111111111";
        String ipKey = "ip:1.2.3.4";
        String url = "/api/events";

        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(true, false);
        assertTrue(rateLimitService.isAllowed(userKey, url));
        assertFalse(rateLimitService.isAllowed(userKey, url));

        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(true, false);
        assertTrue(rateLimitService.isAllowed(ipKey, url));
        assertFalse(rateLimitService.isAllowed(ipKey, url));
    }

    @Test
    public void testResolveLimitCoversAllEndpointCategories() {
        assertEquals(10, rateLimitService.resolveLimit("/api/auth/login"));
        assertEquals(10, rateLimitService.resolveLimit("/auth/register"));
        assertEquals(20, rateLimitService.resolveLimit("/api/payments/checkout"));
        assertEquals(20, rateLimitService.resolveLimit("/api/orders"));
        assertEquals(20, rateLimitService.resolveLimit("/api/admin/users"));
        assertEquals(20, rateLimitService.resolveLimit("/api/events/{id}/holds"));
        assertEquals(60, rateLimitService.resolveLimit("/api/events/{id}/seats"));
        assertEquals(60, rateLimitService.resolveLimit("/api/inventory/list"));
        assertEquals(100, rateLimitService.resolveLimit("/api/events"));
        assertEquals(100, rateLimitService.resolveLimit("/api/tickets"));
        assertEquals(100, rateLimitService.resolveLimit("/api/analytics/dashboard"));
    }
}
