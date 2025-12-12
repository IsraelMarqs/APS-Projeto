package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;
import java.util.List;

@Service
public class AIService {

    @Value("${google.ai.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    public String generateBookDescription(String title, String author) {
        if (apiKey == null || apiKey.isEmpty()) return "Configure a API Key no application.properties para usar a IA.";

        String prompt = "Faça um resumo curto, estilo que usariam na amazon(máximo 300 caracteres) em português sobre o livro (Seja '" + title + "' de " + author + ". Não dê spoilers.";

        // Estrutura JSON simples para a API do Gemini
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            Map response = restTemplate.postForObject(API_URL + apiKey, entity, Map.class);

            // Navegando no JSON de resposta para pegar o texto
            // candidates[0].content.parts[0].text
            List candidates = (List) response.get("candidates");
            Map candidate = (Map) candidates.get(0);
            Map content = (Map) candidate.get("content");
            List parts = (List) content.get("parts");
            Map part = (Map) parts.get(0);

            return (String) part.get("text");
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao gerar descrição. Verifique o console.";
        }
    }
}