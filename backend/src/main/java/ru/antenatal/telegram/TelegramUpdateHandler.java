package ru.antenatal.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.LinkToken;
import ru.antenatal.domain.Platform;
import ru.antenatal.service.LinkService;
import ru.antenatal.service.LinkService.BotStatus;
import ru.antenatal.service.LinkService.UnlinkResult;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUpdateHandler {

    private static final String COMMANDS = """
            Команды:
            /start — меню и привязка
            /app — открыть приложение
            /status — проверить подключение
            /help — инструкция
            /stop — отключить уведомления""";

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

        if (isCommand(text, "/start")) {
            handleStart(chatId, fromId, text);
            return;
        }
        if (isCommand(text, "/app")) {
            telegramClient.sendMessage(chatId, "Откройте приложение, чтобы продолжить.",
                    appButton());
            return;
        }
        if (isCommand(text, "/help")) {
            telegramClient.sendMessage(chatId, helpText(), appButton());
            return;
        }
        if (isCommand(text, "/status")) {
            sendStatus(chatId, fromId);
            return;
        }
        if (isCommand(text, "/stop")) {
            handleStop(chatId, fromId, text);
            return;
        }

        telegramClient.sendMessage(chatId,
                "Не понял команду. Откройте /help, чтобы посмотреть инструкцию.", null);
    }

    private void handleStart(String chatId, String fromId, String text) {
        String payload = payload(text);
        if (payload.isBlank()) {
            telegramClient.sendMessage(chatId, welcomeText(), appButton());
            return;
        }
        if (payload.startsWith("dr_")) {
            handleDoctorLink(chatId, fromId, payload.substring(3));
            return;
        }
        handlePatientLink(chatId, payload);
    }

    private void handleStop(String chatId, String fromId, String text) {
        String payload = payload(text);
        if (!"confirm".equalsIgnoreCase(payload)) {
            telegramClient.sendMessage(chatId, """
                    Отключить уведомления?

                    Этот чат перестанет получать события. Если вы пациентка, доступ к приложению тоже закроется до новой привязки по QR.

                    Чтобы подтвердить, отправьте /stop confirm.
                    Историю чата это не удалит — её можно очистить вручную в Telegram.""", null);
            return;
        }

        UnlinkResult result = linkService.unlinkByMessenger(Platform.TELEGRAM, fromId);
        if (!result.patient() && !result.doctor()) {
            telegramClient.sendMessage(chatId, "Этот чат не подключён.", null);
            return;
        }
        telegramClient.sendMessage(chatId, """
                Уведомления отключены.

                Чтобы подключить снова, получите новый QR у врача или нажмите Telegram в кабинете.
                История чата остаётся в Telegram; очистить её можно вручную в настройках чата.""", null);
    }

    private void sendStatus(String chatId, String fromId) {
        BotStatus status = linkService.botStatus(Platform.TELEGRAM, fromId);
        if (!status.patient() && !status.doctor()) {
            telegramClient.sendMessage(chatId,
                    "Чат не подключён. Получите QR у врача или подключите уведомления в кабинете.",
                    null);
            return;
        }

        StringBuilder text = new StringBuilder();
        if (status.patient()) {
            text.append("Вы подключены как пациентка.");
            if (status.publicCode() != null && !status.publicCode().isBlank()) {
                text.append(" Карточка: ").append(status.publicCode()).append(".");
            }
            text.append('\n');
        }
        if (status.doctor()) {
            text.append("Вы подключены как врач.\n");
        }
        text.append("Канал: Telegram.");
        telegramClient.sendMessage(chatId, text.toString(), null);
    }

    private void handleDoctorLink(String chatId, String fromId, String rawToken) {
        try {
            linkService.confirmDoctorLink(rawToken, Platform.TELEGRAM, chatId, fromId);
            telegramClient.sendMessage(chatId, """
                    Уведомления подключены. События по вашим пациенткам будут приходить сюда.

                    Команды: /app, /status, /help, /stop.""", null);
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

    private String welcomeText() {
        return """
                Здравствуйте! Это помощник для беременных.

                Как начать:
                1. Получите QR-код у врача.
                2. Отсканируйте его или откройте ссылку.
                3. Подтвердите данные карточки и согласие.

                %s

                Если вы врач, подключите уведомления кнопкой Telegram в кабинете.""".formatted(COMMANDS);
    }

    private String helpText() {
        return """
                Помощник для беременных.

                %s

                Чем отличаются /start и /app:
                /start показывает инструкцию, команды и кнопку приложения.
                /app сразу открывает приложение без лишнего текста.

                Как подключиться:
                • пациентке — получить QR у врача, отсканировать и подтвердить данные;
                • врачу — нажать Telegram в кабинете и открыть ссылку.

                История:
                Telegram не позволяет боту удалить всю переписку. /stop отключает уведомления, а историю чата можно очистить вручную в Telegram.

                Приложение не заменяет врача. При тревожных симптомах звоните 103 или 112.""".formatted(COMMANDS);
    }

    private Object appButton() {
        return TelegramClient.appButton("Открыть приложение",
                appProperties.getPublicBaseUrl() + "/app");
    }

    private boolean isCommand(String text, String command) {
        return text.equals(command)
                || text.startsWith(command + " ")
                || text.startsWith(command + "@");
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
