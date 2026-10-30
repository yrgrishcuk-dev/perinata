package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.common.TokenUtil;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.Appointment;
import ru.antenatal.domain.AppointmentStatus;
import ru.antenatal.domain.AppointmentType;
import ru.antenatal.domain.CalendarFeedOwnerType;
import ru.antenatal.domain.CalendarFeedToken;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.PatientResponse;
import ru.antenatal.domain.PatientStatus;
import ru.antenatal.repo.AppointmentRepository;
import ru.antenatal.repo.CalendarFeedTokenRepository;
import ru.antenatal.repo.DoctorRepository;
import ru.antenatal.repo.PatientAccountRepository;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.web.dto.CalendarFeedDtos.CalendarFeedLink;
import ru.antenatal.web.dto.CalendarFeedDtos.CalendarFeedStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarFeedService {

    private static final DateTimeFormatter ICS_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final CalendarFeedTokenRepository calendarFeedTokenRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientCardRepository patientCardRepository;
    private final PatientAccountRepository patientAccountRepository;
    private final AppProperties appProperties;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public CalendarFeedStatus status(CalendarFeedOwnerType ownerType, Long ownerId) {
        return calendarFeedTokenRepository
                .findByOwnerTypeAndOwnerIdAndRevokedAtIsNull(ownerType, ownerId)
                .filter(token -> ownerActive(ownerType, ownerId))
                .map(token -> new CalendarFeedStatus(true, token.getCreatedAt()))
                .orElse(new CalendarFeedStatus(false, null));
    }

    @Transactional
    public CalendarFeedLink createOrRotate(CalendarFeedOwnerType ownerType, Long ownerId) {
        requireOwnerActive(ownerType, ownerId);
        calendarFeedTokenRepository
                .findByOwnerTypeAndOwnerIdAndRevokedAtIsNull(ownerType, ownerId)
                .ifPresent(old -> {
                    old.setRevokedAt(Instant.now());
                    calendarFeedTokenRepository.save(old);
                });

        String raw = TokenUtil.randomToken(32);
        CalendarFeedToken token = new CalendarFeedToken();
        token.setOwnerType(ownerType);
        token.setOwnerId(ownerId);
        token.setTokenHash(TokenUtil.sha256(raw));
        calendarFeedTokenRepository.save(token);

        String url = appProperties.getPublicBaseUrl()
                + "/api/calendar/feed/" + raw + ".ics";
        String webcalUrl = url.replaceFirst("^https?://", "webcal://");
        auditService.log(ownerType.name(), String.valueOf(ownerId),
                "CALENDAR_FEED_CREATE", "CALENDAR_FEED", String.valueOf(token.getId()), null);
        return new CalendarFeedLink(url, webcalUrl, token.getCreatedAt());
    }

    @Transactional
    public void revoke(CalendarFeedOwnerType ownerType, Long ownerId) {
        calendarFeedTokenRepository
                .findByOwnerTypeAndOwnerIdAndRevokedAtIsNull(ownerType, ownerId)
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    calendarFeedTokenRepository.save(token);
                    auditService.log(ownerType.name(), String.valueOf(ownerId),
                            "CALENDAR_FEED_REVOKE", "CALENDAR_FEED",
                            String.valueOf(token.getId()), null);
                });
    }

    @Transactional(readOnly = true)
    public String feed(String rawToken) {
        CalendarFeedToken token = calendarFeedTokenRepository
                .findByTokenHashAndRevokedAtIsNull(TokenUtil.sha256(rawToken))
                .orElseThrow(() -> ApiException.notFound("Подписка не найдена"));
        if (!ownerActive(token.getOwnerType(), token.getOwnerId())) {
            throw ApiException.notFound("Подписка не найдена");
        }

        ZoneId zone = ZoneId.of(appProperties.getClinicTimezone());
        LocalDate today = LocalDate.now(zone);
        Instant from = today.minusDays(30).atStartOfDay(zone).toInstant();
        Instant to = today.plusDays(365).atStartOfDay(zone).toInstant().minusNanos(1);

        if (token.getOwnerType() == CalendarFeedOwnerType.DOCTOR) {
            List<Appointment> appointments = appointmentRepository
                    .findByDoctorIdAndStartAtBetweenOrderByStartAtAsc(
                            token.getOwnerId(), from, to);
            Map<Long, PatientCard> patients = patientCardRepository
                    .findAllById(appointments.stream()
                            .map(Appointment::getPatientCardId)
                            .collect(Collectors.toSet()))
                    .stream()
                    .collect(Collectors.toMap(PatientCard::getId, Function.identity()));
            return buildCalendar("Приёмы врача", appointments,
                    appointment -> patients.get(appointment.getPatientCardId()), null, true);
        }

        List<Appointment> appointments = appointmentRepository
                .findByPatientCardIdAndStartAtBetweenOrderByStartAtAsc(
                        token.getOwnerId(), from, to);
        Map<Long, Doctor> doctors = doctorRepository
                .findAllById(appointments.stream()
                        .map(Appointment::getDoctorId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Doctor::getId, Function.identity()));
        return buildCalendar("Мои приёмы", appointments,
                appointment -> null, doctors, false);
    }

    private boolean ownerActive(CalendarFeedOwnerType ownerType, Long ownerId) {
        if (ownerType == CalendarFeedOwnerType.DOCTOR) {
            return doctorRepository.findById(ownerId)
                    .map(doctor -> "ACTIVE".equals(doctor.getStatus()))
                    .orElse(false);
        }
        return patientCardRepository.findById(ownerId)
                .map(card -> card.getStatus() == PatientStatus.ACTIVE)
                .orElse(false)
                && patientAccountRepository.findByPatientCardIdAndRevokedAtIsNull(ownerId).isPresent();
    }

    private void requireOwnerActive(CalendarFeedOwnerType ownerType, Long ownerId) {
        if (!ownerActive(ownerType, ownerId)) {
            throw ApiException.notFound("Владелец подписки не найден");
        }
    }

    private String buildCalendar(String calendarName, List<Appointment> appointments,
                                 Function<Appointment, PatientCard> patientLookup,
                                 Map<Long, Doctor> doctors, boolean forDoctor) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//Antenatal//RU");
        line(out, "CALSCALE:GREGORIAN");
        line(out, "METHOD:PUBLISH");
        line(out, "X-WR-CALNAME:" + escape(calendarName));
        line(out, "REFRESH-INTERVAL;VALUE=DURATION:PT6H");
        for (Appointment appointment : appointments) {
            PatientCard patient = patientLookup.apply(appointment);
            Doctor doctor = doctors == null ? null : doctors.get(appointment.getDoctorId());
            String type = typeLabel(appointment.getType());
            String summary = forDoctor
                    ? "Приём: " + (patient == null ? "пациентка" : patient.getFullName()) + " · " + type
                    : "Приём: " + type;
            if (appointment.getStatus() == AppointmentStatus.NO_SHOW) {
                summary += " (неявка)";
            }
            String description = description(appointment, patient, doctor, forDoctor);
            line(out, "BEGIN:VEVENT");
            line(out, "UID:appointment-" + appointment.getId() + "@perinata.ru");
            line(out, "DTSTAMP:" + ICS_DATE.format(Instant.now()));
            line(out, "LAST-MODIFIED:" + ICS_DATE.format(appointment.getUpdatedAt()));
            line(out, "DTSTART:" + ICS_DATE.format(appointment.getStartAt()));
            line(out, "DTEND:" + ICS_DATE.format(appointment.endAt()));
            line(out, "SEQUENCE:" + appointment.getSequence());
            line(out, "SUMMARY:" + escape(summary));
            if (appointment.getPlace() != null) {
                line(out, "LOCATION:" + escape(appointment.getPlace()));
            }
            if (!description.isBlank()) {
                line(out, "DESCRIPTION:" + escape(description));
            }
            line(out, "STATUS:" + icsStatus(appointment));
            line(out, "END:VEVENT");
        }
        line(out, "END:VCALENDAR");
        return out.toString();
    }

    private String description(Appointment appointment, PatientCard patient,
                               Doctor doctor, boolean forDoctor) {
        StringBuilder text = new StringBuilder();
        if (appointment.getComment() != null) {
            text.append(appointment.getComment());
        }
        if (forDoctor && patient != null) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append("Карточка: ").append(patient.getPublicCode());
        }
        if (!forDoctor && doctor != null) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append("Врач: ").append(doctor.getFullName());
        }
        if (text.length() > 0) {
            text.append('\n');
        }
        text.append("Антенатальный помощник");
        return text.toString();
    }

    private String icsStatus(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            return "CANCELLED";
        }
        if (appointment.getStatus() == AppointmentStatus.SCHEDULED
                && appointment.getPatientResponse() == PatientResponse.CONFIRMED) {
            return "CONFIRMED";
        }
        if (appointment.getStatus() == AppointmentStatus.SCHEDULED
                && appointment.getPatientResponse() == PatientResponse.PENDING) {
            return "TENTATIVE";
        }
        if (appointment.getStatus() == AppointmentStatus.SCHEDULED
                && appointment.getPatientResponse() == PatientResponse.DECLINED) {
            return "TENTATIVE";
        }
        return "CONFIRMED";
    }

    private String typeLabel(AppointmentType type) {
        return switch (type) {
            case VISIT -> "Приём";
            case ULTRASOUND -> "УЗИ";
            case TESTS -> "Анализы";
            case OTHER -> "Другое";
        };
    }

    private void line(StringBuilder out, String value) {
        out.append(fold(value)).append("\r\n");
    }

    private String fold(String value) {
        if (value.length() <= 73) {
            return value;
        }
        StringBuilder folded = new StringBuilder();
        int index = 0;
        while (index < value.length()) {
            int end = Math.min(index + 73, value.length());
            if (index > 0) {
                folded.append("\r\n ");
            }
            folded.append(value, index, end);
            index = end;
        }
        return folded.toString();
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace(";", "\\;")
                .replace(",", "\\,");
    }
}
