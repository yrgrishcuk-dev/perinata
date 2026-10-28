package ru.antenatal.max;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.antenatal.config.AppProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MaxClient {

    private final AppProperties appProperties;
    private final RestClient restClient;

    public MaxClient(AppProperties appProperties) {
        this.appProperties = appProperties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.restClient = RestClient.builder()
                .baseUrl(appProperties.getMax().getApiBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public boolean sendMessage(String userId, String text, Object attachments) {
        String token = appProperties.getMax().getToken();
        if (token == null || token.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("text", text);
            if (attachments != null) {
                body.put("attachments", attachments);
            }
            restClient.post()
                    .uri("/messages?user_id={userId}", userId)
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("MAX send failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    public Object appButton(String text, String payload) {
        String username = appProperties.getMax().getUsername();
        if (username == null || username.isBlank()) {
            return null;
        }
        Map<String, Object> button = new HashMap<>();
        button.put("type", "open_app");
        button.put("text", text);
        button.put("web_app", username);
        if (payload != null && !payload.isBlank()) {
            button.put("payload", payload);
        }
        Map<String, Object> keyboard = new HashMap<>();
        keyboard.put("buttons", List.of(List.of(button)));
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("type", "inline_keyboard");
        attachment.put("payload", keyboard);
        return List.of(attachment);
    }

    public Object linkButton(String text, String url) {
        Map<String, Object> button = Map.of(
                "type", "link",
                "text", text,
                "url", url);
        Map<String, Object> keyboard = Map.of("buttons", List.of(List.of(button)));
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("type", "inline_keyboard");
        attachment.put("payload", keyboard);
        return List.of(attachment);
    }
}
