package com.cpt202.dailyreadingtracker.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

/**
 * Service responsible for handling file storage operations, including:
 * <ul>
 *     <li>Storing user avatar files</li>
 *     <li>Validating file types and sizes</li>
 *     <li>Loading stored avatar files</li>
 *     <li>Deleting avatar files</li>
 * </ul>
 */

@Service
public class FileStorageService {

    private final Path storageBasePath;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    public FileStorageService(@Value("${app.uploads.dir}") String uploadDirectory){
        this.storageBasePath = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    /**
     * Creates the upload directory if it does not already exist.
     */
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

    /**
     * Validates the uploaded file for type and size constraints.
     *
     * @param file the file to validate
     * @throws IOException if the file is invalid
     */
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

    /**
     * Stores the user's avatar file in the upload directory.
     *
     * @param file     the avatar file to store
     * @param username the username of the user
     * @return the filename of the stored avatar
     * @throws IOException if the file cannot be stored
     */
    public String storeAvatar(MultipartFile file, String username) throws IOException {
        if (file == null || file.isEmpty())
            return null;

        validateFile(file);

        String filename = generateUniqueFilename(file, username);
        Path destinationPath = storageBasePath.resolve(filename);

        try {
            file.transferTo(destinationPath);
            logger.info("Stored avatar for {} at: {}", username, destinationPath);
            return filename;
        } catch (IOException e){
            logger.error("Failed to store avatar for {}", username, e);
            throw new IOException("Failed to store avatar file", e);
        }
    }

    /**
     * Generates a unique filename for the uploaded file.
     *
     * @param file     the uploaded file
     * @param username the username of the user
     * @return the generated unique filename
     */
    public String generateUniqueFilename(MultipartFile file, String username){
        String extension = file.getContentType().split("/")[1];
        return String.format("%s-avatar-%s.%s", formatFilename(username), UUID.randomUUID(), extension);
    }

    /**
     * Formats a filename by replacing invalid characters with underscores.
     *
     * @param filename the original filename
     * @return the formatted filename
     */
    public String formatFilename(String filename){
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    /**
     * Loads the user's avatar file as a URL resource.
     *
     * @param filename the filename of the avatar
     * @return the URL resource for the avatar
     * @throws IOException if the file cannot be loaded
     */
    public UrlResource loadAvatar(String filename) throws IOException{
        if (filename == null || filename.isBlank()){
            logger.debug("Requested empty filename for avatar");
            return null;
        }

        Path filePath = storageBasePath.resolve(filename).normalize();

        if (!filePath.startsWith(storageBasePath))
            throw new IOException("Attempted path traversal attack");
        
        UrlResource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()){
            logger.warn("Avatar file not found or not readable: {}", filename);
            return null;
        }

        return resource;
    }

    /**
     * Deletes the user's avatar file from the storage directory.
     *
     * @param filename the filename of the avatar to delete
     * @throws IOException if the file cannot be deleted
     */
    public void deleteAvatar(String filename) throws IOException{
        if (filename == null || filename.isBlank())
            return;
        
        Path filePath = storageBasePath.resolve(filename).normalize();

        if (!filePath.startsWith(storageBasePath))
            throw new IOException("Attempted path traversal attack");

        try {
            Files.deleteIfExists(filePath);
            logger.info("Deleted avatar file: {}", filename);
        } catch (IOException e) {
            logger.error("Failed to delete avatar file: {}", filename, e);
            throw new IOException("Failed to delete avatar file", e);
        }
    }
}
