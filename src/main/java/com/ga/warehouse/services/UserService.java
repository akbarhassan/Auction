package com.ga.warehouse.services;


import com.ga.warehouse.enums.UserStatus;
import com.ga.warehouse.exceptions.AuthErrorException;
import com.ga.warehouse.exceptions.ResourceAlreadyExistsException;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.Role;
import com.ga.warehouse.models.User;
import com.ga.warehouse.repositories.RoleRepository;
import com.ga.warehouse.repositories.UserRepository;
import com.ga.warehouse.security.JwtUtils;
import com.ga.warehouse.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Autowired
    public UserService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder, RoleRepository roleRepository, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.jwtUtils = jwtUtils;
    }


    /**
     *
     * @param user
     * @return
     */
    public User createUser(User user) {
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

        Role role = roleRepository.findById(user.getRole().getId()).orElseThrow(() -> new ResourceNotFoundException("Role with id " + user.getRole().getId() + " not found"));
        user.setRole(role);

        User newUser = userRepository.save(user);

        // Handle email service for activating the account
        return newUser;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User with id : " + id + " not found"));
    }

    public User updateUser(Long userId, User user) {
        User currentUser = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User with id : " + userId + " not found"));

        if (!currentUser.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new ResourceAlreadyExistsException("User with email : " + user.getEmail() + "already exists");
            }
            currentUser.setEmail(user.getEmail());
        }

        if (user.getRole() != null && user.getRole().getId() != null) {
            if (!currentUser.getRole().getId().equals(user.getRole().getId())) {
                currentUser.setRole(roleRepository.findById(user.getRole().getId()).orElseThrow(() -> new ResourceNotFoundException("Role with id : " + user.getRole().getId() + " not found")));
            }
        }

        if (user.getStatus() != null && !currentUser.getStatus().equals(user.getStatus())) {
            currentUser.setStatus(user.getStatus());
        }

        return userRepository.save(currentUser);

    }

    public User findUserByEmailAddress(String email) {
        return userRepository.findUserByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User with email : " + email + " not found"));
    }

    public User deleteUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User with id: " + id + " not found"));

        if (user.isDeleted()) {
            return user;
        }

        user.setDeleted(true);
        return userRepository.save(user);
    }

    public User registerUser(User user) {
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

        Role role = roleRepository.findById(user.getRole().getId()).orElseThrow(() -> new ResourceNotFoundException("Role with id " + user.getRole().getId() + " not found"));

        user.setRole(role);

        User newUser = userRepository.save(user);

        // Handle mail service here
        return newUser;
    }

    public String loginUser(User user) {
        User currentUser = userRepository.findUserByEmail(user.getEmail()).orElseThrow(() -> new ResourceNotFoundException("User with email : " + user.getEmail() + " not found"));

        if (!passwordEncoder.matches(user.getPassword(), currentUser.getPassword())) {
            throw new AuthErrorException("Wrong password");
        }

        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            throw new AuthErrorException("Wrong status");
        }

        MyUserDetails userDetails = new MyUserDetails(currentUser);

        return jwtUtils.generateJwtToken(userDetails);
    }
}
