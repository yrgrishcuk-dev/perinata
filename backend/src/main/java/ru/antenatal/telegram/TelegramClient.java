package ru.antenatal.telegram;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class TelegramClient {

    private final AppProperties appProperties;
    private final RestClient restClient = buildClient();

    private static RestClient buildClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .requestFactory(factory)
                .build();
    }

    public boolean sendMessage(String chatId, String text, Object replyMarkup) {
        String token = appProperties.getBot().getToken();
        if (token == null || token.isBlank() || chatId == null || chatId.isBlank()) {
            return false;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("disable_web_page_preview", true);
        if (replyMarkup != null) {
            body.put("reply_markup", replyMarkup);
        }
        return call("sendMessage", body);
    }

    public boolean setMyCommands(List<Map<String, String>> commands) {
        return call("setMyCommands", Map.of("commands", commands));
    }

    public boolean setMyDescription(String description) {
        return call("setMyDescription", Map.of("description", description));
    }

    public boolean setMyShortDescription(String description) {
        return call("setMyShortDescription", Map.of("short_description", description));
    }

    public boolean setChatMenuButton(String text, String url) {
        Map<String, Object> webApp = Map.of("url", url);
        Map<String, Object> menuButton = Map.of(
                "type", "web_app",
                "text", text,
                "web_app", webApp);
        return call("setChatMenuButton", Map.of("menu_button", menuButton));
    }

    public boolean call(String method, Object body) {
        String token = appProperties.getBot().getToken();
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            restClient.post()
                    .uri("/bot" + token + "/" + method)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            // Не логируем URI: в нём находится токен бота.
            log.warn("Telegram {} failed: {}", method, e.getClass().getSimpleName());
            return false;
        }
    }

    public static Object appButton(String text, String url) {
        Map<String, Object> button = new HashMap<>();
        button.put("text", text);
        if (url.startsWith("https://")) {
            button.put("web_app", Map.of("url", url));
        } else {
            button.put("url", url);
        }
        return Map.of("inline_keyboard", List.of(List.of(button)));
    }
}
