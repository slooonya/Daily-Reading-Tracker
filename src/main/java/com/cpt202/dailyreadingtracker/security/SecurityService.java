package com.cpt202.dailyreadingtracker.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for handling authentication and security-related functionality.
 * <ul>
 *     <li>Check if the current user is authenticated</li>
 *     <li>Perform automatic login for a user</li>
 *     <li>Handle email verification checks during login</li>
 * </ul>
 */

@Service
@RequiredArgsConstructor
public class SecurityService {
    
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    /**
     * Checks whether the current user is authenticated.
     *
     * @return {@code true} if the user is authenticated, {@code false} otherwise
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || AnonymousAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
            return false;
        }
        return authentication.isAuthenticated();
    }

    /**
     * Automatically logs in a user with the provided username and password.
     * Ensures that the user's email is verified before allowing login.
     *
     * @param username the username of the user
     * @param password the password of the user
     * @throws EmailNotVerifiedException if the user's email is not verified
     * @throws BadCredentialsException   if the username or password is incorrect
     */
    public void autoLogin(String username, String password) {
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            if (!user.isEnabled()) {
                throw new EmailNotVerifiedException("Email address not verified");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authenticationToken = 
                new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());

            authenticationManager.authenticate(authenticationToken);

            if (authenticationToken.isAuthenticated()) {
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                logger.debug("Auto login {} successfully!", username);
            }
        } catch (EmailNotVerifiedException e) {
            logger.warn("Login attempt for unverified user: {}", username);
            throw e;
        } catch (BadCredentialsException e) {
            logger.warn("Bad credentials for user: {}", username);
            throw e;
        }
    }

    /**
     * Exception thrown when a user attempts to log in without verifying their email address.
     */
    public static class EmailNotVerifiedException extends RuntimeException {
        public EmailNotVerifiedException(String message) {
            super(message);
        }
    }
}
