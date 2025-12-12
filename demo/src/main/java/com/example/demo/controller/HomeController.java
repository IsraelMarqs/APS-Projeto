package com.example.demo.controller;

import com.example.demo.entity.Book;
import com.example.demo.service.BookService;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.repository.UserRepository;
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
    private final BookService bookService;
    private final UserRepository userRepository;

    public HomeController(Environment env, BookService bookService, UserRepository userRepository) {
        this.env = env;
        this.bookService = bookService;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index(Model model, Authentication authentication, @RequestParam(value = "q", required = false) String q) {
        // 1. Verifica configuração do Google
        String envId = env.getProperty("GOOGLE_CLIENT_ID");
        String propId = env.getProperty("spring.security.oauth2.client.registration.google.client-id");
        boolean hasGoogle = (envId != null && !envId.isBlank()) || (propId != null && !propId.isBlank());
        model.addAttribute("hasGoogle", hasGoogle);

        // 2. Dados do Usuário Logado (Google ou Normal)
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

        // 3. Busca e Listagem de Livros
        List<Book> books;
        if (q != null && !q.isBlank()) {
            books = bookService.search(q);
        } else {
            // Usa o método que mostra apenas disponíveis (se você já aplicou a lógica da pergunta anterior)
            books = bookService.findAllOrdered();
        }
        model.addAttribute("books", books);
        model.addAttribute("q", q == null ? "" : q);

        // 4. CORREÇÃO: Lógica do botão "Adicionar Livros"
        // Agora aceita tanto CustomUserDetails (Senha) quanto OAuth2User (Google)
        boolean canAdd = false;
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails || principal instanceof OAuth2User) {
                canAdd = true;
            }
        }
        model.addAttribute("canAdd", canAdd);

        return "index";
    }
}