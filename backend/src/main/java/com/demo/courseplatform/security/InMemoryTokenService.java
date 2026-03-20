package com.demo.courseplatform.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "demo.token.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryTokenService implements TokenService {

    private final ConcurrentMap<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();
    private final Duration ttl;

    public InMemoryTokenService(@Value("${demo.token-ttl-hours:12}") long tokenTtlHours) {
        this.ttl = Duration.ofHours(tokenTtlHours);
    }

    @Override
    public String issueToken(Long userId) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, new TokenEntry(userId, Instant.now().plus(ttl)));
        return token;
    }

    @Override
    public Optional<Long> resolveUserId(String token) {
        TokenEntry entry = tokenStore.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            tokenStore.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }

    @Override
    public void revoke(String token) {
        tokenStore.remove(token);
    }

    private record TokenEntry(Long userId, Instant expiresAt) {
    }
}
