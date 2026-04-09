package com.ga.warehouse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for requesting a password reset email.
 * Client sends email → server sends reset link.
 */
public record PasswordResetRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email

) {}