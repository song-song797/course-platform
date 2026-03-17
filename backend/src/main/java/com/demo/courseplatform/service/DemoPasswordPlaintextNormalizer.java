package com.demo.courseplatform.service;

import com.demo.courseplatform.domain.entity.PersistenceModels.UserEntity;
import com.demo.courseplatform.mapper.UserMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoPasswordPlaintextNormalizer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoPasswordPlaintextNormalizer.class);

    private final UserMapper userMapper;
    private final PasswordService passwordService;

    public DemoPasswordPlaintextNormalizer(UserMapper userMapper, PasswordService passwordService) {
        this.userMapper = userMapper;
        this.passwordService = passwordService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<UserEntity> users = loadAllUsers();
        int normalizedCount = 0;
        for (UserEntity user : users) {
            if (!passwordService.isLegacyEncoded(user.passwordHash)) {
                continue;
            }
            if (!isDemoAccount(user.username)) {
                continue;
            }
            user.passwordHash = user.username;
            userMapper.updatePassword(user);
            normalizedCount++;
        }
        if (normalizedCount > 0) {
            log.info("Normalized {} demo user passwords to plaintext storage", normalizedCount);
        }
    }

    private List<UserEntity> loadAllUsers() {
        Map<Long, UserEntity> usersById = new LinkedHashMap<>();
        for (String role : List.of("ADMIN", "TEACHER", "STUDENT")) {
            for (UserEntity user : userMapper.findByRole(role)) {
                usersById.put(user.id, user);
            }
        }
        return new ArrayList<>(usersById.values());
    }

    private boolean isDemoAccount(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return "admin".equals(username)
            || username.matches("t\\d{3}")
            || username.matches("s\\d{3}")
            || username.startsWith("bulk_");
    }
}
