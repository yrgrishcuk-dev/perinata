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
        String username = appProperties.getBot().getUsername();

        if (!valid) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .headers(headers)
                    .body(page("Ссылка недействительна",
                            "Попросите врача показать QR-код ещё раз.", null));
        }
        if (username == null || username.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .headers(headers)
                    .body(page("Бот не настроен",
                            "Обратитесь к администратору приложения.", null));
        }

        String url = "https://t.me/" + username + "?start=p_" + token;
        String html = page("Приглашение от врача",
                "Откройте Telegram и подтвердите привязку карточки.",
                url);
        return ResponseEntity.ok().headers(headers).body(html);
    }

    private String page(String title, String subtitle, String redirectUrl) {
        String safeTitle = HtmlUtils.htmlEscape(title);
        String safeSubtitle = HtmlUtils.htmlEscape(subtitle);
        String redirect = redirectUrl == null ? ""
                : "<meta http-equiv=\"refresh\" content=\"0;url=" + HtmlUtils.htmlEscape(redirectUrl) + "\">";
        String button = redirectUrl == null ? ""
                : "<p><a href=\"" + HtmlUtils.htmlEscape(redirectUrl) + "\">Открыть Telegram</a></p>";
        return "<!doctype html><html lang=\"ru\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<meta name=\"robots\" content=\"noindex,nofollow\">"
                + redirect
                + "<title>" + safeTitle + "</title>"
                + "<style>body{font-family:system-ui,sans-serif;margin:0;padding:40px 24px;"
                + "background:#f6f7f9;color:#1c2430}main{max-width:420px;margin:0 auto;background:#fff;"
                + "border-radius:16px;padding:28px;box-shadow:0 8px 30px rgba(20,30,50,.08)}"
                + "a{display:inline-block;margin-top:12px;background:#2f6fed;color:#fff;"
                + "padding:12px 18px;border-radius:10px;text-decoration:none}</style></head><body><main>"
                + "<h1 style=\"font-size:22px;margin:0 0 12px\">" + safeTitle + "</h1>"
                + "<p style=\"line-height:1.5;color:#4a5568\">" + safeSubtitle + "</p>"
                + button + "</main></body></html>";
    }
}
