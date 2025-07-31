package com.cpt202.dailyreadingtracker.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for loading user details for authentication purposes.
 * <p>
 * This service is a custom implementation of {@link UserDetailsService}, which integrates with the application's
 * {@link UserRepository} to fetch user details based on their email address.
 * </p>
 * <p>
 */

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(email));

        if (!user.isEnabled())
            throw new UnverifiedAccountException();

        if (user.isFreezed())
            throw new AccountFrozenException("Account is frozen");

        Set<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), user.getPassword(), user.isEnabled(),
            true, true, true, authorities);
    }

    // Exception thrown when a user attempts to authenticate with an unverified account.
    public class UnverifiedAccountException extends AuthenticationException {
        public UnverifiedAccountException(){
            super("Account not verified");
        }
    }

    // Exception thrown when a user attempts to authenticate with a frozen account.
    public static class AccountFrozenException extends RuntimeException {
        public AccountFrozenException(String message) {
            super(message);
        }
    }
}
