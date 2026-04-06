package com.ga.warehouse.response;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record SuccessResponse(
        int status,
        String message,
        Object data,
        LocalDateTime timestamp
) {
}
