package com.ga.warehouse.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.path:./uploads}")
    private String uploadDir;

    public String saveFile(MultipartFile file, Long entityId, String subPath) {
        try {
            Path targetPath = Paths.get(uploadDir, subPath);
            Files.createDirectories(targetPath);

            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String fileName = entityId + "_" + System.currentTimeMillis() + "." + extension;

            String contentType = file.getContentType();
            if (!List.of("image/jpeg", "image/png", "image/gif").contains(contentType)) {
                throw new IllegalArgumentException("Invalid file type");
            }

            file.transferTo(Paths.get(targetPath.toString(), fileName));
            return fileName;

        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload file: " + ex.getMessage(), ex);
        }
    }
}
