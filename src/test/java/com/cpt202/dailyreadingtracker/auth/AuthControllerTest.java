package com.cpt202.dailyreadingtracker.auth;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.cpt202.dailyreadingtracker.security.SecurityService;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import com.cpt202.dailyreadingtracker.utils.FileStorageService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private SecurityService securityService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private MockMultipartFile validAvatar;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

        validAvatar = new MockMultipartFile(
                "avatar",
                "test.png",
                "image/png",
                "test-image".getBytes()
        );

        testUser = new User();
        testUser.setEmail("testuser@test.com");
        testUser.setUsername("testuser");
        testUser.setPassword("Password123");
        testUser.setConfirmPassword("Password123");
    }

    // AC_001
    @Test
    public void testGetAuthPage() throws Exception {
        mockMvc.perform(get("/auth"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/authentication"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    // AC_002
    @Test
    public void testGetAuthPageWithError() throws Exception {
        mockMvc.perform(get("/auth").param("error", "true"))
                .andExpect(model().attribute("error", "Your email or password is invalid."));
    }

    // AC_003
    @Test
    public void testProcessRegistrationWithValidData() throws Exception {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        mockMvc.perform(multipart("/register")
                        .file(validAvatar)
                        .flashAttr("user", testUser))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/verification-pending*"));
    }

    // AC_004
    @Test
    public void testProcessRegistrationWithInvalidAvatar() throws Exception {
        MockMultipartFile invalidAvatar = new MockMultipartFile(
                "avatar", "test.jpg", "image/jpg", "test-image".getBytes());

        doThrow(new IOException("JPG format not supported"))
                .when(fileStorageService).validateFile(invalidAvatar);

        mockMvc.perform(multipart("/register")
                        .file(invalidAvatar)
                        .flashAttr("user", testUser))
                .andExpect(model().attributeHasFieldErrors("user", "avatarFile"))
                .andExpect(view().name("auth/authentication"));
    }

    // AC_005
    @Test
    public void testLoginUserWithValidCredentials() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "testuser")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    // AC_006
    @Test
    public void testLoginUserWithInvalidCredentials() throws Exception {
        doThrow(new BadCredentialsException("Invalid credentials"))
                .when(securityService)
                .autoLogin(anyString(), anyString());

        mockMvc.perform(post("/login")
                        .param("username", "wrong")
                        .param("password", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth?mode=login"))
                .andExpect(flash().attributeExists("error"));
    }

    // AC_007
    @Test
    public void testLoginUserWithUnverifiedAccount() throws Exception {
        doThrow(new DisabledException("Account not verified"))
                .when(securityService)
                .autoLogin(anyString(), anyString());

        mockMvc.perform(post("/login")
                        .param("username", "unverified")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/verification-pending*"));
    }

    // AC_008
    @Test
    public void testLogout() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth?logout=true"));
    }

    // AC_009
    @Test
    public void testProcessRegistrationWithDuplicateEmail() throws Exception {
        when(userRepository.existsByEmail(testUser.getEmail())).thenReturn(true);

        mockMvc.perform(multipart("/register")
                        .file(validAvatar)
                        .flashAttr("user", testUser))
                .andExpect(model().attributeHasFieldErrors("user", "email"))
                .andExpect(model().attribute("showRegisterForm", true));
    }
}
