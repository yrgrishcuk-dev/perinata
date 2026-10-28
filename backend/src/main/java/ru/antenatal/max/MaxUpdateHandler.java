package ru.antenatal.max;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.antenatal.domain.LinkToken;
import ru.antenatal.domain.Platform;
import ru.antenatal.service.LinkService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaxUpdateHandler {

    private final MaxClient maxClient;
    private final LinkService linkService;

    public void handle(JsonNode update) {
        String type = update.path("update_type").asText();
        if ("bot_started".equals(type)) {
            String userId = update.path("user").path("user_id").asText();
            String chatId = update.path("chat_id").asText();
            handleStart(userId, chatId, update.path("payload").asText(""));
            return;
        }
        if ("message_created".equals(type)) {
            JsonNode message = update.path("message");
            String userId = message.path("sender").path("user_id").asText();
            String chatId = update.path("chat_id").asText();
            String text = message.path("body").path("text").asText("").trim();
            if (text.startsWith("/start")) {
                handleStart(userId, chatId, commandPayload(text));
            } else if (text.startsWith("/app") || text.startsWith("/menu")) {
                sendWelcome(userId, chatId);
            }
        }
    }

    private void handleStart(String userId, String chatId, String payload) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        if (payload.startsWith("dr_")) {
            try {
                linkService.confirmDoctorLink(payload.substring(3), Platform.MAX, chatId, userId);
                maxClient.sendMessage(userId,
                        "Уведомления подключены. События по вашим пациенткам будут приходить сюда.",
                        null);
            } catch (RuntimeException e) {
                maxClient.sendMessage(userId,
                        "Ссылка недействительна или истекла. Создайте новую в кабинете врача.",
                        null);
            }
            return;
        }

        if (!payload.isBlank()) {
            String rawToken = payload.startsWith("p_") ? payload.substring(2) : payload;
            Optional<LinkToken> token = linkService.findValid(rawToken);
            if (token.isEmpty()) {
                maxClient.sendMessage(userId,
                        "Ссылка недействительна или истекла. Попросите врача показать QR-код ещё раз.",
                        null);
                return;
            }
            Object button = maxClient.appButton("Открыть приглашение", rawToken);
            if (button == null) {
                maxClient.sendMessage(userId,
                        "Мини-приложение MAX ещё не настроено. Обратитесь к администратору.",
                        null);
                return;
            }
            maxClient.sendMessage(userId,
                    "Вас пригласил врач. Откройте приложение, проверьте данные карточки и подтвердите привязку.",
                    button);
            return;
        }

        sendWelcome(userId, chatId);
    }

    private void sendWelcome(String userId, String chatId) {
        Object button = maxClient.appButton("Открыть приложение", "");
        if (button == null) {
            maxClient.sendMessage(userId,
                    "Здравствуйте! Получите QR-код у своего врача, чтобы открыть приложение.",
                    null);
            return;
        }
        maxClient.sendMessage(userId,
                "Здравствуйте! Это помощник для беременных. Получите QR-код у своего врача или откройте приложение.",
                button);
    }

    private String commandPayload(String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) {
            return "";
        }
        String value = parts[1].trim();
        return value.startsWith("/") ? "" : value;
    }
}
