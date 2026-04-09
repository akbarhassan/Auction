package com.ga.warehouse.services;


import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.User;
import com.ga.warehouse.models.UserProfile;
import com.ga.warehouse.repositories.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    /**
     * Create or update profile for a user
     */
    @Transactional
    public UserProfile saveProfile(User user, UserProfile profileData) {
        return userProfileRepository.findByUser(user)
                .map(existingProfile -> {
                    // Update existing profile
                    existingProfile.setFullName(profileData.getFullName());
                    existingProfile.setMobileNumber(profileData.getMobileNumber());
                    existingProfile.setProfilePicture(profileData.getProfilePicture());
                    existingProfile.setCountry(profileData.getCountry());
                    existingProfile.setCity(profileData.getCity());
                    existingProfile.setStreet(profileData.getStreet());
                    existingProfile.setBuilding(profileData.getBuilding());
                    existingProfile.setPostalCode(profileData.getPostalCode());
                    return userProfileRepository.save(existingProfile);
                })
                .orElseGet(() -> {
                    // Create new profile
                    UserProfile newProfile = UserProfile.builder()
                            .user(user)
                            .fullName(profileData.getFullName())
                            .mobileNumber(profileData.getMobileNumber())
                            .profilePicture(profileData.getProfilePicture())
                            .country(profileData.getCountry())
                            .city(profileData.getCity())
                            .street(profileData.getStreet())
                            .building(profileData.getBuilding())
                            .postalCode(profileData.getPostalCode())
                            .build();
                    return userProfileRepository.save(newProfile);
                });
    }


    /**
     * Get profile by user
     */
    @Transactional(readOnly = true)
    public UserProfile getProfileByUser(User user) {
        return userProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profile not found for user: " + user.getId()
                ));
    }

    /**
     * Get profile by user ID
     */
    @Transactional(readOnly = true)
    public UserProfile getProfileByUserId(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profile not found for user ID: " + userId
                ));
    }


}
