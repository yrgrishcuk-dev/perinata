package ru.antenatal.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import ru.antenatal.domain.AlarmStatus;
import ru.antenatal.domain.AlarmType;
import ru.antenatal.domain.AuthorType;
import ru.antenatal.domain.MessageDirection;
import ru.antenatal.domain.Severity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class CommonDtos {

    private CommonDtos() {
    }

    public record GainPoint(
            LocalDate date,
            BigDecimal weightKg,
            int week,
            BigDecimal gainKg
    ) {
    }

    public record CorridorPoint(int week, BigDecimal minGainKg, BigDecimal maxGainKg) {
    }

    public record GainView(
            int week,
            int day,
            String ageLabel,
            BigDecimal minGainKg,
            BigDecimal maxGainKg,
            BigDecimal gainKg,
            String status,
            List<GainPoint> points,
            List<CorridorPoint> corridor
    ) {
    }

    public record WeekContentView(
            long id,
            int weekFrom,
            int weekTo,
            String version,
            String title,
            String fetalText,
            String nutritionText,
            String warningText
    ) {
    }

    public record RationView(
            long id,
            String category,
            String categoryLabel,
            String version,
            String title,
            int kcal,
            int proteinG,
            int fatG,
            int carbsG,
            JsonNode meals
    ) {
    }

    public record AlertView(
            long id,
            Long patientId,
            String patientName,
            String publicCode,
            AlarmType type,
            Severity severity,
            String payload,
            AlarmStatus status,
            String comment,
            Instant createdAt,
            Instant ackedAt
    ) {
    }

    public record MessageView(
            long id,
            MessageDirection direction,
            AuthorType authorType,
            String body,
            Instant createdAt,
            Instant readAt
    ) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
