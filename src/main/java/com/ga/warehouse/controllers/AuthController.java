package com.ga.warehouse.controllers;


import com.ga.warehouse.dto.LoginRequest;
import com.ga.warehouse.dto.PasswordChangeRequest;
import com.ga.warehouse.dto.PasswordResetRequest;
import com.ga.warehouse.dto.RegisterRequest;
import com.ga.warehouse.services.AuthService;
import com.ga.warehouse.models.User;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.utils.ResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    /**
     *
     * @param loginRequest
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<SuccessResponse> login(@RequestBody LoginRequest loginRequest) {
        String token = authService.login(loginRequest.getEmail(), loginRequest.getPassword());
        return ResponseBuilder.success(HttpStatus.OK, "Login successful", Map.of("token", token));

    }

    /**
     *
     * @param registerRequest
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<SuccessResponse> register(@RequestBody RegisterRequest registerRequest) {
        User newUser = authService.register(registerRequest);
        return ResponseBuilder.success(HttpStatus.CREATED, "Register successful", newUser);
    }


    /**
     *
     * @param token
     * @return
     */
    @GetMapping("/verify-email")
    public ResponseEntity<SuccessResponse> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseBuilder.success(
                HttpStatus.OK,
                "Email verified successfully. You can now log in.",
                null
        );
    }

    /**
     * Resend verification email (if user didn't receive it)
     * POST /api/v1/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<SuccessResponse> resendVerification(@RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseBuilder.success(
                HttpStatus.OK,
                "Verification email resent. Please check your inbox.",
                null
        );
    }

    /**
     * Request password reset (send email with reset link)
     * POST /api/v1/auth/request-password-reset
     */
    @PostMapping("/request-password-reset")
    public ResponseEntity<SuccessResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseBuilder.success(
                HttpStatus.OK,
                "If an account exists with this email, a password reset link has been sent.",
                null
        );
    }

    /**
     * Reset password using token from email
     * POST /api/v1/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<SuccessResponse> resetPassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ResponseBuilder.success(
                HttpStatus.OK,
                "Password reset successful. You can now log in with your new password.",
                null
        );
    }
}
