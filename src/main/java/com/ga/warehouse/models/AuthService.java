package com.ga.warehouse.models;


import com.ga.warehouse.enums.UserStatus;
import com.ga.warehouse.exceptions.AuthErrorException;
import com.ga.warehouse.exceptions.ResourceAlreadyExistsException;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.repositories.RoleRepository;
import com.ga.warehouse.repositories.UserRepository;
import com.ga.warehouse.security.JwtUtils;
import com.ga.warehouse.security.MyUserDetails;
import com.ga.warehouse.services.EmailVerificationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationTokenService tokenService;


    @Value("${app.auth.default-role-id:2}")
    private Long defaultRoleId;

    public AuthService(PasswordEncoder passwordEncoder, EmailVerificationTokenService tokenService, JwtUtils jwtUtils, RoleRepository roleRepository, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenService = tokenService;

    }


    /**
     * Register a new user (email NOT verified yet)
     */
    @Transactional
    public User register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ResourceAlreadyExistsException("User with email : " + user.getEmail() + "already exists");
        }

        user.setDeleted(false);
        user.setEmailVerified(true);
        if (user.getStatus() == null) user.setStatus(UserStatus.PENDING);

        if (user.getRole() == null || user.getRole().getId() == null) {
            throw new ResourceNotFoundException("Role is required");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Role role = roleRepository.findById(defaultRoleId).orElseThrow(() -> new ResourceNotFoundException("Default role (ID: " + defaultRoleId + ") not found. Please seed roles first."));
        user.setRole(role);

        User newUser = userRepository.save(user);

        // TODO: make this to a queue instead, thread it
        tokenService.sendVerificationEmail(newUser);

        return newUser;
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

}
