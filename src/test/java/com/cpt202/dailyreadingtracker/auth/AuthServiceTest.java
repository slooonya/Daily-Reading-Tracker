package com.cpt202.dailyreadingtracker.auth;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.cpt202.dailyreadingtracker.role.Role;
import com.cpt202.dailyreadingtracker.role.RoleRepository;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import com.cpt202.dailyreadingtracker.utils.FileStorageService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    
    @Mock
    private FileStorageService fileStorageService;
    
    @Mock
    private VerificationService verificationTokenService;

    @Mock
    private HttpServletRequest request;
    
    @InjectMocks
    private AuthService authService;

    private User testUser;
    private MultipartFile testAvatar;
    private Role userRole, adminRole;

    @BeforeEach
    void setup(){
        testUser = new User();
        testUser.setEmail("testuser@test.com");
        testUser.setUsername("testuser");
        testUser.setPassword("Password123");
        testUser.setConfirmPassword("Password123");

        testAvatar = new MockMultipartFile("avatar", "avatar.png", "image/png", "testimage".getBytes());

        userRole = new Role("ROLE_USER");
        adminRole = new Role("ROLE_ADMIN");
    }

    // AS_001
    @Test
    public void testRegisterNewUserWithAvatar() throws Exception{
        when(userRepository.count()).thenReturn(1L);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(fileStorageService.storeAvatar(any(), anyString())).thenReturn("avatar.png");

        authService.register(testUser, testAvatar, request);

        verify(userRepository).save(testUser);
        assertEquals("encodedPassword", testUser.getPassword());
        assertNull(testUser.getConfirmPassword());
        assertEquals("avatar.png", testUser.getAvatarFileName());
        assertEquals(1, testUser.getRoles().size());
        assertTrue(testUser.getRoles().contains(userRole));
    }

    // AS_002
    @Test
    public void testRegisterFirstUserAsAdmin(){
        when(userRepository.count()).thenReturn(0L);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(adminRole);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        authService.register(testUser, null, request);

        assertTrue(testUser.getRoles().contains(adminRole));
    }

    // AS_003
    @Test
    public void testRegisterWithExistingEmail(){
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(AuthService.RegistrationException.class,
                () -> authService.register(testUser, null, request));
    }

    // AS_004
    @Test
    public void testRegisterWithPasswordMismatch(){
        testUser.setConfirmPassword("MismatchingPassword123");

        assertThrows(AuthService.RegistrationException.class,
                () -> authService.register(testUser, null, request));
    }

    // AS_005
    @Test
    public void testRegisterWithEmailVerificationFailed(){
        when(userRepository.count()).thenReturn(1L);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        doThrow(new RuntimeException("SMTP error"))
                .when(verificationTokenService)
                .createVerification(anyString(), any());

        assertThrows(AuthService.RegistrationException.class,
                () -> authService.register(testUser, null, request));
        verify(userRepository).delete(testUser);
    }

    // AS_006
    @Test
    public void testRegisterWithAvatarUploadFailed() throws IOException {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(fileStorageService.storeAvatar(any(), anyString()))
                .thenThrow(new IOException("Avatar upload failed"));

        assertThrows(RuntimeException.class,
                () -> authService.register(testUser, testAvatar, request));
        verify(userRepository, never()).save(any());
    }
}