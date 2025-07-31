package com.cpt202.dailyreadingtracker.auth;

import java.io.IOException;
import java.util.HashSet;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cpt202.dailyreadingtracker.role.Role;
import com.cpt202.dailyreadingtracker.role.RoleRepository;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import com.cpt202.dailyreadingtracker.utils.FileStorageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Service responsible for user registration and authentication logic.
 * <ul>
 *     <li>Registers new users with optional avatar upload</li>
 *     <li>Encodes passwords securely</li>
 *     <li>Assigns roles to users based on their status (e.g., first user as admin)</li>
 *     <li>Sends email verification for new accounts</li>
 * </ul>
 */

@Service
@RequiredArgsConstructor
public class AuthService {

    private final VerificationService verificationService;

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final FileStorageService fileStorageService;
    
    /**
     * Registers a new user, stores their avatar, assigns roles, and sends a verification email.
     *
     * @param user    the user object containing registration details
     * @param avatar  the user's avatar file (optional)
     * @param request the HTTP request object for generating verification links
     * @throws RegistrationException if registration fails due to user existence, password mismatch, or other issues
     * @throws EmailVerificationException if email verification fails
     */
    @Transactional
    public void register(User user, MultipartFile avatar, HttpServletRequest request){
        try {
            if (userRepository.existsByEmail(user.getEmail()) || 
            userRepository.existsByUsername(user.getUsername())) 
                throw new IllegalArgumentException("User already exists");

            if (!user.isPasswordsMatch())
                throw new RegistrationException("Passwords must match");
   
            user.setConfirmPassword(null);
            user.setPassword(encoder.encode(user.getPassword()));

            if (avatar != null && !avatar.isEmpty()) {
                try {
                    String avatarFilename = fileStorageService.storeAvatar(avatar, user.getUsername());
                    user.setAvatarFileName(avatarFilename);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to store avatar", e);
                }
            }

            boolean isFirstUser = userRepository.count() == 0;

            if (user.getRoles() == null)
                user.setRoles(new HashSet<>());

            Role defaultRole;
            if (isFirstUser) {
                defaultRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role newRole = new Role("ROLE_ADMIN");
                        return roleRepository.save(newRole);
                    });
            } else {
                defaultRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        Role newRole = new Role("ROLE_USER");
                        return roleRepository.save(newRole);
                    });
            }

            user.getRoles().clear();
            user.getRoles().add(defaultRole);

            userRepository.save(user);

            if (request != null) {
                try {
                    verificationService.createVerification(user.getEmail(), request);
                } catch (Exception e){
                    userRepository.delete(user);
                    throw new EmailVerificationException("Failed to send verification email", e);
                }
            }
        } catch (RuntimeException e) {
            throw new RegistrationException("Registration failed: " + e.getMessage(), e);
        }            
    }

     /**
     * Exception thrown when registration fails.
     */
    public class RegistrationException extends RuntimeException{
        public RegistrationException(String message, Throwable cause){
            super(message, cause);
        }

        public RegistrationException(String message){
            super(message);
        }
    }

    /**
     * Exception thrown when email verification fails.
     */
    public class EmailVerificationException extends RuntimeException {
        public EmailVerificationException(String message, Throwable cause){
            super(message, cause);
        }
    }
}
