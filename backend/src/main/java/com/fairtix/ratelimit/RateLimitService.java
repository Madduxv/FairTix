package com.fairtix.ratelimit;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.redisson.api.RateType;
import org.springframework.stereotype.Service;

import org.redisson.api.RateIntervalUnit;

@Service
public class RateLimitService {

    private final RedissonClient redissonClient;

    /**
     * Constructor for RateLimitService.
     * @param redissonClient the Redisson client used for interacting with Redis. 
     */
    public RateLimitService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * Checks if a request from a given key (userId or IP) to a specific endpoint is allowed based on the rate limit.
     * @param key the userId or IP address of the client making the request
     * @param url the URL being accessed
     * @return true if the request is allowed, false if it exceeds the rate limit 
     */
    public boolean isAllowed(String key, String url) {
        String redisKey = "rate_limit:" + key + ":" + url;

        // Retrieves rate limiter from Redis
        RRateLimiter limiter = redissonClient.getRateLimiter(redisKey);

        // Configures the limiter once per key
        if (!limiter.isExists()) {
            // /api/auth/*: 10/min (auth protection)
            if (url.startsWith("/api/auth")) {
                limiter.trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.MINUTES);
            }
            // /api/admin/*, /api/orders/*, *holds*: 20/min (admin/order/hold protection)
            else if (url.startsWith("/api/orders") || url.contains("/holds") || url.startsWith("/api/admin")) {
                limiter.trySetRate(RateType.OVERALL, 20, 1, RateIntervalUnit.MINUTES);
            }
            // /api/inventory/*, *seats*: 60/min (inventory/seat protection)
            else if (url.startsWith("/api/inventory") || url.contains("/seats")) {
                limiter.trySetRate(RateType.OVERALL, 60, 1, RateIntervalUnit.MINUTES);
            }
            // All others: 100/min (default)
            else {
                limiter.trySetRate(RateType.OVERALL, 100, 1, RateIntervalUnit.MINUTES);
            }
        }
        // Attempts to acquire token; returns false if limit is exceeded
        return limiter.tryAcquire(1);
    }
}