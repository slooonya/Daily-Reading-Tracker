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

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final JWTTokenUtil jwtTokenUtil;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder encoder;

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    public PasswordResetService(UserRepository userRepository, JWTTokenUtil jwtTokenUtil, PasswordEncoder encoder,
                                EmailService emailService, PasswordResetTokenRepository passwordResetTokenRepository){
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.encoder = encoder;
    }
    
    public String requestPasswordReset(String email, HttpServletRequest request){
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()){
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

    @Transactional
    public void createPasswordResetToken(User user, String passwordToken){
        PasswordResetToken token = passwordResetTokenRepository.findByUser(user)
            .orElse(new PasswordResetToken(passwordToken, user));

            token.setToken(passwordToken);
            token.setExpirationTime(token.getTokenExpirationTime());

            passwordResetTokenRepository.save(token);
    }

    private String generatePasswordResetUrl(HttpServletRequest request, String token) {
        String baseUrl = request.getRequestURL().toString().replace(request.getServletPath(), "");

        return baseUrl + "/reset-password?token=" + token;
    }

    public void invalidateExistingTokens(String email){
        passwordResetTokenRepository.findByUserEmail(email)
            .ifPresent(token -> {
                token.setExpirationTime(LocalDateTime.now());
                passwordResetTokenRepository.save(token);
            });
    }

    public String validatePasswordResetToken(String tokenValue){
        Optional<PasswordResetToken> optionalToken = passwordResetTokenRepository.findByToken(tokenValue);

        if (!optionalToken.isPresent())
            return "Invalid password reset token";

        PasswordResetToken token = optionalToken.get();

        if (token.isExpired())
            return "Link already expired, resend link";

        return "valid";
    }

    public String processPasswordReset(String token, String newPassword) {
        String validationResult = validatePasswordResetToken(token);

        if (!"valid".equals(validationResult))
            return "Error: " + validationResult;

        Optional<User> optionalUser = findUserByPasswordToken(token);

        if (!optionalUser.isPresent())
            return "Error: Invalid password reset token";

            User user = optionalUser.get();
            resetPassword(user, newPassword);

            passwordResetTokenRepository.deleteByToken(token);

            return "Password reset success";
    }

    @Transactional
    public Optional<User> findUserByPasswordToken(String passwordToken){
        Optional<PasswordResetToken> token = passwordResetTokenRepository.findByToken(passwordToken);
        return token.isPresent() ? Optional.of(token.get().getUser()) : Optional.empty();
    }

    public void resetPassword(User user, String newPassword){
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
    }
}
