package com.example.demo;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalNotificationAdvice {

    private final LoanRequestRepository loanRequestRepository;
    private final UserRepository userRepository;

    public GlobalNotificationAdvice(LoanRequestRepository loanRequestRepository, UserRepository userRepository) {
        this.loanRequestRepository = loanRequestRepository;
        this.userRepository = userRepository;
    }

    @ModelAttribute("incomingCount")
    public long incomingCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return 0;
        User user = getUser(authentication);
        if (user == null) return 0;

        // Conta quantos pedidos PENDING existem para meus livros
        return loanRequestRepository.countByBookOwnerAndStatus(user, LoanRequest.Status.PENDING);
    }

    @ModelAttribute("updatesCount")
    public long updatesCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return 0;
        User user = getUser(authentication);
        if (user == null) return 0;

        // Conta quantos pedidos MEUS foram ACEITOS ou RECUSADOS e eu ainda não vi
        return loanRequestRepository.countByRequesterAndStatusInAndSeenFalse(
                user,
                List.of(LoanRequest.Status.ACCEPTED, LoanRequest.Status.DECLINED)
        );
    }

    private User getUser(Authentication authentication) {
        String email = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails) {
            email = ((CustomUserDetails) principal).getUsername();
        } else if (principal instanceof OAuth2User) {
            Object emailAttr = ((OAuth2User) principal).getAttribute("email");
            if (emailAttr != null) email = emailAttr.toString();
        }

        if (email == null) return null;
        return userRepository.findByEmail(email).orElse(null);
    }
}