package ru.antenatal.web.dto;

import java.time.Instant;

public final class CalendarFeedDtos {

    private CalendarFeedDtos() {
    }

    public record CalendarFeedStatus(boolean active, Instant createdAt) {
    }

    public record CalendarFeedLink(String url, String webcalUrl, Instant createdAt) {
    }
}
