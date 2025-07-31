package com.cpt202.dailyreadingtracker.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import com.cpt202.dailyreadingtracker.utils.EmailService;
import com.cpt202.dailyreadingtracker.utils.JWTTokenUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Service responsible for handling password reset functionality.
 * <ul>
 *     <li>Resetting user passwords</li>
 *     <li>Generating and validating password reset tokens</li>
 *     <li>Sending password reset emails</li>
 *     <li>Invalidating existing tokens</li>
 * </ul>
 */

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final JWTTokenUtil jwtTokenUtil;

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    /**
     * Resets the user's password to the new password provided.
     *
     * @param user        the user whose password is being reset
     * @param newPassword the new password to set
     */
    public void resetPassword(User user, String newPassword){
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Creates or updates a password reset token for the specified user.
     *
     * @param user          the user for whom the token is being created
     * @param passwordToken the token value to associate with the user
     */
    @Transactional
    public void createPasswordResetToken(User user, String passwordToken) {
        PasswordResetToken token = passwordResetTokenRepository.findByUser(user)
            .orElse(new PasswordResetToken(passwordToken, user));
        
        token.setToken(passwordToken);
        token.setExpirationTime(token.getTokenExpirationTime());

        passwordResetTokenRepository.save(token);
    }

    /**
     * Validates a password reset token.
     *
     * @param tokenValue the token value to validate
     * @return a string indicating whether the token is valid or an error message
     */
    public String validatePasswordResetToken(String tokenValue) {
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(tokenValue);
        
        if (!tokenOptional.isPresent()) {
            return "Invalid password reset token";
        }
        
        PasswordResetToken token = tokenOptional.get();
        
        if (token.isExpired()) {
            return "Link already expired, resend link";
        }
        
        return "valid";
    }

    /**
     * Processes a password reset request using a token and a new password.
     *
     * @param token       the password reset token
     * @param newPassword the new password to set
     * @return a string indicating the result of the password reset process
     */
    public String processPasswordReset(String token, String newPassword) {
        String validationResult = validatePasswordResetToken(token);

        if (!"valid".equals(validationResult)) {
            return "Error: " + validationResult;
        }
    
        Optional<User> userOptional = findUserByPasswordToken(token);
        if (!userOptional.isPresent()) {
            return "Error: Invalid password reset token";
        }
    
        User user = userOptional.get();
        resetPassword(user, newPassword);
        
        passwordResetTokenRepository.deleteByToken(token);
        
        return "Password reset successfully";
    }

    /**
     * Sends a password reset email to the user with the specified email address.
     *
     * @param email   the email address of the user requesting a password reset
     * @param request the HTTP request object for generating the reset link
     * @return a string indicating the status of the password reset request
     */
    public String requestPasswordReset(String email, HttpServletRequest request) {
        Optional<User> user = userRepository.findByEmail(email);
        
        if (user.isPresent()) {
            try {
                String token = jwtTokenUtil.generateToken(email);
                createPasswordResetToken(user.get(), token);

                String resetUrl = generatePasswordResetUrl(request, token);
                emailService.sendPasswordResetEmail(user.get(), resetUrl);

                return "If this email exists, a reset link has been sent";
            } catch (Exception e) {
                logger.error("Failed to send password reset email", e);
                return "Error: Failed to send reset email. Please try again later.";
            }
        }
        return "If this email exists, a reset link has been sent";
    }
    
    /**
     * Invalidates existing password reset tokens for the specified email address.
     *
     * @param email the email address of the user whose tokens should be invalidated
     */
    public void invalidateExistingTokens(String email) {
        passwordResetTokenRepository.findByUserEmail(email)
            .ifPresent(token -> {
                token.setExpirationTime(LocalDateTime.now()); 
                passwordResetTokenRepository.save(token);
            });
    }

     /**
     * Finds the user associated with a given password reset token.
     *
     * @param passwordToken the token value to look up
     * @return an optional user associated with the token
     */
    @Transactional
    public Optional<User> findUserByPasswordToken(String passwordToken) {
        Optional<PasswordResetToken> token = passwordResetTokenRepository.findByToken(passwordToken);
        return token.isPresent() ? Optional.of(token.get().getUser()) : Optional.empty();
    }

    /**
     * Generates a password reset URL using the provided token and HTTP request.
     *
     * @param request the HTTP request object
     * @param token   the password reset token
     * @return the generated password reset URL
     */
    private String generatePasswordResetUrl(HttpServletRequest request, String token) {
        String baseUrl = request.getRequestURL().toString()
                .replace(request.getServletPath(), "");
        return baseUrl + "/reset-password?token=" + token;
    }
}
