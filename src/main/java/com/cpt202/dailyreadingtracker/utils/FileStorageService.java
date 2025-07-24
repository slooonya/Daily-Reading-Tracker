package com.cpt202.dailyreadingtracker.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

// Service for handling file storage operations, particularly user avatar images.

@Service
public class FileStorageService {

    private final Path storageBasePath;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    public FileStorageService(@Value("${app.uploads.dir}") String uploadDirectory){
        this.storageBasePath = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init(){
        try{
            Files.createDirectories(storageBasePath);
            logger.info("Created upload directory at: {}", storageBasePath);
        } catch(IOException e){
            logger.error("Could not create upload directory", e);
            throw new RuntimeException("Failed to create upload directory", e);
        }
    }

    public void validateFile(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        if (contentType != null && contentType.equalsIgnoreCase("image/jpg"))
            throw new IOException("JPG format not supported. Please use JPEG or PNG");

        if (!ALLOWED_IMAGE_TYPES.contains(contentType))
            throw new IOException("Invalid file type. Only JPEG/PNG images are allowed");

        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".jpg"))
            throw new IOException("Files with .jpg extension are not accepted. Please use .jpeg or .png");

        if (file.getSize() > MAX_FILE_SIZE)
            throw new IOException("File size exceeds 5 MB");
    }

    public String storeAvatar(MultipartFile file, String username) throws IOException {
        return "";
    }
}
