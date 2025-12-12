package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // CORREÇÃO: Usamos a interface genérica OAuth2User.
        // Isso aceita tanto CustomOAuth2User quanto DefaultOidcUser (que o Google retorna).
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // Extraímos os dados diretamente do mapa de atributos.
        // O Google sempre retorna 'email', 'name' e 'picture'.
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String avatarUrl = oauthUser.getAttribute("picture");

        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser == null) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setPassword("{google}" + java.util.UUID.randomUUID().toString());
            newUser.setRole("ROLE_USER");

            userRepository.save(newUser);
        } else {
            // Atualiza dados se o usuário já existir
            existingUser.setAvatarUrl(avatarUrl);
            existingUser.setName(name);
            userRepository.save(existingUser);
        }

        response.sendRedirect("/profile");
    }
}