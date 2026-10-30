package ru.antenatal.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.antenatal.service.CalendarFeedService;

@RestController
@RequiredArgsConstructor
public class CalendarFeedController {

    private static final MediaType CALENDAR = MediaType.parseMediaType("text/calendar; charset=utf-8");

    private final CalendarFeedService calendarFeedService;

    @GetMapping("/api/calendar/feed/{token:.+}")
    public ResponseEntity<String> feed(@PathVariable String token) {
        String raw = token.endsWith(".ics") ? token.substring(0, token.length() - 4) : token;
        String body = calendarFeedService.feed(raw);
        return ResponseEntity.ok()
                .contentType(CALENDAR)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"appointments.ics\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store")
                .header("X-Robots-Tag", "noindex")
                .body(body);
    }
}
