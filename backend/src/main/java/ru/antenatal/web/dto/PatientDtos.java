package ru.antenatal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.antenatal.domain.AbdominalPain;
import ru.antenatal.domain.DischargeAmount;
import ru.antenatal.domain.DischargeCharacter;
import ru.antenatal.domain.FetalMovements;
import ru.antenatal.domain.GlucoseContext;
import ru.antenatal.domain.Headache;
import ru.antenatal.domain.LegEdema;
import ru.antenatal.domain.Severity;
import ru.antenatal.domain.SleepQuality;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class PatientDtos {

    private PatientDtos() {
    }

    public record InvitePreviewResponse(
            String fullName,
            int ageYears,
            LocalDate edd,
            String doctorName,
            boolean alreadyBound,
            boolean alreadyBoundToThisCard
    ) {
    }

    public record InviteConfirmRequest(boolean consent) {
    }

    public record InviteConfirmResponse(boolean bound, DoctorDtos.PatientSummary patient) {
    }

    public record ContentBundle(
            int week,
            CommonDtos.WeekContentView content,
            CommonDtos.RationView ration
    ) {
    }

    public record DiaryView(
            long id,
            LocalDate date,
            Integer sbp,
            Integer dbp,
            Integer pulse,
            BigDecimal weightKg,
            BigDecimal glucoseMmol,
            GlucoseContext glucoseContext,
            String note,
            DischargeCharacter dischargeCharacter,
            DischargeAmount dischargeAmount,
            Boolean dischargeOdor,
            BigDecimal vaginalPh,
            FetalMovements fetalMovements,
            AbdominalPain lowerAbdominalPain,
            Integer painIntensity,
            LegEdema legEdema,
            SleepQuality sleepQuality,
            Headache headache,
            Instant kickStartedAt,
            Instant kickEndedAt,
            int kickCount,
            Integer kickDurationSeconds,
            Instant updatedAt
    ) {
    }

    public record DiarySaveRequest(
            LocalDate date,
            Integer sbp,
            Integer dbp,
            Integer pulse,
            BigDecimal weightKg,
            BigDecimal glucoseMmol,
            GlucoseContext glucoseContext,
            String note,
            DischargeCharacter dischargeCharacter,
            DischargeAmount dischargeAmount,
            Boolean dischargeOdor,
            BigDecimal vaginalPh,
            FetalMovements fetalMovements,
            AbdominalPain lowerAbdominalPain,
            Integer painIntensity,
            LegEdema legEdema,
            SleepQuality sleepQuality,
            Headache headache
    ) {
    }

    public record DiarySaveResponse(DiaryView entry, CommonDtos.AlertView alert, String message) {
    }

    public record SymptomReportRequest(@NotNull List<String> symptoms) {
    }

    public record SymptomReportResponse(
            boolean critical,
            String message,
            List<String> symptoms,
            Instant reportedAt
    ) {
    }

    public record ContractionAddRequest(@NotNull Instant startAt, @NotNull Instant endAt) {
    }

    public record ContractionPointView(
            long id,
            Instant startAt,
            Instant endAt,
            int durationSeconds
    ) {
    }

    public record ContractionStateResponse(
            Long sessionId,
            Instant startedAt,
            Instant endedAt,
            int countLastHour,
            Integer averageIntervalSeconds,
            Integer averageDurationSeconds,
            Severity severity,
            String message,
            List<ContractionPointView> contractions
    ) {
    }

    public record WatersResponse(boolean critical, String message) {
    }

    public record MessageSendRequest(@NotBlank @Size(max = 2000) String body) {
    }

    public record MessagesResponse(List<CommonDtos.MessageView> messages, long unread) {
    }

    public record ProfileResponse(
            DoctorDtos.PatientSummary summary,
            String doctorName,
            LocalTime reminderTime,
            java.math.BigDecimal heightCm,
            java.math.BigDecimal prePregnancyWeightKg,
            String consentVersion,
            Instant consentAt
    ) {
    }

    public record PatientMeResponse(boolean bound, PatientHomeResponse home) {
    }

    public record PatientHomeResponse(
            DoctorDtos.PatientSummary summary,
            CommonDtos.GainView gain,
            CommonDtos.WeekContentView week,
            CommonDtos.RationView ration,
            DiaryView todayDiary,
            long unreadMessages,
            String doctorName,
            AppointmentDtos.AppointmentView nextAppointment
    ) {
    }
}
