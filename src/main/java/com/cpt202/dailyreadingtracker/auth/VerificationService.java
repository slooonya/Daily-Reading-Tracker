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
import lombok.RequiredArgsConstructor;

/**
 * Service responsible for handling email verification functionality.
 * <ul>
 *     <li>Creating email verification tokens</li>
 *     <li>Validating email verification tokens</li>
 *     <li>Sending email verification links</li>
 * </ul>
 */

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final JWTTokenUtil jwtTokenUtil;
    private final EmailService emailService;

    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);
    
    /**
     * Verifies an email using the provided token.
     *
     * @param tokenValue the token value to validate
     * @return a response entity indicating the result of the verification process
     */
    @Transactional
    public ResponseEntity<String> verifyEmail(String tokenValue) {
        try {
            VerificationToken token = verificationTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

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

    /**
     * Validates the verification token.
     *
     * @param token the token to validate
     */
    private void validateToken(VerificationToken token){
        if (token.getStatus().equals(VerificationToken.STATUS_VERIFIED))
            throw new IllegalArgumentException("Email already verified");
            
        if (token.getExpirationDateTime().isBefore(LocalDateTime.now())){
            verificationTokenRepository.delete(token);
            throw new IllegalArgumentException("Token expired. Please request a new verification eamil.");
        }
    }

    /**
     * Retrieves the email address associated with a given token.
     *
     * @param tokenValue the token value to look up
     * @return the email address associated with the token, or null if not found
     */
    public String getEmailFromToken(String tokenValue) {
        return verificationTokenRepository.findByToken(tokenValue)
            .map(token -> token.getUser().getEmail())
            .orElse(null);
    }

    /**
     * Creates a new email verification token for the specified email address.
     *
     * @param email   the email address to verify
     * @param request the HTTP request object for generating the verification link
     */
    @Transactional
    public void createVerification(String email, HttpServletRequest request){
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

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

    /**
     * Generates an email verification URL using the provided token and HTTP request.
     *
     * @param request the HTTP request object
     * @param token   the verification token
     * @return the generated email verification URL
     */
    private String generateVerificationUrl(HttpServletRequest request, String token){
        String baseUrl = request.getRequestURL().toString().replace(request.getServletPath(), "");
        return baseUrl + "/verify-email?token=" + token;
    }
}
