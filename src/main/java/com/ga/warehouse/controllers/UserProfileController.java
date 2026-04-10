package com.ga.warehouse.controllers;


import com.ga.warehouse.models.UserProfile;
import com.ga.warehouse.repositories.UserProfileRepository;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.services.UserProfileService;
import com.ga.warehouse.utils.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/{userId}/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping
    public ResponseEntity<SuccessResponse> getProfile(@PathVariable Long userId) {
        UserProfile profile = profileService.getProfileByUserId(userId);
        return ResponseBuilder.success(HttpStatus.OK, "Profile retrieved successfully", profile);
    }

    @PutMapping
    public ResponseEntity<SuccessResponse> updateProfile(@PathVariable Long userId, @RequestBody UserProfile profileData) {
        UserProfile updatedProfile = profileService.saveOrUpdate(userId, profileData);
        return ResponseBuilder.success(HttpStatus.OK, "Profile updated successfully", updatedProfile);
    }

    @PostMapping("/picture")
    public ResponseEntity<SuccessResponse> uploadPicture(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        UserProfile updatedProfile = profileService.uploadProfilePicture(userId, file);
        return ResponseBuilder.success(HttpStatus.OK, "Profile picture uploaded successfully", updatedProfile);
    }
}
