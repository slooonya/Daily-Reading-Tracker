package com.cpt202.dailyreadingtracker.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cpt202.dailyreadingtracker.user.UserRepository;
import com.cpt202.dailyreadingtracker.utils.JWTTokenUtil;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.utils.EmailService;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {
    
    @Mock
    private PasswordEncoder encoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JWTTokenUtil jwtTokenUtil;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private PasswordResetToken validToken;
    private PasswordResetToken expiredToken;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.setEmail("usertest@test.com");
        testUser.setPassword("ForgottenPassword123");

        validToken = new PasswordResetToken("validToken", testUser);
        validToken.setExpirationTime(LocalDateTime.now().plusHours(1));

        expiredToken = new PasswordResetToken("expiredToken", testUser);
        expiredToken.setExpirationTime(LocalDateTime.now().minusHours(1));
    }

    // PR_001
    @Test
    public void testResetPasswordWithValidInput() {
        when(encoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        
        passwordResetService.resetPassword(testUser, "newPassword123");
        
        assertEquals("encodedNewPassword", testUser.getPassword());
        verify(userRepository).save(testUser);
    }

    // PR_002
    @Test
    public void testCreatePasswordResetTokenNewToken() {
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

        passwordResetService.createPasswordResetToken(testUser, "token123");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertEquals("token123", tokenCaptor.getValue().getToken());
    }

    // PR_003
    @Test
    public void testCreatePasswordResetTokenUpdateToken() {
        PasswordResetToken existingToken = new PasswordResetToken("oldToken", testUser);
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(existingToken));

        passwordResetService.createPasswordResetToken(testUser, "newToken");

        assertEquals("newToken", existingToken.getToken());
        verify(tokenRepository).save(existingToken);
    }

    // PR_004
    @Test
    public void testValidatePasswordResetTokenWithValidToken() {
        when(tokenRepository.findByToken("validToken")).thenReturn(Optional.of(validToken));

        String result = passwordResetService.validatePasswordResetToken("validToken");

        assertEquals("valid", result);
    }

    // PR_005
    @Test
    public void testValidatePasswordResetTokenWithInvalidToken() {
        when(tokenRepository.findByToken("invalidToken")).thenReturn(Optional.empty());

        assertEquals("Invalid password reset token",
                passwordResetService.validatePasswordResetToken("invalidToken"));
    }

    // PR_006
    @Test
    public void testValidatePasswordResetTokenWithExpiredToken() {
        when(tokenRepository.findByToken("expiredToken")).thenReturn(Optional.of(expiredToken));

        assertEquals("Link already expired, resend link",
                passwordResetService.validatePasswordResetToken("expiredToken"));
    }

    // PR_007
    @Test
    public void testProcessPasswordResetWithValidToken() {
        when(tokenRepository.findByToken("validToken")).thenReturn(Optional.of(validToken));
        when(encoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        String result = passwordResetService.processPasswordReset("validToken", "newPassword123");

        assertEquals("Password reset successfully", result);
        verify(userRepository).save(testUser);
        verify(tokenRepository).deleteByToken("validToken");
    }

    // PR_008
    @Test
    public void testProcessPasswordResetWithExpiredToken() {
        when(tokenRepository.findByToken("expiredToken")).thenReturn(Optional.of(expiredToken));

        String result = passwordResetService.processPasswordReset("expiredToken", "newPassword123");

        assertEquals("Error: Link already expired, resend link", result);
        verify(userRepository, never()).save(any());
    }

    // PR_009
    @Test
    public void testRequestPasswordResetWithValidEmail() {
        when(userRepository.findByEmail("usertest@test.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenUtil.generateToken("usertest@test.com")).thenReturn("generatedToken");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/api"));
        when(request.getServletPath()).thenReturn("/api");

        String result = passwordResetService.requestPasswordReset("usertest@test.com", request);

        assertEquals("If this email exists, a reset link has been sent", result);
        verify(emailService).sendPasswordResetEmail(eq(testUser), contains("generatedToken"));
    }

    // PR_010
    @Test
    public void testRequestPasswordResetWithInvalidEmail() {
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        String result = passwordResetService.requestPasswordReset("nonexistent@test.com", request);

        assertEquals("If this email exists, a reset link has been sent", result);
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    // PR_011
    @Test
    public void testInvalidateExistingTokens() {
        when(tokenRepository.findByUserEmail("usertest@test.com")).thenReturn(Optional.of(validToken));

        passwordResetService.invalidateExistingTokens("usertest@test.com");

        assertTrue(validToken.isExpired());
        verify(tokenRepository).save(validToken);
    }
}
