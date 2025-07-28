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

// Service for security operations (auth status verification, automatic login, user verification checks)

@Service
public class SecurityService {
    
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    public SecurityService(AuthenticationManager authenticationManager, 
                         UserDetailsService userDetailsService,
                         UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || AnonymousAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
            return false;
        }
        return authentication.isAuthenticated();
    }

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

    public static class EmailNotVerifiedException extends RuntimeException {
        public EmailNotVerifiedException(String message) {
            super(message);
        }
    }
}
