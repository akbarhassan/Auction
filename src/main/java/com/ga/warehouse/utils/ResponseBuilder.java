package com.ga.warehouse.utils;

import com.ga.warehouse.response.SuccessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public class ResponseBuilder {

    public static ResponseEntity<SuccessResponse> success(HttpStatus status, String message, Object data) {
        return ResponseEntity.status(status).body(new SuccessResponse(
                status.value(),
                message,
                data,
                LocalDateTime.now()
        ));
    }
}