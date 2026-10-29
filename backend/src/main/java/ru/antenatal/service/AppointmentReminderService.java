package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.Appointment;
import ru.antenatal.domain.AppointmentStatus;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.PatientResponse;
import ru.antenatal.domain.PatientStatus;
import ru.antenatal.repo.AppointmentRepository;
import ru.antenatal.repo.PatientCardRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentReminderService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final PatientCardRepository patientCardRepository;
    private final NotificationService notificationService;
    private final AppProperties appProperties;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void run() {
        Instant now = Instant.now();
        try {
            List<Appointment> due = appointmentRepository
                    .findByStatusAndReminderSentAtIsNullAndStartAtBetweenOrderByStartAtAsc(
                            AppointmentStatus.SCHEDULED, now, now.plus(Duration.ofHours(24)));
            for (Appointment appointment : due) {
                if (!appointment.getStartAt().isAfter(now)) {
                    continue;
                }
                if (appointment.getPatientResponse() != PatientResponse.DECLINED) {
                    patientCardRepository.findById(appointment.getPatientCardId())
                            .filter(card -> card.getStatus() == PatientStatus.ACTIVE)
                            .ifPresent(card -> notifyReminder(appointment, card));
                }
                appointment.setReminderSentAt(now);
                appointmentRepository.save(appointment);
            }
        } catch (RuntimeException e) {
            log.error("Appointment reminder run failed: {}", e.getClass().getName());
            log.debug("Appointment reminder details", e);
        }
    }

    private void notifyReminder(Appointment appointment, PatientCard card) {
        notificationService.notifyPatient(card,
                "Напоминание: " + formatWhen(appointment.getStartAt())
                        + " у вас приём" + placeSuffix(appointment) + ".",
                "appointment:reminder:" + appointment.getId() + ":"
                        + appointment.getStartAt().toEpochMilli());
    }

    private String placeSuffix(Appointment appointment) {
        return appointment.getPlace() == null ? "" : ", " + appointment.getPlace();
    }

    private String formatWhen(Instant startAt) {
        ZoneId zone = ZoneId.of(appProperties.getClinicTimezone());
        ZonedDateTime zoned = startAt.atZone(zone);
        return zoned.format(DATE_FORMAT) + " в " + zoned.format(TIME_FORMAT);
    }
}
