package com.ga.warehouse.services;


import com.ga.warehouse.dto.RegisterRequest;
import com.ga.warehouse.enums.UserStatus;
import com.ga.warehouse.exceptions.AuthErrorException;
import com.ga.warehouse.exceptions.ResourceAlreadyExistsException;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.*;
import com.ga.warehouse.repositories.PasswordHistoryRepository;
import com.ga.warehouse.repositories.RoleRepository;
import com.ga.warehouse.repositories.UserRepository;
import com.ga.warehouse.security.JwtUtils;
import com.ga.warehouse.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationTokenService tokenService;
    private final PasswordResetTokenService resetTokenService;
    private final PasswordHistoryRepository passwordHistoryRepository;

    @Value("${app.auth.default-role-id:2}")
    private Long defaultRoleId;

    @Value("${app.password-history.check-recent-count:10}")
    private int passwordHistoryCheckCount;

    /**
     * Register a new user (email NOT verified yet)
     */
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("User with email : " + request.getEmail() + "already exists");
        }

        Role role = roleRepository.findById(defaultRoleId).orElseThrow(() -> new ResourceNotFoundException("Default role (ID: " + defaultRoleId + ") not found. Please seed roles first."));

        User user = new User();
        user.setEmail(request.getEmail());
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);
        user.setRole(role);
        user.setEmailVerified(false);
        user.setDeleted(false);
        user.setStatus(UserStatus.PENDING);

        User savedUser = userRepository.save(user);
        savePasswordToHistory(savedUser, encodedPassword);


        // TODO: make this to a queue instead, thread it
        tokenService.sendVerificationEmail(savedUser);

        return savedUser;
    }


    /**
     * Login user and return JWT token
     */
    @Transactional
    public String login(String email, String password) {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            throw new AuthErrorException("Wrong status");
        }

        if (!user.getEmailVerified()) {
            throw new AuthErrorException("Please verify your email before logging in. Check your inbox.");
        }

        if (Boolean.TRUE.equals(user.isDeleted())) {
            throw new AuthErrorException("Account has been deleted");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthErrorException("Incorrect password");
        }

        MyUserDetails userDetails = new MyUserDetails(user);

        return jwtUtils.generateJwtToken(userDetails);
    }

    /**
     * Verify email using token
     */
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenService.validateToken(token).orElseThrow(
                () -> new AuthErrorException("Invalid or expired verification token. Please register again.")
        );

        User user = verificationToken.getUser();
        tokenService.markTokenAsUsed(token);

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }


    /**
     * Resend verification email
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with email: " + email + " not found"
                ));

        if (user.getEmailVerified()) {
            throw new ResourceAlreadyExistsException(
                    "Email is already verified. You can log in now."
            );
        }

        tokenService.sendVerificationEmail(user);
    }

    /**
     * Request password reset (send email with reset link)
     */
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with email: " + email + " not found"
                ));

        // Send reset email (always, to prevent email enumeration)
        resetTokenService.sendResetEmail(user);
    }

    /**
     * Reset password using token
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenService.validateToken(token)
                .orElseThrow(() -> new AuthErrorException(
                        "Invalid or expired password reset token"
                ));

        User user = resetToken.getUser();

        if (isPasswordRecentlyUsed(user, newPassword)) {
            throw new AuthErrorException(
                    "You cannot reuse a password from the last 10 changes. Please choose a different password."
            );
        }

        savePasswordToHistory(user, user.getPassword());


        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetTokenService.markTokenAsUsed(token);
    }

    /**
     * Save password to history
     */
    private void savePasswordToHistory(User user, String passwordHash) {
        PasswordHistory history = new PasswordHistory();
        history.setUser(user);
        history.setPasswordHash(passwordHash);
        passwordHistoryRepository.save(history);

        passwordHistoryRepository.deleteOldPasswords(user.getId());
    }


    /**
     * Check if password was recently used
     */
    private boolean isPasswordRecentlyUsed(User user, String newPassword) {
        List<PasswordHistory> recentPasswords = passwordHistoryRepository.findRecentPasswordsByUserId(user.getId());

        return recentPasswords.stream()
                .limit(passwordHistoryCheckCount)
                .anyMatch(history -> passwordEncoder.matches(newPassword, history.getPasswordHash()));
    }

}
