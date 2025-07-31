package com.cpt202.dailyreadingtracker.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Controller responsible for handling email verification functionality.
 * <p>
 * Provides endpoints for:
 * <ul>
 *     <li>Verifying user email addresses using tokens</li>
 *     <li>Displaying verification error messages</li>
 *     <li>Displaying a pending verification page</li>
 *     <li>Resending verification emails</li>
 * </ul>
 * <p>
 */

@Controller
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private static final Logger logger = LoggerFactory.getLogger(VerificationController.class);

    /**
     * Verifies a user's email address using a token.
     * <p>
     * If the token is valid, the user's email is marked as verified, and they are redirected to the login page.
     * If the token is invalid or expired, an error message is displayed.
     * </p>
     *
     * @param token              the email verification token
     * @param model              the model for passing data to the view
     * @param redirectAttributes attributes for redirecting with messages
     * @return the redirect URL or view name based on the verification result
     */
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model, 
                              RedirectAttributes redirectAttributes) {
        try {
            ResponseEntity<String> response = verificationService.verifyEmail(token);

            if (response.getStatusCode() == HttpStatus.OK) {
                return "redirect:/auth?verified=true";
            } else {
                String email = verificationService.getEmailFromToken(token);
                redirectAttributes.addAttribute("error", response.getBody());

                if (email != null) {
                    redirectAttributes.addAttribute("email", email);
                }

                return "redirect:/verification-error";
            }
        } catch (Exception e) {
            logger.error("Email verification failed", e);
            redirectAttributes.addAttribute("error", e.getMessage());
            
            try {
                String email = verificationService.getEmailFromToken(token);

                if (email != null) {
                    redirectAttributes.addAttribute("email", email);
                }
            } catch (Exception ex) {
                logger.warn("Could not extract email from token", ex);
            }
            
            return "redirect:/verification-error";
        }
    }

    @GetMapping("/verification-error")
    public String showVerificationError(@RequestParam(required = false) String error, 
                                        @RequestParam(required = false) String email, Model model) {
        model.addAttribute("error", error);
        model.addAttribute("email", email);

        return "auth/verification-error";
    }

    @GetMapping("/verification-pending")
    public String showVerificationPending(@RequestParam String email,
                                    @RequestParam(required = false) Boolean resent, Model model) {
        model.addAttribute("email", email);

        if (Boolean.TRUE.equals(resent)) {
            model.addAttribute("message", "New verification email sent!");
        }

        return "auth/verification-pending";
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam String email, 
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            verificationService.createVerification(email, request);
            redirectAttributes.addAttribute("email", email);
            return "redirect:/verification-pending?resent=true";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "Failed to resend verification: " + e.getMessage());
            redirectAttributes.addAttribute("email", email);
            return "redirect:/verification-error";
        }
    }
}
