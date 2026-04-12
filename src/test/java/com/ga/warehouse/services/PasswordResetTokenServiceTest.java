package com.ga.warehouse.services;


import com.ga.warehouse.models.PasswordResetToken;
import com.ga.warehouse.models.User;
import com.ga.warehouse.repositories.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class PasswordResetTokenServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetTokenService resetTokenService;

    // ============ TEST DATA ============
    private User user1;
    private PasswordResetToken token1;
    private LocalDateTime now;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        user1 = new User();
        user1.setId(1L);
        user1.setEmail("user@test.com");

        token1 = new PasswordResetToken();
        token1.setId(1L);
        token1.setToken("test-reset-token-uuid");
        token1.setUser(user1);
        token1.setExpiresAt(now.plusHours(1));
        token1.setUsed(false);

        // Set @Value fields
        ReflectionTestUtils.setField(resetTokenService, "tokenExpiryHours", 1);
        ReflectionTestUtils.setField(resetTokenService, "baseUrl", "http://localhost:8080");
    }

    // ============ TEST: generateToken ============
    @Test
    @DisplayName("generateToken: returns valid UUID string")
    void generateToken_returnsValidUuidString() {
        String result = resetTokenService.generateToken();

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        // Verify it's a valid UUID format
        assertThat(UUID.fromString(result)).isNotNull();
    }

    // ============ TEST: createToken ============
    @Test
    @DisplayName("createToken: creates new token for user, deleting old ones")
    void createToken_createsNewToken_deletingOldOnes() {
        doNothing().when(tokenRepository).deleteByUserId(1L);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> {
            PasswordResetToken saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        PasswordResetToken result = resetTokenService.createToken(user1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getUser()).isEqualTo(user1);
        assertThat(result.getToken()).isNotNull();
        assertThat(result.isUsed()).isFalse();
        assertThat(result.getExpiresAt()).isAfter(now);
        assertThat(result.getExpiresAt()).isBefore(now.plusHours(2));

        verify(tokenRepository, times(1)).deleteByUserId(1L);
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
    }

    // ============ TEST: validateToken ============
    @Test
    @DisplayName("validateToken: valid unused token → returns token")
    void validateToken_validUnusedToken_returnsToken() {
        when(tokenRepository.findValidToken(eq("test-reset-token-uuid"), any(LocalDateTime.class))).thenReturn(Optional.of(token1));

        Optional<PasswordResetToken> result = resetTokenService.validateToken("test-reset-token-uuid");

        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("test-reset-token-uuid");

        verify(tokenRepository, times(1)).findValidToken(eq("test-reset-token-uuid"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("validateToken: invalid token → returns empty")
    void validateToken_invalidToken_returnsEmpty() {
        when(tokenRepository.findValidToken(eq("invalid-token"), any(LocalDateTime.class))).thenReturn(Optional.empty());

        Optional<PasswordResetToken> result = resetTokenService.validateToken("invalid-token");

        assertThat(result).isEmpty();

        verify(tokenRepository, times(1)).findValidToken(eq("invalid-token"), any(LocalDateTime.class));
    }

    // ============ TEST: markTokenAsUsed ============
    @Test
    @DisplayName("markTokenAsUsed: existing token → marks as used")
    void markTokenAsUsed_existingToken_marksAsUsed() {
        when(tokenRepository.findByToken("test-reset-token-uuid")).thenReturn(Optional.of(token1));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));

        resetTokenService.markTokenAsUsed("test-reset-token-uuid");

        assertThat(token1.isUsed()).isTrue();

        verify(tokenRepository, times(1)).findByToken("test-reset-token-uuid");
        verify(tokenRepository, times(1)).save(token1);
    }

    @Test
    @DisplayName("markTokenAsUsed: non-existing token → does nothing")
    void markTokenAsUsed_nonExistingToken_doesNothing() {
        when(tokenRepository.findByToken("non-existing")).thenReturn(Optional.empty());

        resetTokenService.markTokenAsUsed("non-existing");

        verify(tokenRepository, times(1)).findByToken("non-existing");
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
    }

    // ============ TEST: sendResetEmail ============
    @Test
    @DisplayName("sendResetEmail: creates token and sends email")
    void sendResetEmail_createsTokenAndSendsEmail() {
        doNothing().when(tokenRepository).deleteByUserId(1L);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> {
            PasswordResetToken saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString(), anyMap());

        resetTokenService.sendResetEmail(user1);

        verify(tokenRepository, times(1)).deleteByUserId(1L);
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendEmail(
                eq("user@test.com"),
                eq("Password Reset Request -  Auction House"),
                eq("password-reset"),
                anyMap()
        );
    }

    // ============ TEST: deleteExpiredTokens ============
    @Test
    @DisplayName("deleteExpiredTokens: expired tokens exist → deletes them")
    void deleteExpiredTokens_expiredTokensExist_deletesThem() {
        PasswordResetToken expiredToken1 = new PasswordResetToken();
        expiredToken1.setId(3L);
        expiredToken1.setToken("expired-1");

        PasswordResetToken expiredToken2 = new PasswordResetToken();
        expiredToken2.setId(4L);
        expiredToken2.setToken("expired-2");

        when(tokenRepository.findByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredToken1, expiredToken2));
        doNothing().when(tokenRepository).deleteAll(anyList());

        resetTokenService.deleteExpiredTokens();

        verify(tokenRepository, times(1)).findByExpiresAtBefore(any(LocalDateTime.class));
        verify(tokenRepository, times(1)).deleteAll(Arrays.asList(expiredToken1, expiredToken2));
    }

    @Test
    @DisplayName("deleteExpiredTokens: no expired tokens → does nothing")
    void deleteExpiredTokens_noExpiredTokens_doesNothing() {
        when(tokenRepository.findByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        resetTokenService.deleteExpiredTokens();

        verify(tokenRepository, times(1)).findByExpiresAtBefore(any(LocalDateTime.class));
        verify(tokenRepository, never()).deleteAll(anyList());
    }
}
