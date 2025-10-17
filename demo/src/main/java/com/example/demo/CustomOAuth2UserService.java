package com.example.demo;

import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauthUser = delegate.loadUser(userRequest);
        Map<String, Object> attrs = oauthUser.getAttributes();

        // Attempt to extract email and name in a provider-agnostic way (Google provides 'email' and 'name')
        String email = (String) attrs.get("email");
        String name = (String) attrs.get("name");
        String picture = (String) attrs.get("picture");

        if (email != null && !email.isBlank()) {
            userRepository.findByEmail(email).ifPresentOrElse(u -> {
                // update basic info
                if (name != null && !name.isBlank()) u.setName(name);
                userRepository.save(u);
            }, () -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name == null || name.isBlank() ? email : name);
                // No password for OAuth users, store an empty string
                newUser.setPassword("");
                userRepository.save(newUser);
            });
        }

        // Return a DefaultOAuth2User with same authorities and attributes
        return new DefaultOAuth2User(
                oauthUser.getAuthorities().stream().map(a -> new SimpleGrantedAuthority(a.getAuthority())).toList(),
                attrs,
                "email"
        );
    }
}

