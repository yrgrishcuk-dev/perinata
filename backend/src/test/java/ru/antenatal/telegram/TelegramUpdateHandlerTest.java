package ru.antenatal.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.LinkToken;
import ru.antenatal.domain.Platform;
import ru.antenatal.service.LinkService;
import ru.antenatal.service.LinkService.BotStatus;
import ru.antenatal.service.LinkService.UnlinkResult;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramUpdateHandlerTest {

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private LinkService linkService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TelegramUpdateHandler handler;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setPublicBaseUrl("https://perinata.ru");
        handler = new TelegramUpdateHandler(telegramClient, linkService, properties);
    }

    @Test
    void startShowsMenuAndCommands() throws Exception {
        handler.handle(update("/start"));

        verify(telegramClient).sendMessage(eq("10"), contains("/app — открыть приложение"), any());
        verify(telegramClient).sendMessage(eq("10"), contains("/stop — отключить уведомления"), any());
    }

    @Test
    void helpShowsCommands() throws Exception {
        handler.handle(update("/help"));

        verify(telegramClient).sendMessage(eq("10"), contains("Помощник беременной."), any());
        verify(telegramClient).sendMessage(eq("10"), contains("/stop — отключить уведомления"), any());
    }

    @Test
    void stopAsksForConfirmation() throws Exception {
        handler.handle(update("/stop"));

        verify(telegramClient).sendMessage(eq("10"), contains("/stop confirm"), eq(null));
    }

    @Test
    void stopConfirmUnlinksMessenger() throws Exception {
        when(linkService.unlinkByMessenger(Platform.TELEGRAM, "20"))
                .thenReturn(new UnlinkResult(true, false));

        handler.handle(update("/stop confirm"));

        verify(telegramClient).sendMessage(eq("10"), contains("Уведомления отключены"), eq(null));
    }

    @Test
    void statusShowsPatientRoleWithoutName() throws Exception {
        when(linkService.botStatus(Platform.TELEGRAM, "20"))
                .thenReturn(new BotStatus(true, false, "A7K2"));

        handler.handle(update("/status"));

        verify(telegramClient).sendMessage(eq("10"), contains("пациентка"), eq(null));
        verify(telegramClient).sendMessage(eq("10"), contains("A7K2"), eq(null));
    }

    @Test
    void patientStartUsesInviteButton() throws Exception {
        when(linkService.findValid("token")).thenReturn(Optional.of(new LinkToken()));

        handler.handle(update("/start p_token"));

        verify(telegramClient).sendMessage(eq("10"), contains("Вас пригласил врач"), any());
    }

    private com.fasterxml.jackson.databind.JsonNode update(String text) throws Exception {
        return objectMapper.readTree("""
                {"message":{"chat":{"id":10},"from":{"id":20},"text":"%s"}}
                """.formatted(text));
    }
}
