package ru.antenatal.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.antenatal.common.TokenUtil;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.TelegramUpdate;
import ru.antenatal.repo.TelegramUpdateRepository;
import ru.antenatal.telegram.TelegramUpdateHandler;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/telegram/webhook")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final TelegramUpdateHandler updateHandler;
    private final TelegramUpdateRepository telegramUpdateRepository;

    @PostMapping("/{secret}")
    public ResponseEntity<Map<String, String>> receive(
            @PathVariable String secret,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String headerSecret,
            @RequestBody String body) {

        String configured = appProperties.getBot().getWebhookSecret();
        if (configured != null && !configured.isBlank()) {
            if (!TokenUtil.constantTimeEquals(configured, secret)
                    && !TokenUtil.constantTimeEquals(configured, headerSecret)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
            }
        }

        try {
            JsonNode update = objectMapper.readTree(body);
            long updateId = update.path("update_id").asLong(0);
            if (updateId > 0 && telegramUpdateRepository.existsById(updateId)) {
                return ResponseEntity.ok(Map.of("status", "duplicate"));
            }
            updateHandler.handle(update);
            if (updateId > 0) {
                TelegramUpdate saved = new TelegramUpdate();
                saved.setUpdateId(updateId);
                saved.setReceivedAt(Instant.now());
                telegramUpdateRepository.save(saved);
            }
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            log.error("Telegram update failed: {}", e.getClass().getName());
            log.debug("Telegram update details", e);
            return ResponseEntity.ok(Map.of("status", "error"));
        }
    }
}
