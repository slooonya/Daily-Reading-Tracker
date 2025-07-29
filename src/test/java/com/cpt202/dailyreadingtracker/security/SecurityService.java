package com.cpt202.dailyreadingtracker.security;

import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private SecurityService securityService;

    private final String testUsername = "testuser";
    private final String testPassword = "password";
    private User testUser;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername(testUsername);
        testUser.setEnabled(true);

        testUserDetails = org.springframework.security.core.userdetails.User
                .withUsername(testUsername)
                .password(testPassword)
                .authorities("ROLE_USER")
                .build();

        SecurityContextHolder.setContext(securityContext);
    }

    // SS_001
    @Test
    public void testIsAuthenticatedWhenAuthenticated() {
        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        boolean result = securityService.isAuthenticated();

        assertTrue(result);
    }

    // SS_002
    @Test
    public void testIsAuthenticatedWhenNotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(null);

        boolean result = securityService.isAuthenticated();

        assertFalse(result);
    }

    // SS_003
    @Test
    public void testAutoLoginWithValidCredentials() {
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(testUserDetails);

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        securityService.autoLogin(testUsername, testPassword);

        verify(securityContext).setAuthentication(any(Authentication.class));
    }

    // SS_004
    @Test
    public void testAutoLoginWithInvalidUser() {
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            securityService.autoLogin(testUsername, testPassword);
        });
    }

    // SS_005
    @Test
    public void testAutoLoginWithUnverifiedEmail() {
        testUser.setEnabled(false);
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

        assertThrows(SecurityService.EmailNotVerifiedException.class, () -> {
            securityService.autoLogin(testUsername, testPassword);
        });
    }

    // SS_006
    @Test
    public void testAutoLoginWithBadCredentials() {
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(testUserDetails);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> {
            securityService.autoLogin(testUsername, testPassword);
        });
    }

}

