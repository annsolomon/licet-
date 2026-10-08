package com.college.dto;

import java.time.Instant;

/**
 * Every error looks the same, so React can show it easily.
 * "layer" tells WHICH layer detected the problem (useful in the viva):
 *   Java validation | Spring exception handling | Oracle constraint | PL/SQL exception | Trigger | Database connection
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String layer) {

    public static ErrorResponse of(int status, String error, String message, String layer) {
        return new ErrorResponse(Instant.now(), status, error, message, layer);
    }
}
