package com.example.demo.controller;

import com.example.demo.entity.Book;
import com.example.demo.entity.User;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.AIService;
import com.example.demo.service.BookService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final BookRepository bookRepository;

    public BookController(BookService bookService, UserRepository userRepository, AIService aiService, BookRepository bookRepository) {
        this.bookService = bookService;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.bookRepository = bookRepository;
    }

    // --- CRIAR LIVRO ---
    @GetMapping("/new")
    public String createForm(Model model, Authentication authentication) {
        String email = getAuthenticatedEmail(authentication);
        if (email == null) return "redirect:/login"; // Segurança extra

        model.addAttribute("book", new Book());
        model.addAttribute("categories", getCategories());
        model.addAttribute("formAction", "/books/new");
        return "book_form";
    }

    @PostMapping("/new")
    public String createSubmit(@Valid @ModelAttribute Book book,
                               BindingResult bindingResult,
                               Authentication authentication,
                               Model model) {
        String email = getAuthenticatedEmail(authentication);
        if (email == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", getCategories());
            model.addAttribute("formAction", "/books/new");
            return "book_form";
        }

        // Associa o livro ao usuário logado (Google ou Normal)
        userRepository.findByEmail(email).ifPresent(book::setOwner);

        // Garante que o livro nasce disponível
        book.setAvailable(true);

        bookService.save(book);
        return "redirect:/";
    }

    // --- EDITAR LIVRO ---
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Book> opt = bookService.findById(id);
        if (opt.isEmpty()) return "redirect:/";

        Book book = opt.get();
        if (!isOwner(authentication, book)) return "redirect:/books/" + id; // Só o dono edita

        model.addAttribute("book", book);
        model.addAttribute("categories", getCategories());
        model.addAttribute("formAction", "/books/" + id + "/edit");
        return "book_form";
    }

    @PostMapping("/{id}/edit")
    public String editSubmit(@PathVariable Long id,
                             @Valid @ModelAttribute Book bookForm,
                             BindingResult bindingResult,
                             Authentication authentication,
                             Model model) {
        Optional<Book> opt = bookService.findById(id);
        if (opt.isEmpty()) return "redirect:/";

        Book book = opt.get();
        if (!isOwner(authentication, book)) return "redirect:/books/" + id;

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", getCategories());
            model.addAttribute("formAction", "/books/" + id + "/edit");
            return "book_form";
        }

        // Atualiza campos
        book.setTitle(bookForm.getTitle());
        book.setAuthors(bookForm.getAuthors());
        book.setPublishedYear(bookForm.getPublishedYear());
        book.setPublisher(bookForm.getPublisher());
        book.setCategory(bookForm.getCategory());
        book.setLanguage(bookForm.getLanguage());
        book.setLocation(bookForm.getLocation());
        book.setContactNumber(bookForm.getContactNumber());

        // CORREÇÃO: Adicionado a atualização da descrição (faltava no seu código)
        book.setDescription(bookForm.getDescription());

        bookService.save(book);
        return "redirect:/";
    }

    // --- DELETAR LIVRO ---
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication) {
        Optional<Book> opt = bookService.findById(id);
        if (opt.isEmpty()) return "redirect:/";

        Book book = opt.get();
        if (isOwner(authentication, book)) {
            bookService.delete(book);
        }
        return "redirect:/";
    }

    // --- VISUALIZAR LIVRO ---
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Book> opt = bookService.findById(id);
        if (opt.isEmpty()) return "redirect:/";

        Book book = opt.get();
        model.addAttribute("book", book);

        boolean owner = isOwner(authentication, book);
        model.addAttribute("isOwner", owner);

        return "book_view";
    }

    // --- SOLICITAR EMPRÉSTIMO ---
    @GetMapping("/{id}/request")
    public String requestLoan(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Book> opt = bookService.findById(id);
        if (opt.isEmpty()) return "redirect:/";

        Book book = opt.get();
        model.addAttribute("book", book);
        return "request";
    }

    // --- GERAÇÃO IA ---
    @PostMapping("/generate-description")
    @ResponseBody
    public Map<String, String> generateDescription(@RequestBody Map<String, String> payload) {
        String title = payload.get("title");
        String authors = payload.get("authors");
        String description = aiService.generateBookDescription(title, authors);
        return Map.of("description", description);
    }

    // --- 2. NOVO MÉTODO: MEUS LIVROS ---
    @GetMapping("/my-books")
    public String myBooks(Model model, Authentication authentication) {
        String email = getAuthenticatedEmail(authentication);
        if (email == null) return "redirect:/login";

        // Busca o usuário no banco para pegar o objeto User completo
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return "redirect:/login";

        // Busca os livros desse usuário
        List<Book> myBooks = bookRepository.findByOwnerOrderByTitleAsc(userOpt.get());

        model.addAttribute("books", myBooks);
        return "my_books";
    }

    // --- MÉTODOS AUXILIARES ---

    // Verifica se o usuário logado é dono do livro
    private boolean isOwner(Authentication authentication, Book book) {
        String email = getAuthenticatedEmail(authentication);
        return email != null && book.getOwner() != null && email.equals(book.getOwner().getEmail());
    }

    // Extrai o email independente do tipo de login (Google ou Senha)
    private String getAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Object principal = authentication.getPrincipal();

        // Login normal
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUsername();
        }
        // Login Google
        if (principal instanceof OAuth2User) {
            Object email = ((OAuth2User) principal).getAttribute("email");
            return email == null ? null : email.toString();
        }
        return null;
    }

    private String[] getCategories() {
        return new String[]{"Ação", "Aventura", "Ficção", "Não Ficção", "Romance", "Tecnologia", "Biografia", "Outros"};
    }
}