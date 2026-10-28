package ru.antenatal.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.antenatal.domain.BmiCategory;
import ru.antenatal.domain.DiabetesType;
import ru.antenatal.domain.PatientStatus;
import ru.antenatal.domain.RiskGroup;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

public final class DoctorDtos {

    private DoctorDtos() {
    }

    public record PatientCreateRequest(
            @NotBlank @Size(max = 255) String fullName,
            @NotNull @Min(12) @Max(60) Integer ageYears,
            @Min(1) @Max(20) Integer parity,
            @NotNull LocalDate edd,
            @DecimalMin("100") @DecimalMax("250") BigDecimal heightCm,
            @DecimalMin("30") @DecimalMax("250") BigDecimal prePregnancyWeightKg,
            RiskGroup riskGroup,
            DiabetesType diabetesType,
            Boolean diaryRequired,
            LocalTime reminderTime
    ) {
    }

    public record PatientUpdateRequest(
            @Size(max = 255) String fullName,
            @Min(12) @Max(60) Integer ageYears,
            @Min(1) @Max(20) Integer parity,
            LocalDate edd,
            @DecimalMin("100") @DecimalMax("250") BigDecimal heightCm,
            @DecimalMin("30") @DecimalMax("250") BigDecimal prePregnancyWeightKg,
            RiskGroup riskGroup,
            DiabetesType diabetesType,
            Boolean diaryRequired,
            LocalTime reminderTime
    ) {
    }

    public record PatientSummary(
            long id,
            String publicCode,
            String fullName,
            int ageYears,
            Integer parity,
            LocalDate edd,
            String gestationalAge,
            int gestationalWeek,
            BigDecimal bmi,
            BmiCategory bmiCategory,
            String bmiCategoryLabel,
            RiskGroup riskGroup,
            DiabetesType diabetesType,
            boolean diaryRequired,
            PatientStatus status,
            boolean bound,
            LocalDate lastDiaryDate,
            long openAlerts,
            Instant createdAt
    ) {
    }

    public record PatientDetails(
            PatientSummary summary,
            Map<String, BigDecimal> thresholds,
            CommonDtos.GainView gain,
            String doctorName,
            String boundPlatform,
            String consentVersion,
            Instant consentAt,
            boolean hasMessengerNotifications,
            BigDecimal heightCm,
            BigDecimal prePregnancyWeightKg,
            java.time.LocalTime reminderTime
    ) {
    }

    public record LinkResponse(String url, Instant expiresAt) {
    }

    public record ThresholdUpdateRequest(@NotNull Map<String, BigDecimal> thresholds) {
    }

    public record CloseRequest(String comment) {
    }

    public record AlertActionRequest(String comment) {
    }

    public record DoctorCreateRequest(
            @NotBlank @Size(max = 255) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            String role
    ) {
    }

    public record DoctorUpdateRequest(
            @Size(max = 255) String fullName,
            String status,
            String role
    ) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record DoctorView(
            long id,
            String fullName,
            String email,
            String role,
            String status,
            boolean messengerLinked,
            Instant lastLoginAt,
            Instant createdAt
    ) {
    }

    public record DoctorMessengerResponse(String url, boolean linked) {
    }

    public record DashboardView(
            long patients,
            long openAlerts,
            long diaryToday,
            long highRisk
    ) {
    }
}
