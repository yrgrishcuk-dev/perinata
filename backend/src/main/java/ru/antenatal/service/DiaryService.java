package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.calc.GainCalculator;
import ru.antenatal.calc.VitalsEvaluator;
import ru.antenatal.common.ApiException;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.AbdominalPain;
import ru.antenatal.domain.AlarmEvent;
import ru.antenatal.domain.AlarmType;
import ru.antenatal.domain.DiabetesType;
import ru.antenatal.domain.DiaryEntry;
import ru.antenatal.domain.DischargeCharacter;
import ru.antenatal.domain.FetalMovements;
import ru.antenatal.domain.Headache;
import ru.antenatal.domain.LegEdema;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.Severity;
import ru.antenatal.repo.DiaryEntryRepository;
import ru.antenatal.web.dto.CommonDtos.AlertView;
import ru.antenatal.web.dto.CommonDtos.GainView;
import ru.antenatal.web.dto.PatientDtos.DiarySaveRequest;
import ru.antenatal.web.dto.PatientDtos.DiarySaveResponse;
import ru.antenatal.web.dto.PatientDtos.DiaryView;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryEntryRepository diaryEntryRepository;
    private final PatientCardService patientCardService;
    private final GainService gainService;
    private final AlertService alertService;
    private final AuditService auditService;
    private final AppProperties appProperties;

    @Transactional
    public DiarySaveResponse save(PatientCard card, DiarySaveRequest request) {
        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        LocalDate date = request.date() == null ? today : request.date();
        if (date.isAfter(today)) {
            throw ApiException.badRequest("Нельзя заполнить дневник на будущую дату");
        }
        if (date.isBefore(today.minusDays(366))) {
            throw ApiException.badRequest("Дата слишком давняя");
        }
        validate(request, card);

        DiaryEntry entry = diaryEntryRepository
                .findByPatientCardIdAndEntryDate(card.getId(), date)
                .orElseGet(DiaryEntry::new);
        entry.setPatientCardId(card.getId());
        entry.setEntryDate(date);
        entry.setSbp(request.sbp());
        entry.setDbp(request.dbp());
        entry.setPulse(request.pulse());
        entry.setWeightKg(request.weightKg());
        entry.setGlucoseMmol(request.glucoseMmol());
        entry.setGlucoseContext(request.glucoseContext());
        entry.setNote(request.note());
        entry.setDischargeCharacter(request.dischargeCharacter());
        if (request.dischargeCharacter() == null
                || request.dischargeCharacter() == DischargeCharacter.NONE) {
            entry.setDischargeAmount(null);
            entry.setDischargeOdor(null);
        } else {
            entry.setDischargeAmount(request.dischargeAmount());
            entry.setDischargeOdor(request.dischargeOdor());
        }
        entry.setVaginalPh(request.vaginalPh());
        entry.setFetalMovements(request.fetalMovements());
        entry.setLowerAbdominalPain(request.lowerAbdominalPain());
        entry.setPainIntensity(request.lowerAbdominalPain() == null
                || request.lowerAbdominalPain() == AbdominalPain.NONE
                ? null
                : request.painIntensity());
        entry.setLegEdema(request.legEdema());
        entry.setSleepQuality(request.sleepQuality());
        entry.setHeadache(request.headache());
        entry.setUpdatedAt(Instant.now());
        diaryEntryRepository.save(entry);

        Map<String, BigDecimal> thresholdMap = patientCardService.thresholds(card);
        VitalsEvaluator.Thresholds thresholds = VitalsEvaluator.fromMap(thresholdMap);

        VitalsEvaluator.VitalAlert bpAlert =
                VitalsEvaluator.evaluateBp(request.sbp(), request.dbp(), thresholds);
        VitalsEvaluator.VitalAlert glucoseAlert =
                VitalsEvaluator.evaluateGlucose(request.glucoseMmol(), request.glucoseContext(), thresholds);

        AlarmEvent topAlarm = null;
        String topMessage = null;
        Severity topSeverity = null;

        if (bpAlert != null) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sbp", request.sbp());
            payload.put("dbp", request.dbp());
            AlarmEvent alarm = alertService.raise(card, AlarmType.BP, bpAlert.severity(),
                    payload, Duration.ofMinutes(30));
            topAlarm = alarm;
            topMessage = bpAlert.message();
            topSeverity = bpAlert.severity();
        }
        if (glucoseAlert != null) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("glucose", request.glucoseMmol());
            payload.put("context", request.glucoseContext());
            AlarmEvent alarm = alertService.raise(card, AlarmType.GLUCOSE, glucoseAlert.severity(),
                    payload, Duration.ofMinutes(30));
            if (topSeverity == null || glucoseAlert.severity() == Severity.CRITICAL) {
                topAlarm = alarm;
                topMessage = glucoseAlert.message();
                topSeverity = glucoseAlert.severity();
            }
        }

        AlarmEvent weightAlarm = evaluateWeight(card, thresholdMap);
        if (weightAlarm != null && topAlarm == null) {
            topAlarm = weightAlarm;
            topMessage = "Прибавка веса выше коридора. Сообщите врачу и проверьте рацион.";
        }

        for (ObservationAlert observation : evaluateObservations(card, request, date)) {
            AlarmEvent alarm = alertService.raise(card, observation.type(), observation.severity(),
                    observation.payload(), Duration.ofHours(23));
            if (topSeverity == null || severityRank(observation.severity()) > severityRank(topSeverity)) {
                topAlarm = alarm;
                topMessage = observation.message();
                topSeverity = observation.severity();
            }
        }

        auditService.log("PATIENT", String.valueOf(card.getId()),
                "DIARY_SAVE", "DIARY_ENTRY", String.valueOf(entry.getId()), null);

        AlertView alertView = topAlarm == null ? null : alertService.viewFor(card, topAlarm);
        String message = topMessage == null ? "Дневник сохранён" : topMessage;
        return new DiarySaveResponse(view(entry), alertView, message);
    }

    @Transactional(readOnly = true)
    public List<DiaryView> history(PatientCard card, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        LocalDate start = from == null ? today.minusDays(90) : from;
        LocalDate end = to == null ? today : to;
        return diaryEntryRepository
                .findByPatientCardIdAndEntryDateBetweenOrderByEntryDateAsc(card.getId(), start, end)
                .stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public DiaryView today(PatientCard card) {
        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        return diaryEntryRepository.findByPatientCardIdAndEntryDate(card.getId(), today)
                .map(this::view).orElse(null);
    }

    private AlarmEvent evaluateWeight(PatientCard card, Map<String, BigDecimal> thresholdMap) {
        if (card.getPrePregnancyWeightKg() == null) {
            return null;
        }
        BigDecimal threshold = thresholdMap.getOrDefault("WEIGHT_GAIN_WARN", BigDecimal.valueOf(2));
        GainView gain = gainService.gain(card);

        boolean aboveCorridor = "ABOVE".equals(gain.status())
                && gain.gainKg() != null
                && gain.maxGainKg() != null
                && gain.gainKg().subtract(gain.maxGainKg()).compareTo(threshold) >= 0;

        boolean weeklyJump = false;
        BigDecimal weeklyChange = null;
        List<DiaryEntry> weights = diaryEntryRepository
                .findTop2ByPatientCardIdAndWeightKgIsNotNullOrderByEntryDateDesc(card.getId());
        if (weights.size() == 2) {
            DiaryEntry current = weights.get(0);
            DiaryEntry previous = weights.get(1);
            long days = ChronoUnit.DAYS.between(previous.getEntryDate(), current.getEntryDate());
            if (days > 0 && days <= 7) {
                weeklyChange = current.getWeightKg().subtract(previous.getWeightKg());
                weeklyJump = weeklyChange.compareTo(threshold) >= 0;
            }
        }

        if (!aboveCorridor && !weeklyJump) {
            return null;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("gainKg", gain.gainKg());
        payload.put("maxGainKg", gain.maxGainKg());
        payload.put("weeklyChangeKg", weeklyChange);
        return alertService.raise(card, AlarmType.WEIGHT, Severity.WARNING,
                payload, Duration.ofHours(23));
    }

    private List<ObservationAlert> evaluateObservations(PatientCard card,
                                                        DiarySaveRequest request,
                                                        LocalDate date) {
        java.util.List<ObservationAlert> alerts = new java.util.ArrayList<>();

        if (request.dischargeCharacter() == DischargeCharacter.BLOODY) {
            alerts.add(new ObservationAlert(AlarmType.DISCHARGE, Severity.CRITICAL,
                    "Кровянистые выделения. Свяжитесь с врачом или позвоните 103/112.",
                    Map.of("discharge", "BLOODY")));
        } else if (request.dischargeCharacter() == DischargeCharacter.WATERY) {
            alerts.add(new ObservationAlert(AlarmType.DISCHARGE, Severity.CRITICAL,
                    "Водянистые выделения. Возможно подтекание вод — свяжитесь с врачом.",
                    Map.of("discharge", "WATERY")));
        } else if (Boolean.TRUE.equals(request.dischargeOdor())) {
            alerts.add(new ObservationAlert(AlarmType.DISCHARGE, Severity.WARNING,
                    "Выделения с запахом. Обсудите это с врачом.",
                    Map.of("odor", true)));
        }

        if (request.vaginalPh() != null
                && request.vaginalPh().compareTo(BigDecimal.valueOf(5.0)) > 0) {
            alerts.add(new ObservationAlert(AlarmType.VAGINAL_PH, Severity.WARNING,
                    "pH влагалища выше 5.0. Обсудите результат с врачом.",
                    Map.of("ph", request.vaginalPh())));
        }

        int week = GainCalculator.gestationalAge(card.getEdd(), date).weeks();
        if (week >= 28 && request.fetalMovements() == FetalMovements.NONE) {
            alerts.add(new ObservationAlert(AlarmType.FETAL_MOVEMENTS, Severity.CRITICAL,
                    "Нет шевелений плода. Свяжитесь с врачом или позвоните 103/112.",
                    Map.of("movements", "NONE", "week", week)));
        } else if (week >= 28 && request.fetalMovements() == FetalMovements.LESS) {
            alerts.add(new ObservationAlert(AlarmType.FETAL_MOVEMENTS, Severity.WARNING,
                    "Шевеления плода меньше обычного. Обсудите это с врачом.",
                    Map.of("movements", "LESS", "week", week)));
        }

        if (request.lowerAbdominalPain() == AbdominalPain.CRAMPING) {
            alerts.add(new ObservationAlert(AlarmType.ABDOMINAL_PAIN, Severity.WARNING,
                    "Схваткообразные боли внизу живота. Обсудите это с врачом.",
                    Map.of("pain", "CRAMPING")));
        } else if (request.painIntensity() != null && request.painIntensity() >= 9) {
            alerts.add(new ObservationAlert(AlarmType.ABDOMINAL_PAIN, Severity.CRITICAL,
                    "Сильная боль внизу живота. Свяжитесь с врачом или позвоните 103/112.",
                    Map.of("pain", request.painIntensity())));
        } else if (request.painIntensity() != null && request.painIntensity() >= 7) {
            alerts.add(new ObservationAlert(AlarmType.ABDOMINAL_PAIN, Severity.WARNING,
                    "Выраженная боль внизу живота. Обсудите это с врачом.",
                    Map.of("pain", request.painIntensity())));
        }

        if (request.legEdema() == LegEdema.ASYMMETRIC) {
            alerts.add(new ObservationAlert(AlarmType.EDEMA, Severity.CRITICAL,
                    "Асимметричные отёки ног. Свяжитесь с врачом.",
                    Map.of("edema", "ASYMMETRIC")));
        } else if (request.legEdema() == LegEdema.PRONOUNCED) {
            alerts.add(new ObservationAlert(AlarmType.EDEMA, Severity.WARNING,
                    "Выраженные отёки ног. Обсудите это с врачом.",
                    Map.of("edema", "PRONOUNCED")));
        }

        if (request.headache() == Headache.MODERATE || request.headache() == Headache.SEVERE) {
            boolean highPressure = (request.sbp() != null && request.sbp() >= 140)
                    || (request.dbp() != null && request.dbp() >= 90);
            if (highPressure) {
                alerts.add(new ObservationAlert(AlarmType.HEADACHE, Severity.CRITICAL,
                        "Головная боль на фоне повышенного давления. Свяжитесь с врачом или позвоните 103/112.",
                        Map.of("headache", request.headache().name(),
                                "sbp", request.sbp() == null ? "" : String.valueOf(request.sbp()),
                                "dbp", request.dbp() == null ? "" : String.valueOf(request.dbp()))));
            } else if (request.headache() == Headache.SEVERE) {
                alerts.add(new ObservationAlert(AlarmType.HEADACHE, Severity.CRITICAL,
                        "Сильная головная боль. Свяжитесь с врачом или позвоните 103/112.",
                        Map.of("headache", "SEVERE")));
            } else {
                alerts.add(new ObservationAlert(AlarmType.HEADACHE, Severity.WARNING,
                        "Умеренная головная боль. Измерьте давление и обсудите это с врачом.",
                        Map.of("headache", "MODERATE")));
            }
        }

        return alerts;
    }

    private int severityRank(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 3;
            case WARNING -> 2;
            default -> 1;
        };
    }

    private record ObservationAlert(AlarmType type, Severity severity,
                                    String message, Map<String, Object> payload) {
    }

    public DiaryView view(DiaryEntry entry) {
        return new DiaryView(
                entry.getId(),
                entry.getEntryDate(),
                entry.getSbp(),
                entry.getDbp(),
                entry.getPulse(),
                entry.getWeightKg(),
                entry.getGlucoseMmol(),
                entry.getGlucoseContext(),
                entry.getNote(),
                entry.getDischargeCharacter(),
                entry.getDischargeAmount(),
                entry.getDischargeOdor(),
                entry.getVaginalPh(),
                entry.getFetalMovements(),
                entry.getLowerAbdominalPain(),
                entry.getPainIntensity(),
                entry.getLegEdema(),
                entry.getSleepQuality(),
                entry.getHeadache(),
                entry.getUpdatedAt());
    }

    private void validate(DiarySaveRequest request, PatientCard card) {
        if ((request.sbp() == null) != (request.dbp() == null)) {
            throw ApiException.badRequest("Укажите оба значения давления");
        }
        if (card.getDiabetesType() != DiabetesType.NONE && request.glucoseMmol() == null) {
            throw ApiException.badRequest("При сахарном диабете нужно указать уровень глюкозы");
        }
        if (request.sbp() != null && (request.sbp() < 60 || request.sbp() > 250)) {
            throw ApiException.badRequest("Систолическое давление вне диапазона");
        }
        if (request.dbp() != null && (request.dbp() < 30 || request.dbp() > 150)) {
            throw ApiException.badRequest("Диастолическое давление вне диапазона");
        }
        if (request.sbp() != null && request.dbp() != null && request.sbp() <= request.dbp()) {
            throw ApiException.badRequest("Систолическое давление должно быть выше диастолического");
        }
        if (request.pulse() != null && (request.pulse() < 30 || request.pulse() > 220)) {
            throw ApiException.badRequest("Пульс вне диапазона");
        }
        if (request.weightKg() != null
                && (request.weightKg().compareTo(BigDecimal.valueOf(30)) < 0
                || request.weightKg().compareTo(BigDecimal.valueOf(250)) > 0)) {
            throw ApiException.badRequest("Вес вне диапазона");
        }
        if (request.glucoseMmol() != null) {
            if (request.glucoseMmol().compareTo(BigDecimal.valueOf(2)) < 0
                    || request.glucoseMmol().compareTo(BigDecimal.valueOf(30)) > 0) {
                throw ApiException.badRequest("Глюкоза вне диапазона");
            }
            if (request.glucoseContext() == null) {
                throw ApiException.badRequest("Укажите контекст измерения глюкозы");
            }
        }
        if (request.vaginalPh() != null
                && (request.vaginalPh().compareTo(BigDecimal.valueOf(3.0)) < 0
                || request.vaginalPh().compareTo(BigDecimal.valueOf(7.0)) > 0)) {
            throw ApiException.badRequest("pH влагалища вне диапазона");
        }
        if (request.painIntensity() != null
                && (request.painIntensity() < 0 || request.painIntensity() > 10)) {
            throw ApiException.badRequest("Интенсивность боли вне диапазона");
        }
        if (request.lowerAbdominalPain() != null
                && request.lowerAbdominalPain() != AbdominalPain.NONE
                && request.painIntensity() == null) {
            throw ApiException.badRequest("Укажите интенсивность боли");
        }
    }
}
