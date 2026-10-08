package com.college.portals.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Same salted SHA-256 as the main application, so one app_user table serves every login. */
public final class PasswordUtil {
    private PasswordUtil() { }

    public static String hash(String password) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(("college-salt:" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
