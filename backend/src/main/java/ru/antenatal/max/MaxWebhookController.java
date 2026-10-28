package ru.antenatal.max;

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

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/max/webhook")
@RequiredArgsConstructor
public class MaxWebhookController {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final MaxUpdateHandler updateHandler;

    @PostMapping("/{secret}")
    public ResponseEntity<Map<String, Boolean>> receive(
            @PathVariable String secret,
            @RequestHeader(value = "X-Max-Bot-Api-Secret", required = false) String headerSecret,
            @RequestBody String body) {

        String botToken = appProperties.getMax().getToken();
        if (botToken == null || botToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ok", false));
        }

        String configured = appProperties.getMax().getWebhookSecret();
        if (configured != null && !configured.isBlank()) {
            if (!TokenUtil.constantTimeEquals(configured, secret)
                    && !TokenUtil.constantTimeEquals(configured, headerSecret)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("ok", false));
            }
        }

        try {
            JsonNode update = objectMapper.readTree(body);
            updateHandler.handle(update);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            log.error("MAX update failed: {}", e.getClass().getName());
            log.debug("MAX update details", e);
            return ResponseEntity.ok(Map.of("ok", false));
        }
    }
}
