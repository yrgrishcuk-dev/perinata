package ru.antenatal.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.antenatal.config.AppProperties;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotConfigurer {

    private static final String MENU_TEXT = "Открыть приложение";
    private static final String DESCRIPTION =
            "Помощник для беременных: дневник, симптомы, связь с врачом.";
    private static final String SHORT_DESCRIPTION =
            "Дневник беременности и связь с врачом.";

    private static final List<Map<String, String>> COMMANDS = List.of(
            Map.of("command", "start", "description", "Начать и подключить карточку"),
            Map.of("command", "app", "description", "Открыть приложение"),
            Map.of("command", "help", "description", "Инструкция и команды"),
            Map.of("command", "status", "description", "Проверить подключение"),
            Map.of("command", "stop", "description", "Отключить уведомления")
    );

    private final TelegramClient telegramClient;
    private final AppProperties appProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleConfiguration() {
        if (appProperties.getBot().getToken() == null
                || appProperties.getBot().getToken().isBlank()) {
            return;
        }
        Thread thread = new Thread(this::configureWithRetries, "telegram-bot-config");
        thread.setDaemon(true);
        thread.start();
    }

    private void configureWithRetries() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (applyOnce()) {
                log.info("Telegram bot config applied");
                return;
            }
            if (attempt < 3) {
                sleep(attempt * 5_000L);
            }
        }
        log.warn("Telegram bot config was not applied; commands and menu may be stale");
    }

    private boolean applyOnce() {
        String baseUrl = appProperties.getPublicBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        boolean commands = telegramClient.setMyCommands(COMMANDS);
        boolean description = telegramClient.setMyDescription(DESCRIPTION);
        boolean shortDescription = telegramClient.setMyShortDescription(SHORT_DESCRIPTION);
        boolean menu = telegramClient.setChatMenuButton(MENU_TEXT, baseUrl + "/app");
        return commands && description && shortDescription && menu;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
