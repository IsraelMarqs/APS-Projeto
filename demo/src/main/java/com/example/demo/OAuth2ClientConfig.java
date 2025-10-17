package com.example.demo;

import java.util.Collections;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    @ConditionalOnExpression("#{(environment['GOOGLE_CLIENT_ID'] != null and environment['GOOGLE_CLIENT_SECRET'] != null) or (environment['spring.security.oauth2.client.registration.google.client-id'] != null and environment['spring.security.oauth2.client.registration.google.client-secret'] != null)}")
    public ClientRegistrationRepository clientRegistrationRepository(Environment env) {
        // Read either from environment variables or spring properties
        String clientId = env.getProperty("GOOGLE_CLIENT_ID");
        if (clientId == null || clientId.isBlank()) {
            clientId = env.getProperty("spring.security.oauth2.client.registration.google.client-id");
        }
        String clientSecret = env.getProperty("GOOGLE_CLIENT_SECRET");
        if (clientSecret == null || clientSecret.isBlank()) {
            clientSecret = env.getProperty("spring.security.oauth2.client.registration.google.client-secret");
        }

        if (clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank()) {
            ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email")
                    .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                    .tokenUri("https://oauth2.googleapis.com/token")
                    .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .userNameAttributeName("sub")
                    .clientName("Google")
                    .build();
            return new InMemoryClientRegistrationRepository(registration);
        }
        // Should not reach here because bean is conditional, but in case, avoid returning an empty repository
        throw new IllegalStateException("OAuth2 client registration properties are missing");
    }
}
