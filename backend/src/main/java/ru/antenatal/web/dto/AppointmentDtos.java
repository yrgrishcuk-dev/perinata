package ru.antenatal.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.antenatal.domain.AppointmentStatus;
import ru.antenatal.domain.AppointmentType;
import ru.antenatal.domain.PatientResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public final class AppointmentDtos {

    private AppointmentDtos() {
    }

    public record AppointmentCreateRequest(
            @NotNull Long patientId,
            @NotNull LocalDate date,
            @NotNull LocalTime time,
            @Min(10) @Max(240) Integer durationMinutes,
            @NotNull AppointmentType type,
            @Size(max = 255) String place,
            @Size(max = 2000) String comment
    ) {
    }

    public record AppointmentUpdateRequest(
            @NotNull LocalDate date,
            @NotNull LocalTime time,
            @Min(10) @Max(240) Integer durationMinutes,
            @NotNull AppointmentType type,
            @Size(max = 255) String place,
            @Size(max = 2000) String comment
    ) {
    }

    public record AppointmentView(
            long id,
            long patientId,
            String patientName,
            String patientPublicCode,
            String doctorName,
            Instant startAt,
            LocalDate date,
            LocalTime time,
            LocalTime endTime,
            int durationMinutes,
            AppointmentType type,
            String place,
            String comment,
            AppointmentStatus status,
            PatientResponse patientResponse,
            String timezone
    ) {
    }
}
