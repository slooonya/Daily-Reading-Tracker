package com.cpt202.dailyreadingtracker.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cpt202.dailyreadingtracker.security.SecurityService;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PasswordResetController {
    
    private final PasswordResetService passwordResetService;
    private final SecurityService securityService;

    public PasswordResetController(PasswordResetService passwordResetService, SecurityService securityService){
        this.passwordResetService = passwordResetService;
        this.securityService = securityService;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model){
        if (securityService.isAuthenticated())
            return "redirect:/home";

        return "auth/forgot-password";
    }

    @RateLimiter(name = "passwordResetLimiter")
    @PostMapping("/forgot-password")
    public String requestPasswordReset(@RequestParam String email, HttpServletRequest request,
                                       RedirectAttributes redirectAttributes){
        String result = passwordResetService.requestPasswordReset(email, request);
        redirectAttributes.addFlashAttribute("message", result);

        return "redirect:/forgot-password";
    }

    @RateLimiter(name = "passwordResetLimiter")
    @PostMapping("/forgot-password/resend")
    public String resendPasswordResetLink(@RequestParam String email, HttpServletRequest request, 
                                          RedirectAttributes redirectAttributes) {
        
        passwordResetService.invalidateExistingTokens(email);

        String result = passwordResetService.requestPasswordReset(email, request);
        redirectAttributes.addAttribute("message", result);

        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetForm(@RequestParam String token, Model model) {
        try {
            String validationResult = passwordResetService.validatePasswordResetToken(token);
            
            if (!"valid".equals(validationResult)) {
                model.addAttribute("error", validationResult);
                return "auth/error";
            }

            model.addAttribute("token", token);

            return "auth/password-reset-form";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred");

            return "auth/error";
        }
    }

    @RateLimiter(name = "passwordResetAttemptLimiter")
    @PostMapping("/reset-password")
    public String processPasswordReset(@RequestParam String token, @RequestParam String password,
                                       @RequestParam String confirmPassword, RedirectAttributes redirectAttributes) {

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "Passwords don't match");
            return "redirect:/reset-password?token=" + token;
        }

        String result = passwordResetService.processPasswordReset(token, password);
        
        if (result.startsWith("Error")) {
            redirectAttributes.addAttribute("error", result);
            return "redirect:/reset-password?token=" + token;
        }

        redirectAttributes.addFlashAttribute("message", result);

        return "redirect:/auth";
    }
}
