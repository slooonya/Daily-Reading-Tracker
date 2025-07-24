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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

// Controller handling authentication-related operations including login, registration, and logout 

@Controller
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final SecurityService securityService;
    private final FileStorageService fileStorageService;

    AuthController(UserRepository userRepository, AuthService authService, 
                   SecurityService securityService, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.securityService = securityService;
        this.fileStorageService = fileStorageService;
    }
    
    @GetMapping("/auth")
    public String getAuthPage(Model model, @RequestParam(required = false) String error,
                              @RequestParam(required = false) String logout, @RequestParam(required = false) String mode){
        model.addAttribute("user", new User());

        if (error != null)
            model.addAttribute("error", "Your email or password is invalid.");

        if (logout != null)
            model.addAttribute("message", "You have been logged out successfully.");
            
        if (mode.equals("register"))
            model.addAttribute("defaultToRegister", true);

        return "auth/authentication";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("user") User user, BindingResult result, 
                                      @RequestParam(required = false) MultipartFile avatar, HttpServletRequest request, 
                                      RedirectAttributes redirectAttributes, Model model) {
        if (!avatar.isEmpty()){
            try {
                fileStorageService.validateFile(avatar);
            } catch (IOException e){
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

        if (result.hasErrors()){
            model.addAttribute("defaultToRegister", true);
            model.addAttribute("showRegisterForm", true);
            model.addAttribute("containerClass", "sign-up-mode");
            return "auth/authentication";
        }

        authService.register(user, avatar, request);

        return "redirect:/verification-pending?email=" + user.getEmail();
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email, @RequestParam String password,
                            RedirectAttributes redirectAttributes){
        try{
            securityService.autoLogin(email, password);
            return "redirect:/home";
        } catch (DisabledException e){
            redirectAttributes.addAttribute("email", email);
            return "redirect:/verication-pending";
        } catch (BadCredentialsException e){
            redirectAttributes.addAttribute("error", "Invalid credentials");
            return "redirect:/auth?mode=login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null)
            new SecurityContextLogoutHandler().logout(request, response, auth);

        return "redirect:/auth?logout=true";
    }
    
}
