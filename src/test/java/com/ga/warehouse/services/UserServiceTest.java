package com.ga.warehouse.services;


import com.ga.warehouse.enums.UserStatus;
import com.ga.warehouse.exceptions.ResourceAlreadyExistsException;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.Role;
import com.ga.warehouse.models.User;
import com.ga.warehouse.repositories.RoleRepository;
import com.ga.warehouse.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // ============ TEST DATA ============
    private User user1;
    private User user2;
    private Role adminRole;
    private Role userRole;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ADMIN");

        userRole = new Role();
        userRole.setId(2L);
        userRole.setName("USER");

        user1 = new User();
        user1.setId(1L);
        user1.setEmail("admin@test.com");
        user1.setPassword("encoded-password-1");
        user1.setRole(adminRole);
        user1.setStatus(UserStatus.ACTIVE);
        user1.setEmailVerified(true);
        user1.setDeleted(false);

        user2 = new User();
        user2.setId(2L);
        user2.setEmail("user@test.com");
        user2.setPassword("encoded-password-2");
        user2.setRole(userRole);
        user2.setStatus(UserStatus.PENDING);
        user2.setEmailVerified(false);
        user2.setDeleted(false);
    }

    // ============ TEST: getAllUsers ============
    @Test
    @DisplayName("getAllUsers: returns list of all users")
    void getAllUsers_returnsAllUsers() {
        List<User> expectedUsers = Arrays.asList(user1, user2);

        when(userRepository.findAll()).thenReturn(expectedUsers);

        List<User> result = userService.getAllUsers();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(user1, user2);

        verify(userRepository, times(1)).findAll();
        verifyNoMoreInteractions(userRepository);
    }

    // ============ TEST: getUserById ============
    @Test
    @DisplayName("getUserById: returns user based on ID")
    void getUserById_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        User result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("admin@test.com");

        verify(userRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("getUserById: non-existing ID → throws ResourceNotFoundException")
    void getUserById_nonExistingId_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with id : 999 not found");

        verify(userRepository, times(1)).findById(999L);
        verifyNoMoreInteractions(userRepository);
    }

    // ============ TEST: findUserByEmailAddress ============
    @Test
    @DisplayName("findUserByEmailAddress: returns user based on email")
    void findUserByEmailAddress_returnsUser() {
        when(userRepository.findUserByEmail("admin@test.com")).thenReturn(Optional.of(user1));

        User result = userService.findUserByEmailAddress("admin@test.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("admin@test.com");

        verify(userRepository, times(1)).findUserByEmail("admin@test.com");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("findUserByEmailAddress: non-existing email → throws ResourceNotFoundException")
    void findUserByEmailAddress_nonExistingEmail_throwsResourceNotFoundException() {
        when(userRepository.findUserByEmail("nonexisting@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserByEmailAddress("nonexisting@test.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with email : nonexisting@test.com not found");

        verify(userRepository, times(1)).findUserByEmail("nonexisting@test.com");
        verifyNoMoreInteractions(userRepository);
    }

    // ============ TEST: createUser ============
    @Test
    @DisplayName("createUser: valid user with role → saves and returns user")
    void createUser_validUserWithRole_savesAndReturnsUser() {
        User newUser = new User();
        newUser.setEmail("newuser@test.com");
        newUser.setPassword("plain-password");
        newUser.setRole(userRole);

        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User saved = i.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        User result = userService.createUser(newUser);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getEmail()).isEqualTo("newuser@test.com");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.getRole()).isEqualTo(userRole);
        assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(result.isDeleted()).isFalse();
        assertThat(result.getEmailVerified()).isTrue();

        verify(userRepository, times(1)).existsByEmail("newuser@test.com");
        verify(roleRepository, times(1)).findById(2L);
        verify(passwordEncoder, times(1)).encode("plain-password");
        verify(userRepository, times(1)).save(newUser);
    }

    @Test
    @DisplayName("createUser: duplicate email → throws ResourceAlreadyExistsException")
    void createUser_duplicateEmail_throwsResourceAlreadyExistsException() {
        User newUser = new User();
        newUser.setEmail("admin@test.com");
        newUser.setPassword("password");
        newUser.setRole(userRole);

        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("User with email : admin@test.comalready exists");

        verify(userRepository, times(1)).existsByEmail("admin@test.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("createUser: missing role → throws ResourceNotFoundException")
    void createUser_missingRole_throwsResourceNotFoundException() {
        User newUser = new User();
        newUser.setEmail("newuser@test.com");
        newUser.setPassword("password");

        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role is required");
    }

    @Test
    @DisplayName("createUser: non-existing role ID → throws ResourceNotFoundException")
    void createUser_nonExistingRole_throwsResourceNotFoundException() {
        User newUser = new User();
        newUser.setEmail("newuser@test.com");
        newUser.setPassword("password");
        newUser.setRole(userRole);

        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        when(roleRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role with id 2 not found");

        verify(userRepository, times(1)).existsByEmail("newuser@test.com");
        verify(roleRepository, times(1)).findById(2L);
    }

    // ============ TEST: updateUser ============
    @Test
    @DisplayName("updateUser: valid email update → updates and returns user")
    void updateUser_validEmailUpdate_updatesAndReturnsUser() {
        User updateRequest = new User();
        updateRequest.setEmail("updated@test.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.existsByEmail("updated@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(1L, updateRequest);

        assertThat(result.getEmail()).isEqualTo("updated@test.com");

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).existsByEmail("updated@test.com");
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    @DisplayName("updateUser: same email → no change to email")
    void updateUser_sameEmail_noChangeToEmail() {
        User updateRequest = new User();
        updateRequest.setEmail("admin@test.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(1L, updateRequest);

        assertThat(result.getEmail()).isEqualTo("admin@test.com");

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    @DisplayName("updateUser: duplicate email → throws ResourceAlreadyExistsException")
    void updateUser_duplicateEmail_throwsResourceAlreadyExistsException() {
        User updateRequest = new User();
        updateRequest.setEmail("user@test.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("User with email : user@test.comalready exists");

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).existsByEmail("user@test.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser: update role → updates user role")
    void updateUser_updateRole_updatesUserRole() {
        User updateRequest = new User();
        updateRequest.setRole(userRole);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(1L, updateRequest);

        assertThat(result.getRole()).isEqualTo(userRole);

        verify(userRepository, times(1)).findById(1L);
        verify(roleRepository, times(1)).findById(2L);
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    @DisplayName("updateUser: update status → updates user status")
    void updateUser_updateStatus_updatesUserStatus() {
        User updateRequest = new User();
        updateRequest.setStatus(UserStatus.INACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(1L, updateRequest);

        assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(user1);
    }

    // ============ TEST: deleteUserById ============
    @Test
    @DisplayName("deleteUserById: existing ID → soft deletes user")
    void deleteUserById_existingId_softDeletesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.deleteUserById(1L);

        assertThat(result.isDeleted()).isTrue();

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(user1);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("deleteUserById: already deleted → returns user without saving")
    void deleteUserById_alreadyDeleted_returnsUserWithoutSaving() {
        user1.setDeleted(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        User result = userService.deleteUserById(1L);

        assertThat(result.isDeleted()).isTrue();

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("deleteUserById: missing ID → throws ResourceNotFoundException")
    void deleteUserById_missingId_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with id: 999 not found");

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
    }
}
