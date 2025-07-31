package com.cpt202.dailyreadingtracker.readingstatistics;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Controller responsible for managing and displaying reading progress for users.
 * <p>
 * Provides endpoints for:
 * <ul>
 *     <li>Rendering the reading progress page with user-specific statistics</li>
 *     <li>Providing book progress statistics as a JSON response</li>
 * </ul>
 * <p>
 */

@Controller
@RequiredArgsConstructor
public class ReadingProgressController {
    private final ReadingStatisticsService readingStatisticsService;
    private final UserRepository userRepository;

    @GetMapping("/progress")
    public String progressPage(Model model, Principal principal) {

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            Map<String, Object> stats = readingStatisticsService.getReadingStatistics(user.getId());

            model.addAttribute("stats", stats);
        } catch (Exception e) {
            System.err.println("Failed to get statistics: " + e.getMessage());
            model.addAttribute("error", "Failed to load statistics");
            model.addAttribute("stats", Map.of(
                    "bookCount", 0,
                    "totalReadingTime", 0,
                    "avgDailyTime", 0
            ));
        }

        return "readinglog/progress";
    }
    
    @GetMapping("/book-progress")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBookProgress(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("The user does not exist."));

        Map<String, Object> result = readingStatisticsService.getBookProgressStats(user.getId());

        return ResponseEntity.ok(result);
    }
}
