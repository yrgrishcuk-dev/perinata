package ru.antenatal.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.antenatal.common.ApiException;
import ru.antenatal.common.TokenUtil;
import ru.antenatal.config.AppProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InitDataValidator {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public record TelegramUser(String id, String firstName, String username) {
    }

    public TelegramUser validate(String initData) {
        return validate(initData, appProperties.getBot().getToken(), "Telegram-бот не настроен");
    }

    public TelegramUser validateMax(String initData) {
        return validate(initData, appProperties.getMax().getToken(), "MAX-бот не настроен");
    }

    private TelegramUser validate(String initData, String botToken, String notConfiguredMessage) {
        if (botToken == null || botToken.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, notConfiguredMessage);
        }
        if (initData == null || initData.isBlank()) {
            throw ApiException.unauthorized("Нет данных мессенджера");
        }

        Map<String, String> data = parse(initData);
        String hash = data.remove("hash");
        if (hash == null || hash.isBlank()) {
            throw ApiException.unauthorized("Некорректные данные мессенджера");
        }

        String dataCheckString = data.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        byte[] secret = hmac("WebAppData".getBytes(StandardCharsets.UTF_8),
                botToken.getBytes(StandardCharsets.UTF_8));
        String computed = HexFormat.of().formatHex(hmac(secret,
                dataCheckString.getBytes(StandardCharsets.UTF_8)));

        if (!TokenUtil.constantTimeEquals(computed, hash)) {
            throw ApiException.unauthorized("Подпись мессенджера неверна");
        }

        String authDate = data.get("auth_date");
        if (authDate != null) {
            long timestamp;
            try {
                timestamp = Long.parseLong(authDate);
            } catch (NumberFormatException e) {
                throw ApiException.unauthorized("Некорректная дата мессенджера");
            }
            long age = Instant.now().getEpochSecond() - timestamp;
            if (age < 0 || age > appProperties.getBot().getInitDataMaxAgeSeconds()) {
                throw ApiException.unauthorized("Сессия мессенджера устарела");
            }
        }

        String userJson = data.get("user");
        if (userJson == null) {
            throw ApiException.unauthorized("Нет пользователя мессенджера");
        }
        try {
            JsonNode node = objectMapper.readTree(userJson);
            String id = node.path("id").asText();
            if (id.isBlank()) {
                throw ApiException.unauthorized("Нет идентификатора мессенджера");
            }
            return new TelegramUser(
                    id,
                    textOrNull(node, "first_name"),
                    textOrNull(node, "username"));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.unauthorized("Некорректный пользователь мессенджера");
        }
    }

    private Map<String, String> parse(String initData) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : initData.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int index = pair.indexOf('=');
            String key = index >= 0 ? pair.substring(0, index) : pair;
            String value = index >= 0 ? pair.substring(index + 1) : "";
            result.put(
                    URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return result;
    }

    private byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC error", e);
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
