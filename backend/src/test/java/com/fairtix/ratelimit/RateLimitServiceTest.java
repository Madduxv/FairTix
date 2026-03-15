package com.fairtix.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.redisson.api.RateType;
import org.redisson.api.RateIntervalUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Unit tests for RateLimitService to verify that rate limiting is enforced correctly
 * for different endpoint types. Uses Mockito to mock Redisson rate limiter behavior.
 */
public class RateLimitServiceTest {
        /**
         * Test that /api/events endpoints (not containing /seats or /holds) are limited to 100 requests per minute per IP (default group).
         * The 101st request should be blocked.
         */
        @Test
        public void testEventsDefaultEndpointRateLimit() {
            when(rateLimiter.isExists()).thenReturn(false);
            when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(100L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
            when(rateLimiter.tryAcquire(1)).thenReturn(
                // 100 trues, then false
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
            String ip = "1.2.3.4";
            // Test /api/events (POST/GET)
            String url = "/api/events";
            for (int i = 0; i < 100; i++) {
                assertTrue(rateLimitService.isAllowed(ip, url));
            }
            assertFalse(rateLimitService.isAllowed(ip, url));

            // Test /api/events/123 (GET/PUT/DELETE)
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
            url = "/api/events/123";
            for (int i = 0; i < 100; i++) {
                assertTrue(rateLimitService.isAllowed(ip, url));
            }
            assertFalse(rateLimitService.isAllowed(ip, url));
        }

        /**
         * Test that /api/events/{eventId}/holds endpoints are limited to 20 requests per minute per IP (holds group).
         * The 21st request should be blocked.
         */
        @Test
        public void testEventsHoldsEndpointRateLimit() {
            when(rateLimiter.isExists()).thenReturn(false);
            when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(20L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
            when(rateLimiter.tryAcquire(1)).thenReturn(
                true, true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, true, true, true, true,
                false
            );
            String ip = "1.2.3.4";
            String url = "/api/events/456/holds";
            for (int i = 0; i < 20; i++) {
                assertTrue(rateLimitService.isAllowed(ip, url));
            }
            assertFalse(rateLimitService.isAllowed(ip, url));
        }

        /**
         * Test that /api/events/{eventId}/seats endpoints are limited to 60 requests per minute per IP (seats group).
         * The 61st request should be blocked.
         */
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
            String ip = "1.2.3.4";
            String url = "/api/events/789/seats";
            for (int i = 0; i < 60; i++) {
                assertTrue(rateLimitService.isAllowed(ip, url));
            }
            assertFalse(rateLimitService.isAllowed(ip, url));
        }
    // Mocked dependencies
    private RedissonClient redissonClient;
    private RRateLimiter rateLimiter;
    // Service under test
    private RateLimitService rateLimitService;

    /**
     * Set up mocks and service before each test.
     */
    @BeforeEach
    public void setUp() {
        redissonClient = mock(RedissonClient.class);
        rateLimiter = mock(RRateLimiter.class);
        // Always return the same mock rate limiter for any key
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        rateLimitService = new RateLimitService(redissonClient);
    }


    /**
     * Test that /api/auth endpoints are limited to 10 requests per minute per IP.
     * The 11th request should be blocked.
     */
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

    /**
     * Test that /api/admin endpoints are limited to 20 requests per minute per IP.
     * The 21st request should be blocked.
     */
    @Test
    public void testAdminEndpointRateLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(20L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, false
        );
        String ip = "1.2.3.4";
        String url = "/api/admin/users/123/promote";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.isAllowed(ip, url));
        }
        assertFalse(rateLimitService.isAllowed(ip, url));
    }


    /**
     * Test that /api/orders and all endpoints containing /holds are limited to 20 requests per minute per IP.
     * The 21st request should be blocked.
     */
    @Test
    public void testOrdersAndHoldsEndpointRateLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(20L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, false
        );
        String ip = "1.2.3.4";
        String url = "/api/orders/create";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.isAllowed(ip, url));
        }
        assertFalse(rateLimitService.isAllowed(ip, url));

        // Also test /api/holds
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(20L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(
            true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, false
        );
        url = "/api/holds/abc";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.isAllowed(ip, url));
        }
        assertFalse(rateLimitService.isAllowed(ip, url));
    }


    /**
     * Test that /api/inventory and all endpoints containing /seats are limited to 60 requests per minute per IP.
     * The 61st request should be blocked.
     */
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
        String ip = "1.2.3.4";
        String url = "/api/inventory/list";
        for (int i = 0; i < 60; i++) {
            assertTrue(rateLimitService.isAllowed(ip, url));
        }
        assertFalse(rateLimitService.isAllowed(ip, url));

        // Also test /api/events/xyz/seats
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
        url = "/api/events/xyz/seats";
        for (int i = 0; i < 60; i++) {
            assertTrue(rateLimitService.isAllowed(ip, url));
        }
        assertFalse(rateLimitService.isAllowed(ip, url));
    }


    /**
     * Test that all other endpoints are limited to 100 requests per minute per IP.
     * The 101st request should be blocked.
     */
    @Test
    public void testDefaultEndpointRateLimit() {
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(eq(RateType.OVERALL), eq(100L), eq(1L), eq(RateIntervalUnit.MINUTES))).thenReturn(true);
        // Allow 100 requests, block the 101st
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
        String ip = "1.2.3.4";
        String url = "/api/other";
        // First 100 requests allowed
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimitService.isAllowed(ip, url));
        }
        // 101st request blocked
        assertFalse(rateLimitService.isAllowed(ip, url));
    }

    /**
     * Test that rate limiting is independent for user and IP keys.
     */
    @Test
    public void testRateLimitByUserIdAndIp() {
        String userKey = "user:11111111-1111-1111-1111-111111111111";
        String ipKey = "ip:1.2.3.4";
        String url = "/api/events";

        // User key: allow once, then block
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(true, false);
        assertTrue(rateLimitService.isAllowed(userKey, url));
        assertFalse(rateLimitService.isAllowed(userKey, url));

        // IP key: allow once, then block
        when(rateLimiter.isExists()).thenReturn(false);
        when(rateLimiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(true, false);
        assertTrue(rateLimitService.isAllowed(ipKey, url));
        assertFalse(rateLimitService.isAllowed(ipKey, url));
    }
}
