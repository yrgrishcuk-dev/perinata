package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.domain.AlarmType;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.Severity;
import ru.antenatal.domain.SymptomReport;
import ru.antenatal.repo.SymptomReportRepository;
import ru.antenatal.web.dto.PatientDtos.SymptomReportRequest;
import ru.antenatal.web.dto.PatientDtos.SymptomReportResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SymptomService {

    private static final Set<String> ALLOWED = Set.of(
            "BLEEDING",
            "LIQUID_DISCHARGE",
            "HEADACHE_VISION",
            "SUDDEN_EDEMA",
            "FEVER",
            "ITCHING",
            "URINATION_PAIN",
            "ABDOMINAL_PAIN",
            "CONTRACTIONS"
    );

    private static final String RED_MESSAGE =
            "Немедленно обратитесь к врачу или вызовите 103/112. "
                    + "Не оставайтесь дома одна, возьмите документы и позвоните в приёмное отделение.";

    private final SymptomReportRepository symptomReportRepository;
    private final AlertService alertService;
    private final AuditService auditService;

    @Transactional
    public SymptomReportResponse report(PatientCard card, SymptomReportRequest request) {
        if (request.symptoms() == null) {
            throw ApiException.badRequest("Список симптомов обязателен");
        }
        Set<String> symptoms = new LinkedHashSet<>(request.symptoms());
        for (String symptom : symptoms) {
            if (!ALLOWED.contains(symptom)) {
                throw ApiException.badRequest("Неизвестный симптом: " + symptom);
            }
        }

        SymptomReport report = new SymptomReport();
        report.setPatientCardId(card.getId());
        report.setReportedAt(Instant.now());
        report.setAnyPositive(!symptoms.isEmpty());
        report.setSymptoms(String.join(",", symptoms));
        symptomReportRepository.save(report);

        if (!symptoms.isEmpty()) {
            alertService.raise(card, AlarmType.SYMPTOM, Severity.CRITICAL,
                    Map.of("symptoms", List.copyOf(symptoms)), Duration.ofMinutes(10));
        }
        auditService.log("PATIENT", String.valueOf(card.getId()),
                "SYMPTOM_REPORT", "SYMPTOM_REPORT", String.valueOf(report.getId()),
                "positive=" + symptoms.size());

        return new SymptomReportResponse(
                !symptoms.isEmpty(),
                symptoms.isEmpty()
                        ? "Симптомов не отмечено. Если самочувствие ухудшится — откройте анкету снова."
                        : RED_MESSAGE,
                List.copyOf(symptoms),
                report.getReportedAt());
    }

    @Transactional(readOnly = true)
    public List<SymptomReportResponse> history(PatientCard card) {
        return symptomReportRepository.findTop50ByPatientCardIdOrderByReportedAtDesc(card.getId())
                .stream()
                .map(report -> new SymptomReportResponse(
                        report.isAnyPositive(),
                        report.isAnyPositive() ? RED_MESSAGE : "Симптомов не отмечено.",
                        report.getSymptoms().isBlank()
                                ? List.of()
                                : List.of(report.getSymptoms().split(",")),
                        report.getReportedAt()))
                .toList();
    }
}
