package com.ga.warehouse.services;

import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.User;
import com.ga.warehouse.models.UserProfile;
import com.ga.warehouse.repositories.UserProfileRepository;
import com.ga.warehouse.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public UserProfile getProfileByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
    }

    @Transactional
    public UserProfile saveOrUpdate(Long userId, UserProfile profileData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfile profile = profileRepository.findByUser(user).orElse(new UserProfile());
        profile.setUser(user);
        profile.setFullName(profileData.getFullName());
        profile.setMobileNumber(profileData.getMobileNumber());
        profile.setCountry(profileData.getCountry());
        profile.setCity(profileData.getCity());
        profile.setStreet(profileData.getStreet());
        profile.setBuilding(profileData.getBuilding());
        profile.setPostalCode(profileData.getPostalCode());

        return profileRepository.save(profile);
    }

    @Transactional
    public UserProfile uploadProfilePicture(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfile profile = profileRepository.findByUser(user)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        String fileName = fileStorageService.saveFile(file, userId, "profiles");

        profile.setProfilePicture(fileName);

        return profileRepository.save(profile);
    }
}