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
    private final BookService bookService;
    private final UserRepository userRepository;
    private final BookRepository bookRepository; // Adicionado para atualizar o status do livro

    // Injeção de dependências atualizada
    public LoanRequestController(LoanRequestRepository loanRequestRepository,
                                 BookService bookService,
                                 UserRepository userRepository,
                                 BookRepository bookRepository) {
        this.loanRequestRepository = loanRequestRepository;
        this.bookService = bookService;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    // --- 1. Fazer um pedido (Solicitar livro) ---
    @PostMapping("/books/{id}/request")
    public String submitRequest(@PathVariable Long id, @RequestParam(required = false) String message, Authentication authentication, Model model) {
        String email = authenticatedEmail(authentication);
        if (email == null) return "redirect:/login";

        Optional<Book> ob = bookService.findById(id);
        if (ob.isEmpty()) return "redirect:/";
        Book book = ob.get();

        // Não pode pedir o próprio livro
        if (book.getOwner() != null && book.getOwner().getEmail().equals(email)) {
            model.addAttribute("errorMessage", "Você não pode solicitar empréstimo do seu próprio livro.");
            model.addAttribute("book", book);
            return "book_view"; // Supondo que exista essa view, senão redirecionar
        }

        Optional<User> ou = userRepository.findByEmail(email);
        if (ou.isEmpty()) return "redirect:/login";
        User requester = ou.get();

        LoanRequest lr = new LoanRequest();
        lr.setBook(book);
        lr.setRequester(requester);
        lr.setMessage(message == null ? "" : message.trim());
        lr.setStatus(LoanRequest.Status.PENDING); // Garante status inicial
        loanRequestRepository.save(lr);

        return "redirect:/requests";
    }

    // --- 2. Listar MEUS pedidos (Eu pedi emprestado) ---
    @GetMapping("/requests")
    public String myRequests(Model model, Authentication authentication) {
        String email = authenticatedEmail(authentication);
        if (email == null) return "redirect:/login";

        Optional<User> ou = userRepository.findByEmail(email);
        if (ou.isEmpty()) return "redirect:/login";
        User user = ou.get();

        // --- LÓGICA DE LIMPEZA ---
        List<LoanRequest> unseen = loanRequestRepository.findByRequesterAndStatusInAndSeenFalse(
                user, List.of(LoanRequest.Status.ACCEPTED, LoanRequest.Status.DECLINED));

        if (!unseen.isEmpty()) {
            unseen.forEach(req -> req.setSeen(true));
            loanRequestRepository.saveAll(unseen);
        }

        // CORREÇÃO: Força o contador a zerar na visualização atual
        // Isso sobrescreve o valor antigo que veio do GlobalNotificationAdvice
        model.addAttribute("updatesCount", 0L);
        // -------------------------

        List<LoanRequest> list = loanRequestRepository.findByRequesterOrderByCreatedAtDesc(user);
        model.addAttribute("requests", list);
        return "requests";
    }

    // --- 3. Listar pedidos RECEBIDOS (Querem meus livros) ---
    @GetMapping("/requests/received")
    public String receivedRequests(Model model, Authentication authentication) {
        String email = authenticatedEmail(authentication);
        if (email == null) return "redirect:/login";

        Optional<User> ou = userRepository.findByEmail(email);
        if (ou.isEmpty()) return "redirect:/login";
        User user = ou.get();

        List<LoanRequest> list = loanRequestRepository.findByBookOwnerOrderByCreatedAtDesc(user);
        model.addAttribute("requests", list);
        return "requests_received"; // Procura requests_received.html
    }

    // --- 4. Responder a um pedido (Aceitar/Recusar) ---
    @PostMapping("/requests/{id}/respond")
    public String respond(@PathVariable Long id, @RequestParam("action") String action, Authentication authentication) {
        String email = authenticatedEmail(authentication);
        if (email == null) return "redirect:/login";

        Optional<LoanRequest> olr = loanRequestRepository.findById(id);
        if (olr.isEmpty()) return "redirect:/requests/received";
        LoanRequest request = olr.get();

        // Segurança: só o dono pode responder
        if (request.getBook().getOwner() == null || !email.equals(request.getBook().getOwner().getEmail())) {
            return "redirect:/requests/received";
        }

        if ("accept".equalsIgnoreCase(action)) {
            // Lógica de Exclusividade:

            // 1. Aceita este pedido
            request.setStatus(LoanRequest.Status.ACCEPTED);

            // 2. Marca livro como indisponível
            Book book = request.getBook();
            book.setAvailable(false);
            bookRepository.save(book);

            // 3. Recusa automaticamente os outros pedidos PENDENTES deste livro
            List<LoanRequest> pendingRequests = loanRequestRepository.findByBookAndStatus(book, LoanRequest.Status.PENDING);
            for (LoanRequest other : pendingRequests) {
                if (!other.getId().equals(request.getId())) {
                    other.setStatus(LoanRequest.Status.DECLINED);
                }
            }
            loanRequestRepository.saveAll(pendingRequests);

        } else if ("decline".equalsIgnoreCase(action)) {
            request.setStatus(LoanRequest.Status.DECLINED);
        }

        loanRequestRepository.save(request);
        return "redirect:/requests/received";
    }

    // Helper para pegar email do usuário logado (Google ou Normal)
    private String authenticatedEmail(Authentication authentication) {
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
}