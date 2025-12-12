package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Define as URLs públicas e protegidas
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/error", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Desabilita CSRF (apenas para desenvolvimento/teste)
                .csrf(csrf -> csrf.disable())
                // Configuração do Login Normal (Formulário)
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email") // CORREÇÃO CRÍTICA 1: Avisa que usamos 'email' no HTML, não 'username'
                        .defaultSuccessUrl("/profile", true)
                        .permitAll()
                )
                // Configuração do Login com Google
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(user -> user.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                );

        return http.build();
    }

    // CORREÇÃO CRÍTICA 2: Define o codificador de senha (BCrypt) para o login normal funcionar
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}