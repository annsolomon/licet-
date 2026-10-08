package com.college.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Password hashing. The database stores SHA-256( "college-salt:" + password ) as lower-case hex,
 * never the plain password. 04_sample_data.sql computes the same value with
 * LOWER(RAWTOHEX(STANDARD_HASH('college-salt:admin123','SHA256'))).
 *
 * (A production system would use BCrypt; SHA-256 keeps this project dependency-free and explainable.)
 */
public final class PasswordUtil {

    private static final String SALT = "college-salt:";

    private PasswordUtil() { }

    public static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((SALT + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
