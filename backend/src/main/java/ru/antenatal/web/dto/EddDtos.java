package ru.antenatal.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ru.antenatal.domain.EddMethod;

import java.time.LocalDate;

public final class EddDtos {

    private EddDtos() {
    }

    public record EddCalcRequest(
            @NotNull LocalDate lmpDate,
            @Min(21) @Max(35) Integer cycleLength
    ) {
    }

    public record EddPreview(
            LocalDate edd,
            LocalDate lmpDate,
            int cycleLength,
            int gestationalWeek,
            EddMethod method
    ) {
    }
}
