package com.cpt202.dailyreadingtracker.auth;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VerificationControllerTest {

    @Mock
    private VerificationService verificationTokenService;

    @InjectMocks
    private VerificationController verificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(verificationController).build();
    }

    // VC_001
    @Test
    public void testVerifyEmailWithValidToken() throws Exception {
        when(verificationTokenService.verifyEmail("validToken"))
                .thenReturn(ResponseEntity.ok("Success"));

        mockMvc.perform(get("/verify-email")
                        .param("token", "validToken"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth?verified=true"));
    }

    // VC_002
    @Test
    public void testVerifyEmailWithInvalidToken() throws Exception {
        when(verificationTokenService.verifyEmail("invalidToken"))
                .thenReturn(ResponseEntity.badRequest().body("Invalid token"));
        when(verificationTokenService.getEmailFromToken("invalidToken"))
                .thenReturn("user@test.com");

        mockMvc.perform(get("/verify-email")
                        .param("token", "invalidToken"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/verification-error*"));
    }

    // VC_003
    @Test
    public void testVerifyEmailWithExpiredToken() throws Exception {
        when(verificationTokenService.verifyEmail("expiredToken"))
                .thenReturn(ResponseEntity.badRequest().body("Token expired"));
        when(verificationTokenService.getEmailFromToken("expiredToken"))
                .thenReturn("user@test.com");

        mockMvc.perform(get("/verify-email")
                        .param("token", "expiredToken"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verification-error?error=Token+expired&email=user%40test.com"));
    }

    // VC_004
    @Test
    public void testShowVerificationError() throws Exception {
        mockMvc.perform(get("/verification-error")
                        .param("error", "Token expired")
                        .param("email", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/verification-error"))
                .andExpect(model().attribute("error", "Token expired"))
                .andExpect(model().attribute("email", "user@test.com"));
    }

    // VC_005
    @Test
    public void testResendVerificationSuccess() throws Exception {
        mockMvc.perform(post("/resend-verification")
                        .param("email", "user@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify-pending?resent=true&email=user%40test.com"));
    }

    // VC_006
    @Test
    public void testResendVerificationFailure() throws Exception {
        doThrow(new RuntimeException("Invalid email"))
                .when(verificationTokenService)
                .createVerification(eq("invalid@test.com"), any());

        mockMvc.perform(post("/resend-verification")
                        .param("email", "invalid@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/verification-error*"));
    }

    // VC_007
    @Test
    public void testVerifyEmailWithException() throws Exception {
        when(verificationTokenService.verifyEmail("errorToken"))
                .thenThrow(new RuntimeException("Service error"));
        when(verificationTokenService.getEmailFromToken("errorToken"))
                .thenReturn("user@test.com");

        mockMvc.perform(get("/verify-email")
                        .param("token", "errorToken"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/verification-error*"));
    }

    // VC_008
    @Test
    public void testVerifyEmailNoEmailFromToken() throws Exception {
        when(verificationTokenService.verifyEmail("noEmailToken"))
                .thenReturn(ResponseEntity.badRequest().body("Error"));
        when(verificationTokenService.getEmailFromToken("noEmailToken"))
                .thenReturn(null);

        mockMvc.perform(get("/verify-email")
                        .param("token", "noEmailToken"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verification-error?error=Error"));
    }
}
