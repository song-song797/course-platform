package com.demo.courseplatform.security;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final String keyPrefix;
    private final Duration ttl;

    public TokenService(StringRedisTemplate stringRedisTemplate,
                        @Value("${demo.token-prefix:course-platform:token:}") String keyPrefix,
                        @Value("${demo.token-ttl-hours:12}") long tokenTtlHours) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.keyPrefix = keyPrefix;
        this.ttl = Duration.ofHours(tokenTtlHours);
    }

    public String issueToken(Long userId) {
        String token = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(buildKey(token), String.valueOf(userId), ttl);
        return token;
    }

    public Optional<Long> resolveUserId(String token) {
        String value = stringRedisTemplate.opsForValue().get(buildKey(token));
        return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
    }

    public void revoke(String token) {
        stringRedisTemplate.delete(buildKey(token));
    }

    private String buildKey(String token) {
        return keyPrefix + token;
    }
}
