package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Iterator;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // H2 Console requires frame options to be disabled
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        // Public endpoints
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**", "/webjars/**", "/error").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        // Form login configuration
        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .permitAll()
        );

        // Conditionally enable OAuth2 login if client registrations are present
        boolean hasRegistration = false;
        if (clientRegistrationRepository instanceof Iterable<?>) {
            Iterator<?> it = ((Iterable<?>) clientRegistrationRepository).iterator();
            hasRegistration = it.hasNext();
        }

        if (hasRegistration) {
            http.oauth2Login(oauth2 -> {
                oauth2.loginPage("/login");
                // If we have a custom user service, register it to persist OAuth users
                if (customOAuth2UserService != null) {
                    oauth2.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService));
                }
                oauth2.defaultSuccessUrl("/", true);
            });
        }

        return http.build();
    }
}
