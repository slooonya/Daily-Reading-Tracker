package com.cpt202.dailyreadingtracker.auth;


import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import com.cpt202.dailyreadingtracker.utils.EmailService;
import com.cpt202.dailyreadingtracker.utils.JWTTokenUtil;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
public class VerificationServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private VerificationTokenRepository tokenRepository;
    
    @Mock
    private EmailService emailService;

    @Mock
    private JWTTokenUtil jwtTokenUtil;

    @Mock
    private HttpServletRequest request;
    
    @InjectMocks
    private VerificationService verificationService;

    private User testUser;
    private VerificationToken validToken;
    private VerificationToken expiredToken;
    private VerificationToken verifiedToken;

    @BeforeEach
    void setup() {
        testUser = new User();
        testUser.setEmail("testuser@test.com");
        testUser.setEnabled(false);

        validToken = new VerificationToken();
        validToken.setToken("validToken");
        validToken.setUser(testUser);
        validToken.setStatus(VerificationToken.STATUS_PENDING);
        validToken.setExpirationDateTime(LocalDateTime.now().plusHours(24));

        expiredToken = new VerificationToken();
        expiredToken.setToken("expiredToken");
        expiredToken.setUser(testUser);
        expiredToken.setStatus(VerificationToken.STATUS_PENDING);
        expiredToken.setExpirationDateTime(LocalDateTime.now().minusHours(1));

        verifiedToken = new VerificationToken();
        verifiedToken.setToken("verifiedToken");
        verifiedToken.setUser(testUser);
        verifiedToken.setStatus(VerificationToken.STATUS_VERIFIED);
        verifiedToken.setExpirationDateTime(LocalDateTime.now().plusHours(24));
    }

    // VS_001
    @Test
    public void testCreateVerificationNewUser() {
        when(userRepository.findByEmail("usertest@test.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenUtil.generateToken("usertest@test.com")).thenReturn("generatedToken");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080"));
        when(request.getServletPath()).thenReturn("");

        VerificationToken expectedToken = new VerificationToken();
        expectedToken.setToken("generatedToken");
        when(tokenRepository.save(any())).thenReturn(expectedToken);

        verificationService.createVerification("usertest@test.com", request);

        verify(tokenRepository).deleteAllByUser(testUser);
        verify(tokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendVerificationEmail(eq(testUser), contains("generatedToken"));
    }

    // VS_002
    @Test
    public void testVerifyEmailValidToken() {
        when(tokenRepository.findByToken("validToken")).thenReturn(Optional.of(validToken));

        ResponseEntity<String> response = verificationService.verifyEmail("validToken");

        assertTrue(testUser.isEnabled());
        assertEquals(VerificationToken.STATUS_VERIFIED, validToken.getStatus());
        assertNotNull(validToken.getConfirmedDateTime());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Email verified successfully. You may now log in.", response.getBody());
    }

    // VS_003
    @Test
    public void testVerifyEmailAlreadyVerified() {
        when(tokenRepository.findByToken("verifiedToken")).thenReturn(Optional.of(verifiedToken));

        ResponseEntity<String> response = verificationService.verifyEmail("verifiedToken");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Email already verified"));
        verify(userRepository, never()).save(any());
    }

    // VS_004
    @Test
    public void testVerifyEmailExpiredToken() {
        when(tokenRepository.findByToken("expiredToken")).thenReturn(Optional.of(expiredToken));

        ResponseEntity<String> response = verificationService.verifyEmail("expiredToken");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Token expired"));
        verify(tokenRepository).delete(expiredToken);
    }

    // VS_005
    @Test
    public void testVerifyEmailInvalidToken() {
        when(tokenRepository.findByToken("invalidToken")).thenReturn(Optional.empty());

        ResponseEntity<String> response = verificationService.verifyEmail("invalidToken");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid verification token"));
    }

    // VS_006
    @Test
    public void testGetEmailFromTokenValidToken() {
        when(tokenRepository.findByToken("validToken")).thenReturn(Optional.of(validToken));

        String email = verificationService.getEmailFromToken("validToken");

        assertEquals("testuser@test.com", email);
    }

    // VS_007
    @Test
    void testGetEmailFromTokenInvalidToken() {
        when(tokenRepository.findByToken("invalidToken")).thenReturn(Optional.empty());

        String email = verificationService.getEmailFromToken("invalidToken");

        assertNull(email);
    }

}

