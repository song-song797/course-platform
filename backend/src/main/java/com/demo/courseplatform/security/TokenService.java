package com.demo.courseplatform.security;

import java.util.Optional;

public interface TokenService {

    String issueToken(Long userId);

    Optional<Long> resolveUserId(String token);

    void revoke(String token);
}
