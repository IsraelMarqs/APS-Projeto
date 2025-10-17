package com.example.demo;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final Environment env;

    public HomeController(Environment env) {
        this.env = env;
    }

    @GetMapping("/")
    public String index(Model model, Authentication authentication) {
        boolean hasGoogle = false;
        String clientId = env.getProperty("GOOGLE_CLIENT_ID");
        if (clientId != null && !clientId.isBlank()) hasGoogle = true;

        model.addAttribute("hasGoogle", hasGoogle);

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof OAuth2User) {
                OAuth2User oauth = (OAuth2User) principal;
                model.addAttribute("userName", oauth.getAttribute("name"));
                model.addAttribute("userEmail", oauth.getAttribute("email"));
                model.addAttribute("picture", oauth.getAttribute("picture"));
            } else if (principal instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) principal;
                model.addAttribute("userName", userDetails.getName());
                model.addAttribute("userEmail", userDetails.getUsername());
            }
        }
        return "index";
    }
}
