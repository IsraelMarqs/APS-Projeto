package com.example.demo;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public BookController(BookRepository bookRepository, UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/new")
    public String createForm(Model model, Authentication authentication) {
        // Only authenticated users can create books (form users or OAuth users)
        String email = getAuthenticatedEmail(authentication);
        if (email == null) {
            return "redirect:/login";
        }
        model.addAttribute("book", new Book());
        model.addAttribute("categories", getCategories());
        model.addAttribute("formAction", "/books/new");
        return "book_form";
    }

    @PostMapping("/new")
    public String createSubmit(@Valid @ModelAttribute Book book, BindingResult bindingResult, Authentication authentication, Model model) {
        String email = getAuthenticatedEmail(authentication);
        if (email == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", getCategories());
            model.addAttribute("formAction", "/books/new");
            return "book_form";
        }
        userRepository.findByEmail(email).ifPresent(book::setOwner);
        bookRepository.save(book);
        return "redirect:/";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Book> opt = bookRepository.findById(id);
        if (opt.isEmpty()) return "redirect:/";
        Book book = opt.get();
        if (!isOwner(authentication, book)) {
            return "redirect:/books/" + id;
        }
        model.addAttribute("book", book);
        model.addAttribute("categories", getCategories());
        model.addAttribute("formAction", "/books/" + id + "/edit");
        return "book_form";
    }

    @PostMapping("/{id}/edit")
    public String editSubmit(@PathVariable Long id, @Valid @ModelAttribute Book bookForm, BindingResult bindingResult, Authentication authentication, Model model) {
        Optional<Book> opt = bookRepository.findById(id);
        if (opt.isEmpty()) return "redirect:/";
        Book book = opt.get();
        if (!isOwner(authentication, book)) {
            return "redirect:/books/" + id;
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", getCategories());
            model.addAttribute("formAction", "/books/" + id + "/edit");
            return "book_form";
        }
        // update mutable fields
        book.setTitle(bookForm.getTitle());
        book.setAuthors(bookForm.getAuthors());
        book.setPublishedYear(bookForm.getPublishedYear());
        book.setPublisher(bookForm.getPublisher());
        book.setCategory(bookForm.getCategory());
        book.setLanguage(bookForm.getLanguage());
        book.setLocation(bookForm.getLocation());
        book.setContactNumber(bookForm.getContactNumber());
        bookRepository.save(book);
        return "redirect:/";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication) {
        Optional<Book> opt = bookRepository.findById(id);
        if (opt.isEmpty()) return "redirect:/";
        Book book = opt.get();
        if (isOwner(authentication, book)) {
            bookRepository.delete(book);
        }
        return "redirect:/";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Book> opt = bookRepository.findById(id);
        if (opt.isEmpty()) return "redirect:/";
        Book book = opt.get();
        model.addAttribute("book", book);
        boolean owner = isOwner(authentication, book);
        model.addAttribute("isOwner", owner);
        return "book_view";
    }

    @GetMapping("/{id}/request")
    public String requestLoan(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Book> opt = bookRepository.findById(id);
        if (opt.isEmpty()) return "redirect:/";
        Book book = opt.get();
        model.addAttribute("book", book);
        return "request";
    }

    private boolean isOwner(Authentication authentication, Book book) {
        String email = getAuthenticatedEmail(authentication);
        return email != null && book.getOwner() != null && email.equals(book.getOwner().getEmail());
    }

    private String getAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUsername();
        }
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
