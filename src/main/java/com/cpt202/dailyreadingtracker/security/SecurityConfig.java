package com.cpt202.dailyreadingtracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.cpt202.dailyreadingtracker.security.CustomUserDetailsService.AccountFrozenException;

// Configiration defining application security policies

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/register",
                    "/register/**",
                    "/auth",
                    "/auth/**",
                    "/uploads/**",
                    "/verify-email",
                    "/verify-email/**",
                    "/verify-email**",
                    "/resend-verification",
                    "/resend-verification/**",
                    "/verification-pending",
                    "/verification-pending/**",
                    "/forgot-password",
                    "/forgot-password/**",
                    "/reset-password",
                    "/reset-password/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/logout",
                    "/error"
                ).permitAll()
                .requestMatchers("/home").hasRole("USER")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth")
                .failureHandler(authenticationFailureHandler())
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/auth?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )

            .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/auth"))
            );

            return http.build();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String email = request.getParameter("username");
            String redirectUrl = "/auth?error=true";

            if (exception instanceof InternalAuthenticationServiceException &&
                exception.getCause() instanceof CustomUserDetailsService.UnverifiedAccountException)
                    redirectUrl = "/verification-pending?email=" + email;
            else if (exception instanceof BadCredentialsException)
                redirectUrl = "/auth?error=true";
            else if (exception instanceof InternalAuthenticationServiceException){
                Throwable cause = exception.getCause();
                if (cause instanceof AccountFrozenException)
                    redirectUrl = "/account-frozen";
            }

            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

            System.out.println("SecurityConfig Login Success - Permissions: " + authentication.getAuthorities());
            String targetUrl = isAdmin ? "/homeforadmin" : "home";
            response.sendRedirect(targetUrl);
        };
    }
    
}
