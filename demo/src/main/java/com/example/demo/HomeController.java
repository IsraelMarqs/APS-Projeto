package com.example.demo;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    private final Environment env;
    private final BookRepository bookRepository;

    private final UserRepository userRepository;

    public HomeController(Environment env, BookRepository bookRepository, UserRepository userRepository) {
        this.env = env;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index(Model model, Authentication authentication, @RequestParam(value = "q", required = false) String q) {
        boolean hasGoogle = false;
        String clientId = env.getProperty("GOOGLE_CLIENT_ID");
        if (clientId != null && !clientId.isBlank()) hasGoogle = true;

        model.addAttribute("hasGoogle", hasGoogle);

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof OAuth2User oauth) {
                model.addAttribute("userName", oauth.getAttribute("name"));
                model.addAttribute("userEmail", oauth.getAttribute("email"));
                model.addAttribute("picture", oauth.getAttribute("picture"));
            } else if (principal instanceof CustomUserDetails userDetails) {
                model.addAttribute("userName", userDetails.getName());
                model.addAttribute("userEmail", userDetails.getUsername());
            }
        }

        List<Book> books;
        if (q != null && !q.isBlank()) {
            books = bookRepository.findByTitleContainingIgnoreCaseOrAuthorsContainingIgnoreCaseOrderByTitleAsc(q, q);
        } else {
            books = bookRepository.findAllByOrderByTitleAsc();
        }
        model.addAttribute("books", books);
        model.addAttribute("q", q == null ? "" : q);

        // Determine if authenticated internal user can add books
        boolean canAdd = false;
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CustomUserDetails) {
            canAdd = true;
        }
        model.addAttribute("canAdd", canAdd);

        return "index";
    }
}
