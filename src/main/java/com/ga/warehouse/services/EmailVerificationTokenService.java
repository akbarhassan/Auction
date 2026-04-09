package com.ga.warehouse.services;


import com.ga.warehouse.models.EmailVerificationToken;
import com.ga.warehouse.models.User;
import com.ga.warehouse.repositories.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.verification.token-expiry-hours:24}")
    private int tokenExpiryHours;

    @Value("${app.verification.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Generate a random UUID token
     */
    public String generateToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Create or update verification token for a user
     */
    @Transactional
    public EmailVerificationToken createToken(User user) {
        tokenRepository.deleteByUserId(user.getId());

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpiryHours);

        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(expiresAt);
        verificationToken.setUsed(false);
        EmailVerificationToken savedToken = tokenRepository.save(verificationToken);
        log.info("🔑 Created verification token for user {}: {}", user.getId(), token);

        return savedToken;
    }

    /**
     * Validate token and return it if valid
     */
    @Transactional(readOnly = true)
    public Optional<EmailVerificationToken> validateToken(String token) {
        return tokenRepository.findValidToken(token, LocalDateTime.now());
    }

    /**
     * Mark token as used after successful verification
     */
    @Transactional
    public void markTokenAsUsed(String token) {
        tokenRepository.findByToken(token).ifPresent(t -> {
            t.setUsed(true);
            tokenRepository.save(t);
            log.info("✅ Token marked as used: {}", token);
        });
    }

    /**
     * Send verification email to user
     */
    @Transactional
    public void sendVerificationEmail(User user) {
        EmailVerificationToken token = createToken(user);
        String verificationLink = baseUrl + "/api/auth/verify-email?token=" + token.getToken();
        Map<String, Object> model = new HashMap<>();
        model.put("email", user.getEmail());
        model.put("verificationLink", verificationLink);
        model.put("expiresAt", token.getExpiresAt());

        emailService.sendEmail(
                user.getEmail(),
                "Verify email - Auction House",
                "email-verification",
                model
        );

        log.info("📧 Verification email sent to {}", user.getEmail());
    }


    /**
     * Clean up expired tokens (run periodically via @Scheduled)
     */
    @Transactional
    public void deleteExpiredTokens() {
        List<EmailVerificationToken> expiredTokens = tokenRepository.findByExpiresAtBefore(LocalDateTime.now());
        if (!expiredTokens.isEmpty()) {
            tokenRepository.deleteAll(expiredTokens);
            log.info("🗑️ Deleted {} expired verification tokens", expiredTokens.size());
        }
    }

    /**
     * Check if user's email is already verified
     */
    @Transactional(readOnly = true)
    public boolean isEmailVerified(User user) {
        return tokenRepository.findByUserId(user.getId())
                .filter(token -> token.isUsed())
                .isPresent();
    }


}
