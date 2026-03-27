package com.fairtix.config;

import org.mockito.Mockito;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Replaces the real RedissonClient with a mock for tests,
 * so tests don't need a running Redis instance.
 */
@Configuration
public class TestRedisConfig {

  @Bean
  @Primary
  public RedissonClient redissonClient() {
    RedissonClient mock = Mockito.mock(RedissonClient.class);
    RRateLimiter limiter = Mockito.mock(RRateLimiter.class);
    when(mock.getRateLimiter(anyString())).thenReturn(limiter);
    when(limiter.tryAcquire(1)).thenReturn(true);
    when(limiter.isExists()).thenReturn(false);
    when(limiter.trySetRate(Mockito.any(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any())).thenReturn(true);
    return mock;
  }
}
