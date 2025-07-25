package com.cpt202.dailyreadingtracker.auth;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import com.cpt202.dailyreadingtracker.utils.EmailService;
import com.cpt202.dailyreadingtracker.utils.JWTTokenUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

// Service handling email verification token operations.

@Service
public class VerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final JWTTokenUtil jwtTokenUtil;
    private final EmailService emailService;

    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

    public VerificationService(VerificationTokenRepository verificationTokenRepository, UserRepository userRepository,
                               JWTTokenUtil jwtTokenUtil, EmailService emailService){
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.emailService = emailService;
    }
    
    @Transactional
    public ResponseEntity<String> verifyEmail(String tokenValue) {
        try {
            VerificationToken token = verificationTokenRepository.findByToken(tokenValue).orElseThrow(() ->
                new IllegalArgumentException("Invalid verification token"));

            validateToken(token);

            token.getUser().setEnabled(true);
            token.setStatus(VerificationToken.STATUS_VERIFIED);
            token.setConfirmedDateTime(LocalDateTime.now());

            userRepository.save(token.getUser());

            return ResponseEntity.ok("Email verified successfully. You may now log in.");
        } catch (Exception e) {
            logger.error("Email verification failed", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private void validateToken(VerificationToken token){
        if (token.getStatus().equals(VerificationToken.STATUS_VERIFIED))
            throw new IllegalArgumentException("Email already verified");
            
        if (token.getExpirationDateTime().isBefore(LocalDateTime.now())){
            verificationTokenRepository.delete(token);
            throw new IllegalArgumentException("Token expired. Please request a new verification eamil.");
        }
    }

    public String getEmailFromToken(String tokenValue) {
        return verificationTokenRepository.findByToken(tokenValue)
            .map(token -> token.getUser().getEmail())
            .orElse(null);
    }

    @Transactional
    public void createVerification(String email, HttpServletRequest request){
        User user = userRepository.findByEmail(email).orElseThrow(() -> 
            new IllegalArgumentException("User not found with email: " + email));

        verificationTokenRepository.deleteAllByUser(user);

        VerificationToken token = new VerificationToken();
        token.setToken(jwtTokenUtil.generateToken(email));
        token.setUser(user);
        token.setStatus(VerificationToken.STATUS_PENDING);
        token.setExpirationDateTime(LocalDateTime.now().plusHours(24));

        verificationTokenRepository.save(token);

        String verificationUrl = generateVerificationUrl(request, token.getToken());

        emailService.sendVerificationEmail(user, verificationUrl);
    }

    private String generateVerificationUrl(HttpServletRequest request, String token){
        String baseUrl = request.getRequestURL().toString().replace(request.getServletPath(), "");
        return baseUrl + "/verify-email?token=" + token;
    }
}
