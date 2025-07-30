package com.cpt202.dailyreadingtracker.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import com.cpt202.dailyreadingtracker.utils.EmailService;
import com.cpt202.dailyreadingtracker.utils.FileStorageService;
import com.cpt202.dailyreadingtracker.violationlog.ViolationLogRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ViolationLogRepository violationLogRepository;

    @Value("${app.uploads.dir}")
    private String uploadDir;

    @Value("${app.uploads.host}")
    private String host;

    @GetMapping("/profile")
    public String getUserProfilePage(Model model, Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            model.addAttribute("user", user);
            model.addAttribute("imgHost", host);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load user profile");
        }
        return "readinglog/profile";
    }

    @GetMapping("/admin-profile")
    public String getAdminProfilePage(Model model, Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            model.addAttribute("user", user);
            model.addAttribute("imgHost", host);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load user profile");
        }

        return "admin/admin-profile";
    }

    @PutMapping("/user-profile/{userId}/avatar")
    public ResponseEntity<?> updateAvatar(@PathVariable Long userId,@RequestParam("avatar") MultipartFile avatar) {
        try {
            if (avatar.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }
            
            String contentType = avatar.getContentType();
            if (!"image/jpeg".equalsIgnoreCase(contentType) && !"image/png".equalsIgnoreCase(contentType)) {
                return ResponseEntity.badRequest().body("Only JPEG/PNG images allowed");
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            if (user.getAvatarFileName() != null) {
                try {
                    fileStorageService.deleteAvatar(user.getAvatarFileName());
                } catch (IOException e) {
                    System.err.println("Could not delete old avatar: " + e.getMessage());
                }
            }
            
            String filename = fileStorageService.storeAvatar(avatar, user.getUsername());
            user.setAvatarFileName(filename);

            userRepository.save(user);
            
            return ResponseEntity.ok().body(Map.of(
                "message", "Avatar updated successfully",
                "filename", filename
            ));
            
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("File upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/user-profile/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
        
        emailService.sendPasswordChangeNotification(user);
        
        return ResponseEntity.ok().body("Password changed successfully");
    }

    @PutMapping("/user-profile/update")
    @ResponseBody
    public UserVo updateUserProfile(@RequestBody Map<String, Object> updates, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (updates.containsKey("username")) {
            String newUsername = (String) updates.get("username");
            
            if (!user.getUsername().equals(newUsername)) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken");
                }
                
                violationLogRepository.updateUsername(user.getId(), newUsername);
                
            }

            user.setUsername(newUsername);
        }

        if (updates.containsKey("aboutMe")) {
            user.setAboutMe((String) updates.get("aboutMe"));
        }

        user = userRepository.save(user);
        return new UserVo(user);
    }
}
