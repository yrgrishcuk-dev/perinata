package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.common.JsonUtil;
import ru.antenatal.domain.AlarmEvent;
import ru.antenatal.domain.AlarmStatus;
import ru.antenatal.domain.AlarmType;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.Role;
import ru.antenatal.domain.Severity;
import ru.antenatal.repo.AlarmEventRepository;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.security.CurrentPrincipal;
import ru.antenatal.web.dto.CommonDtos.AlertView;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlarmEventRepository alarmEventRepository;
    private final PatientCardRepository patientCardRepository;
    private final NotificationService notificationService;
    private final JsonUtil jsonUtil;
    private final AuditService auditService;
    private final DoctorAccessService doctorAccessService;

    @Transactional
    public AlarmEvent raise(PatientCard card, AlarmType type, Severity severity,
                            Object payload, Duration dedupeWindow) {
        return raise(card, type, severity, payload, dedupeWindow, null);
    }

    @Transactional
    public AlarmEvent raise(PatientCard card, AlarmType type, Severity severity,
                            Object payload, Duration dedupeWindow, String doctorMessage) {
        Instant after = Instant.now().minus(dedupeWindow);
        List<AlarmStatus> dedupeStatuses = type == AlarmType.MISSED_DIARY
                ? List.of(AlarmStatus.NEW, AlarmStatus.ACKED, AlarmStatus.CLOSED)
                : List.of(AlarmStatus.NEW, AlarmStatus.ACKED);
        AlarmEvent existing = type == AlarmType.APPOINTMENT_RESPONSE
                ? null
                : alarmEventRepository
                .findFirstByPatientCardIdAndTypeAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
                        card.getId(), type, dedupeStatuses, after)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        AlarmEvent alarm = new AlarmEvent();
        alarm.setPatientCardId(card.getId());
        alarm.setType(type);
        alarm.setSeverity(severity);
        alarm.setPayload(payload == null ? null : jsonUtil.write(payload));
        alarmEventRepository.save(alarm);

        String prefix = severity == Severity.CRITICAL
                ? "Тревожное событие"
                : severity == Severity.WARNING ? "Событие" : "Пометка";
        if (type != AlarmType.MISSED_DIARY) {
            String message = doctorMessage != null
                    ? doctorMessage
                    : prefix + " по карточке " + card.getPublicCode() + ". Откройте кабинет.";
            notificationService.notifyDoctor(card, message, "alert:" + alarm.getId() + ":doctor");
        }

        auditService.log("SYSTEM", null, "ALARM_CREATE", "ALARM",
                String.valueOf(alarm.getId()), "type=" + type + ",severity=" + severity);
        return alarm;
    }

    @Transactional
    public AlertView acknowledge(Doctor doctor, Long alarmId, String comment) {
        AlarmEvent alarm = requireAlarm(doctor, alarmId);
        if (alarm.getStatus() == AlarmStatus.CLOSED) {
            throw ApiException.badRequest("Событие уже закрыто");
        }
        alarm.setStatus(AlarmStatus.ACKED);
        alarm.setAckedBy(doctor.getId());
        alarm.setAckedAt(Instant.now());
        if (comment != null && !comment.isBlank()) {
            alarm.setComment(comment.trim());
        }
        alarmEventRepository.save(alarm);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "ALARM_ACK", "ALARM", String.valueOf(alarmId), comment);
        return view(alarm, patientCardRepository.findById(alarm.getPatientCardId()).orElse(null));
    }

    @Transactional
    public AlertView close(Doctor doctor, Long alarmId, String comment) {
        AlarmEvent alarm = requireAlarm(doctor, alarmId);
        alarm.setStatus(AlarmStatus.CLOSED);
        if (alarm.getAckedAt() == null) {
            alarm.setAckedBy(doctor.getId());
            alarm.setAckedAt(Instant.now());
        }
        if (comment != null && !comment.isBlank()) {
            alarm.setComment(comment.trim());
        }
        alarmEventRepository.save(alarm);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "ALARM_CLOSE", "ALARM", String.valueOf(alarmId), comment);
        return view(alarm, patientCardRepository.findById(alarm.getPatientCardId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<AlertView> forDoctor(Doctor doctor, AlarmStatus status) {
        List<PatientCard> cards = doctor.getRole() == Role.ADMIN
                ? patientCardRepository.findByClinicIdAndStatusOrderByCreatedAtDesc(
                        doctor.getClinicId(), ru.antenatal.domain.PatientStatus.ACTIVE)
                : patientCardRepository.findByDoctorIdAndStatusOrderByCreatedAtDesc(
                        doctor.getId(), ru.antenatal.domain.PatientStatus.ACTIVE);

        List<AlertView> result = new ArrayList<>();
        for (PatientCard card : cards) {
            for (AlarmEvent alarm : alarmEventRepository
                    .findByPatientCardIdOrderByCreatedAtDesc(card.getId())) {
                if (status == null || alarm.getStatus() == status) {
                    result.add(view(alarm, card));
                }
            }
        }
        result.sort(Comparator.comparing(AlertView::createdAt).reversed());
        return result.size() > 200 ? result.subList(0, 200) : result;
    }

    @Transactional(readOnly = true)
    public List<AlertView> forPatient(PatientCard card) {
        return alarmEventRepository.findByPatientCardIdOrderByCreatedAtDesc(card.getId())
                .stream()
                .limit(50)
                .map(alarm -> view(alarm, card))
                .toList();
    }

    public AlertView viewFor(PatientCard card, AlarmEvent alarm) {
        return view(alarm, card);
    }

    private AlarmEvent requireAlarm(Doctor doctor, Long alarmId) {
        AlarmEvent alarm = alarmEventRepository.findById(alarmId)
                .orElseThrow(() -> ApiException.notFound("Событие не найдено"));
        PatientCard card = patientCardRepository.findById(alarm.getPatientCardId())
                .orElseThrow(() -> ApiException.notFound("Пациентка не найдена"));
        doctorAccessService.requirePatient(doctor, card.getId());
        return alarm;
    }

    private AlertView view(AlarmEvent alarm, PatientCard card) {
        return new AlertView(
                alarm.getId(),
                alarm.getPatientCardId(),
                card == null ? null : card.getFullName(),
                card == null ? null : card.getPublicCode(),
                alarm.getType(),
                alarm.getSeverity(),
                alarm.getPayload(),
                alarm.getStatus(),
                alarm.getComment(),
                alarm.getCreatedAt(),
                alarm.getAckedAt());
    }
}
