package com.example.demo;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class LoanRequestController {

    private final LoanRequestRepository loanRequestRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public LoanRequestController(LoanRequestRepository loanRequestRepository, BookRepository bookRepository, UserRepository userRepository) {
        this.loanRequestRepository = loanRequestRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/books/{id}/request")
    public String submitRequest(@PathVariable Long id, @RequestParam(required = false) String message, Authentication authentication, Model model) {
        String email = authenticatedEmail(authentication);
        if (email == null) {
            return "redirect:/login";
        }
        Optional<Book> ob = bookRepository.findById(id);
        if (ob.isEmpty()) return "redirect:/";
        Book book = ob.get();
        // cannot request your own book
        if (book.getOwner() != null && book.getOwner().getEmail().equals(email)) {
            model.addAttribute("errorMessage", "Você não pode solicitar empréstimo do seu próprio livro.");
            model.addAttribute("book", book);
            return "book_view";
        }
        Optional<User> ou = userRepository.findByEmail(email);
        if (ou.isEmpty()) return "redirect:/login";
        User requester = ou.get();

        LoanRequest lr = new LoanRequest();
        lr.setBook(book);
        lr.setRequester(requester);
        lr.setMessage(message == null ? "" : message.trim());
        loanRequestRepository.save(lr);
        return "redirect:/requests";
    }

    @GetMapping("/requests")
    public String myRequests(Model model, Authentication authentication) {
        String email = authenticatedEmail(authentication);
        if (email == null) return "redirect:/login";
        Optional<User> ou = userRepository.findByEmail(email);
        if (ou.isEmpty()) return "redirect:/login";
        User user = ou.get();
        List<LoanRequest> list = loanRequestRepository.findByRequesterOrderByCreatedAtDesc(user);
        model.addAttribute("requests", list);
        return "requests";
    }

    @GetMapping("/requests/received")
    public String receivedRequests(Model model, Authentication authentication) {
        String email = authenticatedEmail(authentication);
        if (email == null) return "redirect:/login";
        Optional<User> ou = userRepository.findByEmail(email);
        if (ou.isEmpty()) return "redirect:/login";
        User user = ou.get();
        List<LoanRequest> list = loanRequestRepository.findByBookOwnerOrderByCreatedAtDesc(user);
        model.addAttribute("requests", list);
        return "requests_received";
    }

    @PostMapping("/requests/{id}/respond")
    public String respond(@PathVariable Long id, @RequestParam("action") String action, Authentication authentication) {
        String email = authenticatedEmail(authentication);
        if (email == null) return "redirect:/login";
        Optional<LoanRequest> olr = loanRequestRepository.findById(id);
        if (olr.isEmpty()) return "redirect:/requests/received";
        LoanRequest lr = olr.get();
        // only owner of book can respond
        if (lr.getBook().getOwner() == null || !email.equals(lr.getBook().getOwner().getEmail())) {
            return "redirect:/requests/received";
        }
        if ("accept".equalsIgnoreCase(action)) lr.setStatus(LoanRequest.Status.ACCEPTED);
        else if ("decline".equalsIgnoreCase(action)) lr.setStatus(LoanRequest.Status.DECLINED);
        loanRequestRepository.save(lr);
        return "redirect:/requests/received";
    }

    private String authenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) return ((CustomUserDetails) principal).getUsername();
        if (principal instanceof OAuth2User) {
            Object email = ((OAuth2User) principal).getAttribute("email");
            return email == null ? null : email.toString();
        }
        return null;
    }
}

