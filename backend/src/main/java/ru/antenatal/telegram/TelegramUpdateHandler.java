package ru.antenatal.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.LinkToken;
import ru.antenatal.service.LinkService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUpdateHandler {

    private final TelegramClient telegramClient;
    private final LinkService linkService;
    private final AppProperties appProperties;

    public void handle(JsonNode update) {
        JsonNode message = update.path("message");
        if (message.isMissingNode() || message.isNull()) {
            return;
        }
        String chatId = message.path("chat").path("id").asText();
        String fromId = message.path("from").path("id").asText();
        String text = message.path("text").asText("").trim();

        if (text.startsWith("/start")) {
            String payload = payload(text);
            if (payload.isBlank()) {
                telegramClient.sendMessage(chatId,
                        "Здравствуйте! Это помощник для беременных. Получите QR-код у своего врача и отсканируйте его.",
                        TelegramClient.appButton("Открыть приложение", appProperties.getPublicBaseUrl() + "/app"));
                return;
            }
            if (payload.startsWith("dr_")) {
                handleDoctorLink(chatId, fromId, payload.substring(3));
                return;
            }
            handlePatientLink(chatId, payload);
            return;
        }

        if (text.startsWith("/app") || text.startsWith("/menu")) {
            telegramClient.sendMessage(chatId, "Откройте приложение, чтобы продолжить.",
                    TelegramClient.appButton("Открыть приложение", appProperties.getPublicBaseUrl() + "/app"));
            return;
        }

        telegramClient.sendMessage(chatId,
                "Команды: /start — начать, /app — открыть приложение, /help — помощь.",
                null);
    }

    private void handleDoctorLink(String chatId, String fromId, String rawToken) {
        try {
            linkService.confirmDoctorLink(rawToken, chatId, fromId);
            telegramClient.sendMessage(chatId,
                    "Уведомления подключены. События по вашим пациенткам будут приходить сюда.",
                    null);
        } catch (RuntimeException e) {
            log.info("Doctor link failed: {}", e.getMessage());
            telegramClient.sendMessage(chatId,
                    "Ссылка недействительна или истекла. Создайте новую в кабинете врача.", null);
        }
    }

    private void handlePatientLink(String chatId, String payload) {
        String rawToken = payload.startsWith("p_") ? payload.substring(2) : payload;
        Optional<LinkToken> token = linkService.findValid(rawToken);
        if (token.isEmpty()) {
            telegramClient.sendMessage(chatId,
                    "Ссылка недействительна или истекла. Попросите врача показать QR-код ещё раз.",
                    null);
            return;
        }
        String url = appProperties.getPublicBaseUrl() + "/invite?t=" + rawToken;
        telegramClient.sendMessage(chatId,
                "Вас пригласил врач. Откройте приложение, проверьте данные карточки и подтвердите привязку.",
                TelegramClient.appButton("Открыть приглашение", url));
    }

    private String payload(String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) {
            return "";
        }
        String value = parts[1].trim();
        if (value.startsWith("/")) {
            return "";
        }
        return value;
    }
}
