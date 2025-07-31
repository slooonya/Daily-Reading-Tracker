package com.cpt202.dailyreadingtracker.app;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller responsible for general application navigation and error handling.
 * <p>
 * Provides endpoints for:
 * <ul>
 *     <li>Rendering the home page</li>
 *     <li>Displaying the error page</li>
 * </ul>
 * <p>
 */

@Controller
@RequiredArgsConstructor
public class AppController {

    @GetMapping("/home")
    public String getHomePage(Authentication authentication, Model model, HttpServletRequest request) {
        model.addAttribute("_csrf", request.getAttribute("_csrf"));
        return "readinglog/home";
    }

    @GetMapping("/error")
    public String showErrorPage() {
        return "error";
    }
}
