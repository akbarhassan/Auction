package com.ga.warehouse.services;


import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.User;
import com.ga.warehouse.models.UserProfile;
import com.ga.warehouse.repositories.UserProfileRepository;
import com.ga.warehouse.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserProfileServiceTest {

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserProfileService userProfileService;

    // ============ TEST DATA ============
    private User user1;
    private UserProfile profile1;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setEmail("user@test.com");

        profile1 = new UserProfile();
        profile1.setId(1L);
        profile1.setUser(user1);
        profile1.setFullName("John Doe");
        profile1.setMobileNumber("+1234567890");
        profile1.setCountry("USA");
        profile1.setCity("New York");
        profile1.setStreet("123 Main St");
        profile1.setBuilding("Apt 4B");
        profile1.setPostalCode("10001");
        profile1.setProfilePicture("profile.jpg");
    }

    // ============ TEST: getProfileByUserId ============
    @Test
    @DisplayName("getProfileByUserId: existing user with profile → returns profile")
    void getProfileByUserId_existingUserWithProfile_returnsProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(profileRepository.findByUser(user1)).thenReturn(Optional.of(profile1));

        UserProfile result = userProfileService.getProfileByUserId(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getUser()).isEqualTo(user1);

        verify(userRepository, times(1)).findById(1L);
        verify(profileRepository, times(1)).findByUser(user1);
        verifyNoMoreInteractions(userRepository, profileRepository);
    }

    @Test
    @DisplayName("getProfileByUserId: existing user without profile → throws ResourceNotFoundException")
    void getProfileByUserId_existingUserWithoutProfile_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(profileRepository.findByUser(user1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfileByUserId(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");

        verify(userRepository, times(1)).findById(1L);
        verify(profileRepository, times(1)).findByUser(user1);
        verifyNoMoreInteractions(userRepository, profileRepository);
    }

    @Test
    @DisplayName("getProfileByUserId: non-existing user → throws ResourceNotFoundException")
    void getProfileByUserId_nonExistingUser_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfileByUserId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findById(999L);
        verifyNoInteractions(profileRepository);
    }

    // ============ TEST: saveOrUpdate ============
    @Test
    @DisplayName("saveOrUpdate: existing user without profile → creates new profile")
    void saveOrUpdate_existingUserWithoutProfile_createsNewProfile() {
        UserProfile profileData = new UserProfile();
        profileData.setFullName("Jane Smith");
        profileData.setMobileNumber("+0987654321");
        profileData.setCountry("Canada");
        profileData.setCity("Toronto");
        profileData.setStreet("456 Oak St");
        profileData.setBuilding("Suite 100");
        profileData.setPostalCode("M5H 2N2");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(profileRepository.findByUser(user1)).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(i -> {
            UserProfile saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        UserProfile result = userProfileService.saveOrUpdate(1L, profileData);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getFullName()).isEqualTo("Jane Smith");
        assertThat(result.getUser()).isEqualTo(user1);

        verify(userRepository, times(1)).findById(1L);
        verify(profileRepository, times(1)).findByUser(user1);
        verify(profileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("saveOrUpdate: existing user with profile → updates existing profile")
    void saveOrUpdate_existingUserWithProfile_updatesExistingProfile() {
        UserProfile profileData = new UserProfile();
        profileData.setFullName("Updated Name");
        profileData.setMobileNumber("+1111111111");
        profileData.setCountry("UK");
        profileData.setCity("London");
        profileData.setStreet("789 Pine St");
        profileData.setBuilding("Flat 12");
        profileData.setPostalCode("SW1A 1AA");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(profileRepository.findByUser(user1)).thenReturn(Optional.of(profile1));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile result = userProfileService.saveOrUpdate(1L, profileData);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFullName()).isEqualTo("Updated Name");
        assertThat(result.getMobileNumber()).isEqualTo("+1111111111");
        assertThat(result.getCountry()).isEqualTo("UK");
        assertThat(result.getCity()).isEqualTo("London");
        assertThat(result.getStreet()).isEqualTo("789 Pine St");
        assertThat(result.getBuilding()).isEqualTo("Flat 12");
        assertThat(result.getPostalCode()).isEqualTo("SW1A 1AA");
        assertThat(result.getUser()).isEqualTo(user1);

        verify(userRepository, times(1)).findById(1L);
        verify(profileRepository, times(1)).findByUser(user1);
        verify(profileRepository, times(1)).save(profile1);
    }

    @Test
    @DisplayName("saveOrUpdate: non-existing user → throws ResourceNotFoundException")
    void saveOrUpdate_nonExistingUser_throwsResourceNotFoundException() {
        UserProfile profileData = new UserProfile();
        profileData.setFullName("Test User");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.saveOrUpdate(999L, profileData))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findById(999L);
        verifyNoInteractions(profileRepository);
    }

    // ============ TEST: uploadProfilePicture ============
    @Test
    @DisplayName("uploadProfilePicture: existing user with profile → uploads and updates picture")
    void uploadProfilePicture_existingUserWithProfile_uploadsAndUpdatesPicture() {
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "image content".getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(profileRepository.findByUser(user1)).thenReturn(Optional.of(profile1));
        when(fileStorageService.saveFile(any(MultipartFile.class), anyLong(), anyString())).thenReturn("uploaded-photo.jpg");
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile result = userProfileService.uploadProfilePicture(1L, file);

        assertThat(result.getProfilePicture()).isEqualTo("uploaded-photo.jpg");

        verify(userRepository, times(1)).findById(1L);
        verify(profileRepository, times(1)).findByUser(user1);
        verify(fileStorageService, times(1)).saveFile(file, 1L, "profiles");
        verify(profileRepository, times(1)).save(profile1);
    }

    @Test
    @DisplayName("uploadProfilePicture: existing user without profile → creates profile and uploads picture")
    void uploadProfilePicture_existingUserWithoutProfile_createsProfileAndUploadsPicture() {
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "image content".getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(profileRepository.findByUser(user1)).thenReturn(Optional.empty());
        when(fileStorageService.saveFile(any(MultipartFile.class), anyLong(), anyString())).thenReturn("new-photo.jpg");
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(i -> {
            UserProfile saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        UserProfile result = userProfileService.uploadProfilePicture(1L, file);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getProfilePicture()).isEqualTo("new-photo.jpg");
        assertThat(result.getUser()).isEqualTo(user1);

        verify(userRepository, times(1)).findById(1L);
        verify(profileRepository, times(1)).findByUser(user1);
        verify(fileStorageService, times(1)).saveFile(file, 1L, "profiles");
        verify(profileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("uploadProfilePicture: empty file → throws IllegalArgumentException")
    void uploadProfilePicture_emptyFile_throwsIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> userProfileService.uploadProfilePicture(1L, emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is empty");
    }

    @Test
    @DisplayName("uploadProfilePicture: non-existing user → throws ResourceNotFoundException")
    void uploadProfilePicture_nonExistingUser_throwsResourceNotFoundException() {
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "image content".getBytes());

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.uploadProfilePicture(999L, file))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findById(999L);
        verifyNoInteractions(profileRepository, fileStorageService);
    }
}
