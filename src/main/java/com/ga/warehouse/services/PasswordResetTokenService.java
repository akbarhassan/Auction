package com.ga.warehouse.services;

import com.ga.warehouse.models.PasswordResetToken;
import com.ga.warehouse.models.User;
import com.ga.warehouse.repositories.PasswordResetTokenRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.password-reset.token-expiry-hours:1}")
    private int tokenExpiryHours;

    @Value("${app.password-reset.base-url:http://localhost:8080}")
    private String baseUrl;


    /**
     * Generate random UUID token
     */
    public String generateToken() {
        return UUID.randomUUID().toString();
    }


    /**
     * Create reset token for user (deletes old ones first)
     */
    @Transactional
    public PasswordResetToken createToken(User user) {
        tokenRepository.deleteByUserId(user.getId());

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpiryHours);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiresAt(expiresAt);
        resetToken.setUsed(false);

        PasswordResetToken savedToken = tokenRepository.save(resetToken);
        log.info("🔑 Created password reset token for user {}: {}", user.getId(), token);

        return savedToken;
    }

    /**
     * Validate token (checks expiry + used status)
     */
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> validateToken(String token) {
        return tokenRepository.findValidToken(token, LocalDateTime.now());
    }

    /**
     * Mark token as used after successful reset
     */
    @Transactional
    public void markTokenAsUsed(String token) {
        tokenRepository.findByToken(token).ifPresent(t -> {
            t.setUsed(true);
            tokenRepository.save(t);
            log.info("✅ Password reset token marked as used: {}", token);
        });
    }

    /**
     * Send password reset email
     */
    @Transactional
    public void sendResetEmail(User user) {
        PasswordResetToken token = createToken(user);
        String resetLink = baseUrl + "/api/v1/auth/reset-password?token=" + token.getToken();

        Map<String, Object> model = Map.of(
                "email", user.getEmail(),
                "resetLink", resetLink,
                "expiryHours", tokenExpiryHours
        );

        emailService.sendEmail(
                user.getEmail(),
                "Password Reset Request -  Auction House",
                "password-reset",
                model
        );

        log.info("📧 Password reset email sent to {}", user.getEmail());
    }

    /**
     * Clean up expired tokens (call via @Scheduled)
     */
    @Transactional
    public void deleteExpiredTokens() {
        List<PasswordResetToken> expiredTokens = tokenRepository.findByExpiresAtBefore(LocalDateTime.now());
        if (!expiredTokens.isEmpty()) {
            tokenRepository.deleteAll(expiredTokens);
            log.info("🗑️ Deleted {} expired password reset tokens", expiredTokens.size());
        }
    }

}
