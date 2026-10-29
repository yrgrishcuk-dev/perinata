package ru.antenatal.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;
import ru.antenatal.config.AppProperties;
import ru.antenatal.service.LinkService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PublicController {

    private final LinkService linkService;
    private final AppProperties appProperties;

    @GetMapping(value = "/l/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> landing(@PathVariable String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-store");
        headers.add("X-Robots-Tag", "noindex");

        boolean valid = linkService.findValid(token).isPresent();
        if (!valid) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .headers(headers)
                    .body(page("Ссылка недействительна",
                            "Попросите врача показать QR-код ещё раз.", ""));
        }

        String telegramUsername = appProperties.getBot().getUsername();
        String maxUsername = appProperties.getMax().getUsername();
        StringBuilder buttons = new StringBuilder();

        if (telegramUsername != null && !telegramUsername.isBlank()) {
            String url = "https://t.me/" + telegramUsername + "?start=p_" + token;
            buttons.append("<a class=\"primary\" href=\"")
                    .append(HtmlUtils.htmlEscape(url))
                    .append("\">Открыть в Telegram</a>");
        }
        if (maxUsername != null && !maxUsername.isBlank()) {
            String url = "https://max.ru/" + maxUsername + "?start=p_" + token;
            buttons.append("<a class=\"secondary\" href=\"")
                    .append(HtmlUtils.htmlEscape(url))
                    .append("\">Открыть в MAX</a>");
        }

        if (buttons.length() == 0) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .headers(headers)
                    .body(page("Мессенджеры не настроены",
                            "Обратитесь к администратору приложения.", ""));
        }

        return ResponseEntity.ok().headers(headers)
                .body(page("Приглашение от врача",
                        "Выберите мессенджер, чтобы подтвердить привязку карточки.",
                        buttons.toString()));
    }

    @GetMapping("/api/public/config")
    public Map<String, String> config() {
        return Map.of("telegramBotUsername", appProperties.getBot().getUsername());
    }

    private String page(String title, String subtitle, String buttonsHtml) {
        String safeTitle = HtmlUtils.htmlEscape(title);
        String safeSubtitle = HtmlUtils.htmlEscape(subtitle);
        return "<!doctype html><html lang=\"ru\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<meta name=\"robots\" content=\"noindex,nofollow\">"
                + "<title>" + safeTitle + "</title>"
                + "<style>body{font-family:system-ui,sans-serif;margin:0;padding:40px 24px;"
                + "background:#f6f7f9;color:#1c2430}main{max-width:420px;margin:0 auto;background:#fff;"
                + "border-radius:16px;padding:28px;box-shadow:0 8px 30px rgba(20,30,50,.08)}"
                + "a{display:block;text-align:center;margin-top:12px;padding:13px 18px;"
                + "border-radius:10px;text-decoration:none;font-weight:600}"
                + "a.primary{background:#2f6fed;color:#fff}"
                + "a.secondary{background:#eef2f7;color:#1c2430}"
                + "p{line-height:1.5;color:#4a5568}</style></head><body><main>"
                + "<h1 style=\"font-size:22px;margin:0 0 12px\">" + safeTitle + "</h1>"
                + "<p>" + safeSubtitle + "</p>"
                + buttonsHtml + "</main></body></html>";
    }
}
