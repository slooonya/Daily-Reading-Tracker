package com.cpt202.dailyreadingtracker.user;


import com.cpt202.dailyreadingtracker.utils.FileStorageService;
import com.cpt202.dailyreadingtracker.utils.EmailService;
import com.cpt202.dailyreadingtracker.violationlog.ViolationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ViolationLogRepository violationLogRepository;

    @Mock
    private Model model;

    @InjectMocks
    private UserController userController;

    @Mock
    private Principal mockPrincipal;

    private User testUser;
    private final String testEmail = "test@test.com";
    private final String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(testEmail);
        testUser.setUsername(testUsername);
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);

        ReflectionTestUtils.setField(userController, "host", "http://localhost:8080");
    }

    // UC_001
    @Test
    public void testGetUserProfilePageWithValidUser() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(mockPrincipal.getName()).thenReturn(testEmail);

        String viewName = userController.getUserProfilePage(model, mockPrincipal);

        assertEquals("readinglog/profile", viewName);
        verify(model).addAttribute("user", testUser);
        verify(model).addAttribute("imgHost", "http://localhost:8080");
    }

    // UC_002
    @Test
    public void testGetUserProfilePageWithInvalidUser() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(mockPrincipal.getName()).thenReturn(testEmail);

        String viewName = userController.getUserProfilePage(model, mockPrincipal);

        assertEquals("readinglog/profile", viewName);
        verify(model).addAttribute("error", "Failed to load user profile");
    }

    // UC_003
    @Test
    public void testGetAdminProfilePageWithValidUser() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(mockPrincipal.getName()).thenReturn(testEmail);

        String viewName = userController.getAdminProfilePage(model, mockPrincipal);

        assertEquals("administration/admin-profile", viewName);
        verify(model).addAttribute("user", testUser);
        verify(model).addAttribute("imgHost", "http://localhost:8080");
    }

    // UC_004
    @Test
    public void testUpdateAvatarWithValidFile() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileStorageService.storeAvatar(any(), any())).thenReturn("new-avatar.jpg");

        ResponseEntity<?> response = userController.updateAvatar(1L, mockFile);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fileStorageService).storeAvatar(mockFile, testUsername);
        verify(userRepository).save(testUser);
    }

    // UC_005
    @Test
    public void testUpdateAvatarWithEmptyFile() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(true);

        ResponseEntity<?> response = userController.updateAvatar(1L, mockFile);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("File is empty", response.getBody());
    }

    // UC_006
    @Test
    public void testUpdateAvatarWithInvalidFileType() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("text/plain");

        ResponseEntity<?> response = userController.updateAvatar(1L, mockFile);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Only JPEG/PNG images allowed", response.getBody());
    }

    // UC_007
    @Test
    public void testChangePasswordWithValidCurrentPassword() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("current123");
        request.setNewPassword("new123");
        request.setConfirmPassword("new123");

        when(mockPrincipal.getName()).thenReturn(testEmail);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("current123", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("new123")).thenReturn("newEncodedPassword");

        ResponseEntity<?> response = userController.changePassword(request, mockPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password changed successfully", response.getBody());
        verify(emailService).sendPasswordChangeNotification(testUser);
    }

    // UC_008
    @Test
    public void testChangePasswordWithInvalidCurrentPassword() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("wrong123");
        request.setNewPassword("new123");
        request.setConfirmPassword("new123");


        when(mockPrincipal.getName()).thenReturn(testEmail);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong123", "encodedPassword")).thenReturn(false);

        ResponseEntity<?> response = userController.changePassword(request, mockPrincipal);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Current password is incorrect", response.getBody());
    }

    // UC_009
    @Test
    public void testUpdateUserProfileWithExistingUsername() {
        Map<String, Object> updates = Map.of("username", "existinguser");
        when(mockPrincipal.getName()).thenReturn(testEmail);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> {
            userController.updateUserProfile(updates, mockPrincipal);
        });
    }

}
