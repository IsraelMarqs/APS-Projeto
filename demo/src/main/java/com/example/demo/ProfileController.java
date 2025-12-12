package com.example.demo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Optional;

@Controller
@RequestMapping("/profile")

    public class ProfileController {

        private final UserRepository userRepository;

        public ProfileController(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        @GetMapping
        public String viewProfile(Model model, Authentication authentication) {
            User user = getAuthenticatedUser(authentication);
            if (user == null) return "redirect:/login";

            model.addAttribute("user", user);
            return "profile";
        }

        @PostMapping("/update")
        public String updateProfile(@ModelAttribute User userForm, Authentication authentication) {
            User currentUser = getAuthenticatedUser(authentication);
            if (currentUser == null) return "redirect:/login";

            // Atualizamos apenas os campos permitidos
            currentUser.setName(userForm.getName());
            currentUser.setAvatarUrl(userForm.getAvatarUrl());
            currentUser.setBio(userForm.getBio());

            userRepository.save(currentUser);
            return "redirect:/profile?success";
        }

        private User getAuthenticatedUser(Authentication authentication) {
            if (authentication == null || !authentication.isAuthenticated()) return null;
            String email = null;

            // Lógica para pegar o email (compatível com OAuth e Login normal)
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                email = ((CustomUserDetails) principal).getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                email = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
            }

            if (email == null) return null;
            return userRepository.findByEmail(email).orElse(null);
        }
    }

