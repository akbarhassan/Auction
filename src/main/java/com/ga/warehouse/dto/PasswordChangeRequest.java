package com.ga.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(

        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")

        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain uppercase, lowercase, and number")
        String password

) {
}