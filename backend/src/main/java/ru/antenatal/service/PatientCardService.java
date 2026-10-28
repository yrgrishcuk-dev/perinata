package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.calc.BmiCalculator;
import ru.antenatal.calc.GainCalculator;
import ru.antenatal.common.ApiException;
import ru.antenatal.common.TokenUtil;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.AlarmStatus;
import ru.antenatal.domain.BmiCategory;
import ru.antenatal.domain.DiabetesType;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PatientAccount;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.NotificationRecipientType;
import ru.antenatal.domain.PatientStatus;
import ru.antenatal.domain.PatientThreshold;
import ru.antenatal.domain.RiskGroup;
import ru.antenatal.domain.Role;
import ru.antenatal.repo.AlarmEventRepository;
import ru.antenatal.repo.DiaryEntryRepository;
import ru.antenatal.repo.DoctorMessengerRepository;
import ru.antenatal.repo.DoctorRepository;
import ru.antenatal.repo.LinkTokenRepository;
import ru.antenatal.repo.NotificationOutboxRepository;
import ru.antenatal.repo.PatientAccountRepository;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.repo.PatientThresholdRepository;
import ru.antenatal.repo.ThresholdDefaultRepository;
import ru.antenatal.web.dto.CommonDtos.GainView;
import ru.antenatal.web.dto.DoctorDtos.DashboardView;
import ru.antenatal.web.dto.DoctorDtos.PatientCreateRequest;
import ru.antenatal.web.dto.DoctorDtos.PatientDetails;
import ru.antenatal.web.dto.DoctorDtos.PatientSummary;
import ru.antenatal.web.dto.DoctorDtos.PatientUpdateRequest;
import ru.antenatal.web.dto.PatientDtos.OnboardingRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientCardService {

    private final PatientCardRepository patientCardRepository;
    private final PatientAccountRepository patientAccountRepository;
    private final LinkTokenRepository linkTokenRepository;
    private final PatientThresholdRepository patientThresholdRepository;
    private final ThresholdDefaultRepository thresholdDefaultRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final AlarmEventRepository alarmEventRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorMessengerRepository doctorMessengerRepository;
    private final DoctorAccessService doctorAccessService;
    private final GainService gainService;
    private final AuditService auditService;
    private final AppProperties appProperties;

    @Transactional
    public PatientSummary create(Doctor doctor, PatientCreateRequest request) {
        validateEdd(request.edd());
        PatientCard card = new PatientCard();
        card.setClinicId(doctor.getClinicId());
        card.setDoctorId(doctor.getId());
        card.setPublicCode(uniquePublicCode());
        card.setFullName(request.fullName().trim());
        card.setAgeYears(request.ageYears());
        card.setParity(request.parity());
        card.setEdd(request.edd());
        card.setHeightCm(request.heightCm());
        card.setPrePregnancyWeightKg(request.prePregnancyWeightKg());
        recalculateBmi(card);
        card.setRiskGroup(request.riskGroup() == null ? RiskGroup.LOW : request.riskGroup());
        card.setDiabetesType(request.diabetesType() == null ? DiabetesType.NONE : request.diabetesType());
        card.setDiaryRequired(request.diaryRequired() != null
                ? request.diaryRequired()
                : card.getRiskGroup() == RiskGroup.HIGH);
        card.setReminderTime(request.reminderTime() == null ? LocalTime.of(9, 0) : request.reminderTime());
        patientCardRepository.save(card);

        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "PATIENT_CREATE", "PATIENT_CARD", String.valueOf(card.getId()), null);
        return summary(card);
    }

    @Transactional
    public PatientSummary update(Doctor doctor, Long patientId, PatientUpdateRequest request) {
        PatientCard card = doctorAccessService.requirePatient(doctor, patientId);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            card.setFullName(request.fullName().trim());
        }
        if (request.ageYears() != null) {
            card.setAgeYears(request.ageYears());
        }
        if (request.parity() != null) {
            card.setParity(request.parity());
        }
        if (request.edd() != null) {
            validateEdd(request.edd());
            card.setEdd(request.edd());
        }
        if (request.heightCm() != null) {
            card.setHeightCm(request.heightCm());
        }
        if (request.prePregnancyWeightKg() != null) {
            card.setPrePregnancyWeightKg(request.prePregnancyWeightKg());
        }
        recalculateBmi(card);

        if (request.riskGroup() != null) {
            card.setRiskGroup(request.riskGroup());
            if (request.riskGroup() == RiskGroup.HIGH && request.diaryRequired() == null) {
                card.setDiaryRequired(true);
            }
        }
        if (request.diabetesType() != null) {
            card.setDiabetesType(request.diabetesType());
        }
        if (request.diaryRequired() != null) {
            card.setDiaryRequired(request.diaryRequired());
        }
        if (request.reminderTime() != null) {
            card.setReminderTime(request.reminderTime());
        }
        patientCardRepository.save(card);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "PATIENT_UPDATE", "PATIENT_CARD", String.valueOf(card.getId()), null);
        return summary(card);
    }

    @Transactional
    public void delete(Doctor doctor, Long patientId) {
        PatientCard card = doctorAccessService.requirePatient(doctor, patientId);
        notificationOutboxRepository.deleteByRecipientTypeAndRecipientId(
                NotificationRecipientType.PATIENT, card.getId());
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "PATIENT_DELETE", "PATIENT_CARD", String.valueOf(patientId), null);
        patientCardRepository.delete(card);
    }

    @Transactional
    public void close(Doctor doctor, Long patientId, String comment) {
        PatientCard card = doctorAccessService.requirePatient(doctor, patientId);
        card.setStatus(PatientStatus.CLOSED);
        card.setClosedAt(Instant.now());
        patientCardRepository.save(card);
        patientAccountRepository.findByPatientCardIdAndRevokedAtIsNull(patientId)
                .ifPresent(account -> {
                    account.setRevokedAt(Instant.now());
                    patientAccountRepository.save(account);
                });
        revokeLinkTokens(patientId);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "PATIENT_CLOSE", "PATIENT_CARD", String.valueOf(patientId), comment);
    }

    @Transactional
    public void revokeBinding(Doctor doctor, Long patientId) {
        PatientCard card = doctorAccessService.requirePatient(doctor, patientId);
        patientAccountRepository.findByPatientCardIdAndRevokedAtIsNull(card.getId())
                .ifPresent(account -> {
                    account.setRevokedAt(Instant.now());
                    patientAccountRepository.save(account);
                });
        revokeLinkTokens(card.getId());
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "BINDING_REVOKE", "PATIENT_CARD", String.valueOf(patientId), null);
    }

    @Transactional
    public PatientSummary updateOnboarding(PatientCard card, OnboardingRequest request) {
        card.setHeightCm(request.heightCm());
        card.setPrePregnancyWeightKg(request.prePregnancyWeightKg());
        if (request.reminderTime() != null) {
            card.setReminderTime(request.reminderTime());
        }
        recalculateBmi(card);
        patientCardRepository.save(card);
        auditService.log("PATIENT", String.valueOf(card.getId()),
                "ONBOARDING", "PATIENT_CARD", String.valueOf(card.getId()), null);
        return summary(card);
    }

    @Transactional(readOnly = true)
    public List<PatientSummary> list(Doctor doctor, String query) {
        List<PatientCard> cards = doctor.getRole() == Role.ADMIN
                ? patientCardRepository.findByClinicIdAndStatusOrderByCreatedAtDesc(
                        doctor.getClinicId(), PatientStatus.ACTIVE)
                : patientCardRepository.findByDoctorIdAndStatusOrderByCreatedAtDesc(
                        doctor.getId(), PatientStatus.ACTIVE);

        String normalized = query == null ? "" : query.trim().toLowerCase();
        return cards.stream()
                .filter(card -> normalized.isBlank()
                        || card.getFullName().toLowerCase().contains(normalized)
                        || card.getPublicCode().toLowerCase().contains(normalized))
                .map(this::summary)
                .toList();
    }

    @Transactional
    public PatientDetails details(Doctor doctor, Long patientId) {
        PatientCard card = doctorAccessService.requirePatient(doctor, patientId);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "PATIENT_VIEW", "PATIENT_CARD", String.valueOf(patientId), null);
        Doctor owner = doctorRepository.findById(card.getDoctorId()).orElse(null);
        PatientAccount account = patientAccountRepository
                .findByPatientCardIdAndRevokedAtIsNull(card.getId()).orElse(null);
        boolean messengerLinked = doctorMessengerRepository
                .findByDoctorIdAndRevokedAtIsNull(card.getDoctorId()).isPresent();
        GainView gain = gainService.gain(card);
        return new PatientDetails(
                summary(card),
                thresholds(card),
                gain,
                owner == null ? null : owner.getFullName(),
                account == null ? null : account.getPlatform().name(),
                account == null ? null : account.getConsentVersion(),
                account == null ? null : account.getConsentAt(),
                messengerLinked,
                card.getHeightCm(),
                card.getPrePregnancyWeightKg(),
                card.getReminderTime());
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> thresholds(PatientCard card) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        thresholdDefaultRepository.findAll()
                .forEach(item -> result.put(item.getCode(), item.getValue()));
        patientThresholdRepository.findByPatientCardId(card.getId())
                .forEach(item -> result.put(item.getCode(), item.getValue()));
        return result;
    }

    @Transactional
    public Map<String, BigDecimal> setThresholds(Doctor doctor, Long patientId,
                                                 Map<String, BigDecimal> values) {
        PatientCard card = doctorAccessService.requirePatient(doctor, patientId);
        Set<String> allowed = thresholdDefaultRepository.findAll().stream()
                .map(item -> item.getCode())
                .collect(Collectors.toSet());

        for (Map.Entry<String, BigDecimal> entry : values.entrySet()) {
            if (!allowed.contains(entry.getKey())) {
                throw ApiException.badRequest("Неизвестный порог: " + entry.getKey());
            }
            BigDecimal value = entry.getValue();
            if (value == null || value.compareTo(BigDecimal.ZERO) < 0
                    || value.compareTo(BigDecimal.valueOf(400)) > 0) {
                throw ApiException.badRequest("Недопустимое значение порога: " + entry.getKey());
            }
            PatientThreshold threshold = patientThresholdRepository
                    .findByPatientCardIdAndCode(card.getId(), entry.getKey())
                    .orElseGet(PatientThreshold::new);
            threshold.setPatientCardId(card.getId());
            threshold.setCode(entry.getKey());
            threshold.setValue(value);
            patientThresholdRepository.save(threshold);
        }
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "THRESHOLD_UPDATE", "PATIENT_CARD", String.valueOf(patientId), values.toString());
        return thresholds(card);
    }

    @Transactional(readOnly = true)
    public DashboardView dashboard(Doctor doctor) {
        List<PatientCard> cards = doctor.getRole() == Role.ADMIN
                ? patientCardRepository.findByClinicIdAndStatusOrderByCreatedAtDesc(
                        doctor.getClinicId(), PatientStatus.ACTIVE)
                : patientCardRepository.findByDoctorIdAndStatusOrderByCreatedAtDesc(
                        doctor.getId(), PatientStatus.ACTIVE);

        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        long openAlerts = 0;
        long diaryToday = 0;
        long highRisk = 0;
        for (PatientCard card : cards) {
            openAlerts += alarmEventRepository.countByPatientCardIdAndStatus(card.getId(), AlarmStatus.NEW);
            if (diaryEntryRepository.existsByPatientCardIdAndEntryDate(card.getId(), today)) {
                diaryToday++;
            }
            if (card.getRiskGroup() == RiskGroup.HIGH) {
                highRisk++;
            }
        }
        return new DashboardView(cards.size(), openAlerts, diaryToday, highRisk);
    }

    @Transactional(readOnly = true)
    public PatientSummary summary(PatientCard card) {
        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        GainCalculator.GestationalAge age =
                GainCalculator.gestationalAge(card.getEdd(), today);
        boolean bound = patientAccountRepository
                .findByPatientCardIdAndRevokedAtIsNull(card.getId()).isPresent();
        LocalDate lastDiary = diaryEntryRepository
                .findFirstByPatientCardIdOrderByEntryDateDesc(card.getId())
                .map(entry -> entry.getEntryDate())
                .orElse(null);
        long openAlerts = alarmEventRepository
                .countByPatientCardIdAndStatus(card.getId(), AlarmStatus.NEW);
        BmiCategory category = card.getBmiCategory();
        return new PatientSummary(
                card.getId(),
                card.getPublicCode(),
                card.getFullName(),
                card.getAgeYears(),
                card.getParity(),
                card.getEdd(),
                age.label(),
                age.weeks(),
                card.getBmi(),
                category,
                category == null ? null : category.getLabel(),
                card.getRiskGroup(),
                card.getDiabetesType(),
                card.isDiaryRequired(),
                card.getStatus(),
                bound,
                lastDiary,
                openAlerts,
                card.getCreatedAt());
    }

    private void revokeLinkTokens(Long patientCardId) {
        linkTokenRepository.findByPatientCardIdAndUsedAtIsNullAndRevokedAtIsNull(patientCardId)
                .forEach(token -> {
                    token.setRevokedAt(Instant.now());
                    linkTokenRepository.save(token);
                });
    }

    private void recalculateBmi(PatientCard card) {
        BigDecimal bmi = BmiCalculator.bmi(card.getHeightCm(), card.getPrePregnancyWeightKg());
        card.setBmi(bmi);
        card.setBmiCategory(BmiCalculator.category(bmi));
    }

    private void validateEdd(LocalDate edd) {
        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        if (edd.isBefore(today.minusDays(30)) || edd.isAfter(today.plusDays(300))) {
            throw ApiException.badRequest("ПДР вне допустимого диапазона");
        }
    }

    private String uniquePublicCode() {
        for (int i = 0; i < 20; i++) {
            String code = TokenUtil.publicCode();
            if (!patientCardRepository.existsByPublicCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Не удалось сгенерировать код карточки");
    }
}
