package com.cpt202.dailyreadingtracker.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.UrlResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class FileStorageServiceTest {
    
    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;
    private MultipartFile validImage;
    private MultipartFile largeFile;
    private MultipartFile invalidTypeFile;
    private String testUsername = "testuser";

    @BeforeEach
    void setup() throws Exception {
        fileStorageService = new FileStorageService(tempDir.toString());
        fileStorageService.init();

        validImage = new MockMultipartFile(
                "avatar","test.png","image/png", new byte[1024]);

        largeFile = new MockMultipartFile(
                "avatar", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        invalidTypeFile = new MockMultipartFile(
                "avatar", "test.jpg", "image/jpg", new byte[1024]);
    }

    // FS_001
    @Test
    public void testStoreAvatarWithValidFile() throws Exception {
        String filename = fileStorageService.storeAvatar(validImage, testUsername);

        assertNotNull(filename);
        assertTrue(Files.exists(tempDir.resolve(filename)));
        assertTrue(filename.contains(testUsername) && filename.endsWith(".png"));
    }

    // FS_002
    @Test
    public void testStoreAvatarWithEmptyFile() throws Exception {
        assertNull(fileStorageService.storeAvatar(null, testUsername));
    }

    // FS_003
    @Test
    public void testStoreAvatarWithInvalidType() {
        assertThrows(IOException.class,
                () -> fileStorageService.storeAvatar(invalidTypeFile, testUsername));
    }

    // FS_004
    @Test
    public void testStoreAvatarWithLargeFile() {
        assertThrows(IOException.class,
                () -> fileStorageService.storeAvatar(largeFile, testUsername));
    }

    // FS_005
    @Test
    public void testLoadAvatarWithExistingFile() throws Exception {
        String filename = fileStorageService.storeAvatar(validImage, testUsername);
        UrlResource resource = fileStorageService.loadAvatar(filename);

        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    // FS_006
    @Test
    void testDeleteAvatarWithExistingFile() throws Exception {
        String filename = fileStorageService.storeAvatar(validImage, testUsername);
        fileStorageService.deleteAvatar(filename);

        assertFalse(Files.exists(tempDir.resolve(filename)));
    }
}
