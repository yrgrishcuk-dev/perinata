package ru.antenatal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public final class CalendarDtos {

    private CalendarDtos() {
    }

    public record CalendarEventRequest(
            @NotNull LocalDate date,
            @NotBlank @Size(max = 200) String title,
            LocalTime time,
            Long patientId,
            @Size(max = 1000) String note
    ) {
    }

    public record CalendarEventView(
            long id,
            LocalDate date,
            LocalTime time,
            String title,
            String note,
            Long patientId,
            String patientName,
            String patientPublicCode
    ) {
    }
}
