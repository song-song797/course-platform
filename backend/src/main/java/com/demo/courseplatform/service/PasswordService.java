package com.demo.courseplatform.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    public String encode(String rawPassword) {
        return rawPassword;
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return rawPassword.equals(encodedPassword) || legacyEncode(rawPassword).equals(encodedPassword);
    }

    public boolean isLegacyEncoded(String value) {
        return value != null && value.matches("[0-9a-fA-F]{64}");
    }

    public String legacyEncode(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString().toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("密码加密算法不可用", exception);
        }
    }
}
