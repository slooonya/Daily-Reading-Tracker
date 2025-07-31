package com.cpt202.dailyreadingtracker.auth;

import java.io.IOException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cpt202.dailyreadingtracker.security.SecurityService;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import com.cpt202.dailyreadingtracker.utils.FileStorageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller responsible for handling user authentication and registration.
 * <p>
 * Provides endpoints for:
 * <ul>
 *     <li>Rendering the authentication page (login and registration forms)</li>
 *     <li>Processing user registration</li>
 *     <li>Handling user login</li>
 *     <li>Logging out users</li>
 * </ul>
 * <p>
 */

@Controller
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final SecurityService securityService;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/auth")
    public String getAuthPage(Model model, @RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout, @RequestParam(required = false) String mode) {
        model.addAttribute("user", new User());
        
        if (error != null)
            model.addAttribute("error", "Your email or password is invalid.");
        
        if (logout != null)
            model.addAttribute("message", "You have been logged out successfully.");
        
        if (mode.equals("register"))
            model.addAttribute("defaultToRegister", true);

        return "auth/authentication";
    }

    /**
     * Processes user registration by validating input, saving the user, and optionally storing an avatar image.
     * <p>
     * Validation points:
     * <ul>
     *     <li>Checking if the username and email are unique</li>
     *     <li>Ensuring that passwords match</li>
     *     <li>Validating the uploaded avatar file (if provided)</li>
     * </ul>
     * <p>
     * If validation fails, the user is redirected back to the registration form with error messages.
     * On successful registration, the user is redirected to the verification pending page.
     * </p>
     *
     * @param user                the user object containing registration details
     * @param result              the binding result for validation errors
     * @param avatar              the optional avatar file uploaded by the user
     * @param request             the HTTP request object
     * @param redirectAttributes  attributes for redirecting with messages
     * @param model               the model for passing data to the view
     * @return the name of the view to render or a redirect URL
     */
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("user") User user, BindingResult result,
                                    @RequestParam(required = false) MultipartFile avatar, HttpServletRequest request, 
                                    RedirectAttributes redirectAttributes, Model model) {
        if (!avatar.isEmpty()) {
            try {
                fileStorageService.validateFile(avatar);
            } catch (IOException e) {
                String errorMessage = e.getMessage().contains("JPG format") ? e.getMessage() 
                                                                          : "Invalid image file: " + e.getMessage();
                result.rejectValue("avatarFile", "file.error", errorMessage);
            }
        }

        if (user.getConfirmPassword() == null || user.getConfirmPassword().isEmpty())
            result.rejectValue("confirmPassword", "NotEmpty", 
                        "Password confirmation is required");

        if (!user.isPasswordsMatch())
            result.rejectValue("confirmPassword", "Match", "Passwords must match");

        if (userRepository.existsByEmail(user.getEmail()))
            result.rejectValue("email", "Duplicate", "Taken");

        if (userRepository.existsByUsername(user.getUsername()))
            result.rejectValue("username", "Duplicate", "Taken");

        if (result.hasErrors()) {
            model.addAttribute("defaultToRegister", true);
            model.addAttribute("showRegisterForm", true);
            model.addAttribute("containerClass", "sign-up-mode");
            return "auth/authentication";
        }
        
        authService.register(user, avatar, request);
        return "redirect:/verification-pending?email=" + user.getEmail();
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String username, @RequestParam String password, 
                            RedirectAttributes redirectAttributes) {
        try {
            securityService.autoLogin(username, password);
            return "redirect:/home";
        } catch (DisabledException e) {
            redirectAttributes.addAttribute("email", username);
            return "redirect:/verification-pending";
        } catch (BadCredentialsException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid credentials");
            return "redirect:/auth?mode=login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null)
            new SecurityContextLogoutHandler().logout(request, response, auth);

        return "redirect:/auth?logout=true";
    }
    
}
