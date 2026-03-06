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
     * Checks if a request from a given IP to a specific endpoint is allowed based on the rate limit.
     * @param ip the IP address of the client making the request
     * @param url the URL being accessed
     * @return true if the request is allowed, false if it exceeds the rate limit 
     */
    public boolean isAllowed(String ip, String url) {
        String key = "rate_limit:" + ip + ":" + url;

        // Retrieves rate limiter from Redis
        RRateLimiter limiter = redissonClient.getRateLimiter(key);

        // Configures the limiter once per key
        if (!limiter.isExists()) {
            // Limit to 3 requests per 30 seconds
            limiter.trySetRate(RateType.OVERALL, 3, 30, RateIntervalUnit.SECONDS);
        }
        // Attempts to acquire token; returns false if limit is exceeded
        return limiter.tryAcquire(1);
    }
}