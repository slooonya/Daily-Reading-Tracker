package com.cpt202.dailyreadingtracker.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.cpt202.dailyreadingtracker.security.SecurityService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class PasswordResetControllerTest {

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private PasswordResetController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // PRC_001
    @Test
    public void testShowForgotPasswordFormAuthenticated() throws Exception {
        when(securityService.isAuthenticated()).thenReturn(true);

        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    // PRC_002
    @Test
    public void testShowForgotPasswordFormNotAuthenticated() throws Exception {
        when(securityService.isAuthenticated()).thenReturn(false);

        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"));
    }

    // PRC_003
    @Test
    public void testRequestPasswordResetWithValidEmail() throws Exception {
        when(passwordResetService.requestPasswordReset(anyString(), any()))
                .thenReturn("If this email exists, a reset link has been sent");

        mockMvc.perform(post("/forgot-password")
                        .param("email", "testuser@test.com"))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/forgot-password"))
                        .andExpect(flash().attributeExists("message"));
    }

    // PRC_004
    @Test
    public void testShowResetFormWithValidToken() throws Exception {
        when(passwordResetService.validatePasswordResetToken("validToken"))
                .thenReturn("valid");

        mockMvc.perform(get("/reset-password")
                        .param("token", "validToken"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("auth/password-reset-form"))
                        .andExpect(model().attributeExists("token"));
    }

    // PRC_005
    @Test
    public void testProcessPasswordResetWithValidData() throws Exception {
        when(passwordResetService.processPasswordReset("validToken", "newPass123"))
                .thenReturn("Password reset successfully");

        mockMvc.perform(post("/reset-password")
                        .param("token", "validToken")
                        .param("password", "newPass123")
                        .param("confirmPassword", "newPass123"))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/auth"))
                        .andExpect(flash().attributeExists("message"));
    }

    // PRC_006
    @Test
    public void testProcessPasswordResetWithPasswordMismatch() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", "validToken")
                        .param("password", "newPass123")
                        .param("confirmPassword", "wrongPass"))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrlPattern("/reset-password?token=validToken&error=Passwords+don%27t+match"));
    }
}
