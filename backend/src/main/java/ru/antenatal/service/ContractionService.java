package ru.antenatal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.calc.ContractionEvaluator;
import ru.antenatal.calc.GainCalculator;
import ru.antenatal.common.ApiException;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.AlarmType;
import ru.antenatal.domain.Contraction;
import ru.antenatal.domain.ContractionSession;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.Severity;
import ru.antenatal.repo.ClinicalRuleRepository;
import ru.antenatal.repo.ContractionRepository;
import ru.antenatal.repo.ContractionSessionRepository;
import ru.antenatal.web.dto.PatientDtos.ContractionAddRequest;
import ru.antenatal.web.dto.PatientDtos.ContractionPointView;
import ru.antenatal.web.dto.PatientDtos.ContractionStateResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContractionService {

    private final ContractionSessionRepository sessionRepository;
    private final ContractionRepository contractionRepository;
    private final ClinicalRuleRepository clinicalRuleRepository;
    private final ObjectMapper objectMapper;
    private final AlertService alertService;
    private final AuditService auditService;
    private final AppProperties appProperties;

    @Transactional
    public ContractionStateResponse add(PatientCard card, ContractionAddRequest request) {
        Instant now = Instant.now();
        if (!request.endAt().isAfter(request.startAt())) {
            throw ApiException.badRequest("Окончание схватки должно быть позже начала");
        }
        if (request.startAt().isBefore(now.minus(Duration.ofHours(6)))) {
            throw ApiException.badRequest("Схватка слишком давно");
        }
        if (request.endAt().isAfter(now.plus(Duration.ofMinutes(2)))) {
            throw ApiException.badRequest("Время схватки в будущем");
        }
        int duration = (int) Duration.between(request.startAt(), request.endAt()).getSeconds();
        if (duration <= 0 || duration > 900) {
            throw ApiException.badRequest("Некорректная длительность схватки");
        }

        ContractionSession session = sessionRepository
                .findFirstByPatientCardIdAndEndedAtIsNullOrderByStartedAtDesc(card.getId())
                .orElseGet(() -> {
                    ContractionSession created = new ContractionSession();
                    created.setPatientCardId(card.getId());
                    created.setStartedAt(request.startAt());
                    return sessionRepository.save(created);
                });

        if (request.startAt().isBefore(session.getStartedAt())) {
            session.setStartedAt(request.startAt());
            sessionRepository.save(session);
        }

        Contraction contraction = new Contraction();
        contraction.setSessionId(session.getId());
        contraction.setStartAt(request.startAt());
        contraction.setEndAt(request.endAt());
        contraction.setDurationSeconds(duration);
        contractionRepository.save(contraction);

        ContractionStateResponse response = buildState(card, session);
        if (response.severity() == Severity.CRITICAL || response.severity() == Severity.WARNING) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("countLastHour", response.countLastHour());
            payload.put("averageIntervalSeconds", response.averageIntervalSeconds());
            payload.put("averageDurationSeconds", response.averageDurationSeconds());
            alertService.raise(card, AlarmType.CONTRACTION, response.severity(),
                    payload, Duration.ofMinutes(30));
        }
        auditService.log("PATIENT", String.valueOf(card.getId()),
                "CONTRACTION_ADD", "CONTRACTION", String.valueOf(contraction.getId()), null);
        return response;
    }

    @Transactional(readOnly = true)
    public ContractionStateResponse state(PatientCard card) {
        ContractionSession session = sessionRepository
                .findFirstByPatientCardIdAndEndedAtIsNullOrderByStartedAtDesc(card.getId())
                .orElse(null);
        if (session == null) {
            return new ContractionStateResponse(
                    null, null, null, 0, null, null, Severity.INFO,
                    "Нажмите «Началась схватка», чтобы начать наблюдение.", List.of());
        }
        return buildState(card, session);
    }

    @Transactional
    public void close(PatientCard card) {
        sessionRepository.findFirstByPatientCardIdAndEndedAtIsNullOrderByStartedAtDesc(card.getId())
                .ifPresent(session -> {
                    session.setEndedAt(Instant.now());
                    sessionRepository.save(session);
                    auditService.log("PATIENT", String.valueOf(card.getId()),
                            "CONTRACTION_CLOSE", "CONTRACTION_SESSION",
                            String.valueOf(session.getId()), null);
                });
    }

    @Transactional
    public ContractionStateResponse waters(PatientCard card) {
        alertService.raise(card, AlarmType.WATERS, Severity.CRITICAL,
                Map.of("reportedAt", Instant.now().toString()), Duration.ofMinutes(10));
        auditService.log("PATIENT", String.valueOf(card.getId()),
                "WATERS_REPORTED", "PATIENT_CARD", String.valueOf(card.getId()), null);
        return new ContractionStateResponse(
                null, null, null, 0, null, null, Severity.CRITICAL,
                "Отошли воды. Немедленно поезжайте в родильный дом или вызовите 103.",
                List.of());
    }

    private ContractionEvaluator.Parameters parameters() {
        ContractionEvaluator.Parameters defaults = ContractionEvaluator.Parameters.defaults();
        JsonNode preterm = rule("CONTRACTION_PRETERM");
        JsonNode warn = rule("CONTRACTION_TERM_WARN");
        JsonNode go = rule("CONTRACTION_TERM_GO");
        return new ContractionEvaluator.Parameters(
                preterm.path("weeksBefore").asInt(defaults.pretermWeeksBefore()),
                preterm.path("count").asInt(defaults.pretermCount()),
                preterm.path("windowMinutes").asInt(defaults.windowMinutes()),
                warn.path("count").asInt(defaults.termCount()),
                go.path("intervalMinutes").asInt(defaults.goIntervalMinutes()),
                go.path("durationSeconds").asInt(defaults.goDurationSeconds()),
                go.path("count").asInt(defaults.goCount()),
                preterm.path("message").asText(defaults.pretermMessage()),
                warn.path("message").asText(defaults.termWarnMessage()),
                go.path("message").asText(defaults.termGoMessage()));
    }

    private JsonNode rule(String code) {
        return clinicalRuleRepository.findFirstByCodeAndActiveTrueOrderByIdDesc(code)
                .map(rule -> {
                    try {
                        return objectMapper.readTree(rule.getParams());
                    } catch (Exception e) {
                        return objectMapper.createObjectNode();
                    }
                })
                .orElseGet(objectMapper::createObjectNode);
    }

    private ContractionStateResponse buildState(PatientCard card, ContractionSession session) {
        Instant now = Instant.now();
        List<Contraction> contractions = contractionRepository
                .findBySessionIdOrderByStartAtAsc(session.getId());
        List<ContractionEvaluator.Point> points = contractions.stream()
                .map(c -> new ContractionEvaluator.Point(c.getStartAt(), c.getEndAt()))
                .toList();

        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        int weeks = GainCalculator.gestationalAge(card.getEdd(), today).weeks();
        ContractionEvaluator.Assessment assessment =
                ContractionEvaluator.evaluate(weeks, points, now, parameters());

        List<ContractionPointView> views = contractions.stream()
                .map(c -> new ContractionPointView(c.getId(), c.getStartAt(), c.getEndAt(),
                        c.getDurationSeconds()))
                .toList();

        return new ContractionStateResponse(
                session.getId(),
                session.getStartedAt(),
                session.getEndedAt(),
                assessment.countLastHour(),
                assessment.averageIntervalSeconds(),
                assessment.averageDurationSeconds(),
                assessment.severity(),
                assessment.message(),
                views);
    }
}
