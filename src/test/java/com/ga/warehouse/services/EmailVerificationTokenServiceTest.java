package com.ga.warehouse.services;


import com.ga.warehouse.models.EmailVerificationToken;
import com.ga.warehouse.models.User;
import com.ga.warehouse.repositories.EmailVerificationTokenRepository;
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
public class EmailVerificationTokenServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationTokenService tokenService;

    // ============ TEST DATA ============
    private User user1;
    private EmailVerificationToken token1;
    private LocalDateTime now;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        user1 = new User();
        user1.setId(1L);
        user1.setEmail("user@test.com");

        token1 = new EmailVerificationToken();
        token1.setId(1L);
        token1.setToken("test-token-uuid");
        token1.setUser(user1);
        token1.setExpiresAt(now.plusHours(24));
        token1.setUsed(false);

        // Set @Value fields
        ReflectionTestUtils.setField(tokenService, "tokenExpiryHours", 24);
        ReflectionTestUtils.setField(tokenService, "baseUrl", "http://localhost:8080");
    }

    // ============ TEST: generateToken ============
    @Test
    @DisplayName("generateToken: returns valid UUID string")
    void generateToken_returnsValidUuidString() {
        String result = tokenService.generateToken();

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
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> {
            EmailVerificationToken saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        EmailVerificationToken result = tokenService.createToken(user1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getUser()).isEqualTo(user1);
        assertThat(result.getToken()).isNotNull();
        assertThat(result.isUsed()).isFalse();
        assertThat(result.getExpiresAt()).isAfter(now);

        verify(tokenRepository, times(1)).deleteByUserId(1L);
        verify(tokenRepository, times(1)).save(any(EmailVerificationToken.class));
    }

    // ============ TEST: validateToken ============
    @Test
    @DisplayName("validateToken: valid unused token → returns token")
    void validateToken_validUnusedToken_returnsToken() {
        when(tokenRepository.findValidToken(eq("test-token-uuid"), any(LocalDateTime.class))).thenReturn(Optional.of(token1));

        Optional<EmailVerificationToken> result = tokenService.validateToken("test-token-uuid");

        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("test-token-uuid");

        verify(tokenRepository, times(1)).findValidToken(eq("test-token-uuid"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("validateToken: invalid token → returns empty")
    void validateToken_invalidToken_returnsEmpty() {
        when(tokenRepository.findValidToken(eq("invalid-token"), any(LocalDateTime.class))).thenReturn(Optional.empty());

        Optional<EmailVerificationToken> result = tokenService.validateToken("invalid-token");

        assertThat(result).isEmpty();

        verify(tokenRepository, times(1)).findValidToken(eq("invalid-token"), any(LocalDateTime.class));
    }

    // ============ TEST: markTokenAsUsed ============
    @Test
    @DisplayName("markTokenAsUsed: existing token → marks as used")
    void markTokenAsUsed_existingToken_marksAsUsed() {
        when(tokenRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(token1));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        tokenService.markTokenAsUsed("test-token-uuid");

        assertThat(token1.isUsed()).isTrue();

        verify(tokenRepository, times(1)).findByToken("test-token-uuid");
        verify(tokenRepository, times(1)).save(token1);
    }

    @Test
    @DisplayName("markTokenAsUsed: non-existing token → does nothing")
    void markTokenAsUsed_nonExistingToken_doesNothing() {
        when(tokenRepository.findByToken("non-existing")).thenReturn(Optional.empty());

        tokenService.markTokenAsUsed("non-existing");

        verify(tokenRepository, times(1)).findByToken("non-existing");
        verify(tokenRepository, never()).save(any(EmailVerificationToken.class));
    }

    // ============ TEST: sendVerificationEmail ============
    @Test
    @DisplayName("sendVerificationEmail: creates token and sends email")
    void sendVerificationEmail_createsTokenAndSendsEmail() {
        doNothing().when(tokenRepository).deleteByUserId(1L);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> {
            EmailVerificationToken saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString(), anyMap());

        tokenService.sendVerificationEmail(user1);

        verify(tokenRepository, times(1)).deleteByUserId(1L);
        verify(tokenRepository, times(1)).save(any(EmailVerificationToken.class));
        verify(emailService, times(1)).sendEmail(
                eq("user@test.com"),
                eq("Verify email - Auction House"),
                eq("email-verification"),
                anyMap()
        );
    }

    // ============ TEST: deleteExpiredTokens ============
    @Test
    @DisplayName("deleteExpiredTokens: expired tokens exist → deletes them")
    void deleteExpiredTokens_expiredTokensExist_deletesThem() {
        EmailVerificationToken expiredToken1 = new EmailVerificationToken();
        expiredToken1.setId(3L);
        expiredToken1.setToken("expired-1");

        EmailVerificationToken expiredToken2 = new EmailVerificationToken();
        expiredToken2.setId(4L);
        expiredToken2.setToken("expired-2");

        when(tokenRepository.findByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredToken1, expiredToken2));
        doNothing().when(tokenRepository).deleteAll(anyList());

        tokenService.deleteExpiredTokens();

        verify(tokenRepository, times(1)).findByExpiresAtBefore(any(LocalDateTime.class));
        verify(tokenRepository, times(1)).deleteAll(Arrays.asList(expiredToken1, expiredToken2));
    }

    @Test
    @DisplayName("deleteExpiredTokens: no expired tokens → does nothing")
    void deleteExpiredTokens_noExpiredTokens_doesNothing() {
        when(tokenRepository.findByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        tokenService.deleteExpiredTokens();

        verify(tokenRepository, times(1)).findByExpiresAtBefore(any(LocalDateTime.class));
        verify(tokenRepository, never()).deleteAll(anyList());
    }

    // ============ TEST: isEmailVerified ============
    @Test
    @DisplayName("isEmailVerified: used token exists → returns true")
    void isEmailVerified_usedTokenExists_returnsTrue() {
        token1.setUsed(true);

        when(tokenRepository.findByUserId(1L)).thenReturn(Optional.of(token1));

        boolean result = tokenService.isEmailVerified(user1);

        assertThat(result).isTrue();

        verify(tokenRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("isEmailVerified: unused token exists → returns false")
    void isEmailVerified_unusedTokenExists_returnsFalse() {
        token1.setUsed(false);

        when(tokenRepository.findByUserId(1L)).thenReturn(Optional.of(token1));

        boolean result = tokenService.isEmailVerified(user1);

        assertThat(result).isFalse();

        verify(tokenRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("isEmailVerified: no token exists → returns false")
    void isEmailVerified_noTokenExists_returnsFalse() {
        when(tokenRepository.findByUserId(1L)).thenReturn(Optional.empty());

        boolean result = tokenService.isEmailVerified(user1);

        assertThat(result).isFalse();

        verify(tokenRepository, times(1)).findByUserId(1L);
    }
}
