package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.AlarmType;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.PatientStatus;
import ru.antenatal.domain.Severity;
import ru.antenatal.repo.DiaryEntryRepository;
import ru.antenatal.repo.PatientCardRepository;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final PatientCardRepository patientCardRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final NotificationService notificationService;
    private final AlertService alertService;
    private final AppProperties appProperties;

    @Scheduled(cron = "0 * * * * *")
    public void run() {
        try {
            ZoneId zone = ZoneId.of(appProperties.getClinicTimezone());
            ZonedDateTime now = ZonedDateTime.now(zone);
            LocalDate today = now.toLocalDate();
            List<PatientCard> cards = patientCardRepository.findByStatus(PatientStatus.ACTIVE);

            for (PatientCard card : cards) {
                sendDiaryReminder(card, now, today);
                sendMissedDiaryReminder(card, now, today);
                sendWeeklySymptomReminder(card, now, today);
                flagMissedDiary(card, now, today);
            }
        } catch (RuntimeException e) {
            log.error("Reminder run failed: {}", e.getClass().getName());
            log.debug("Reminder run details", e);
        }
    }

    private void sendDiaryReminder(PatientCard card, ZonedDateTime now, LocalDate today) {
        if (now.getHour() != card.getReminderTime().getHour()
                || now.getMinute() != card.getReminderTime().getMinute()) {
            return;
        }
        if (diaryEntryRepository.existsByPatientCardIdAndEntryDate(card.getId(), today)) {
            return;
        }
        notificationService.notifyPatient(card,
                "Время заполнить дневник. Откройте приложение.",
                "reminder:diary:" + card.getId() + ":" + today);
    }

    private void sendMissedDiaryReminder(PatientCard card, ZonedDateTime now, LocalDate today) {
        if (!card.isDiaryRequired()) {
            return;
        }
        if (now.getHour() != 21 || now.getMinute() != 0) {
            return;
        }
        if (card.getReminderTime().getHour() == 21 && card.getReminderTime().getMinute() == 0) {
            return;
        }
        if (diaryEntryRepository.existsByPatientCardIdAndEntryDate(card.getId(), today)) {
            return;
        }
        notificationService.notifyPatient(card,
                "Дневник за сегодня не заполнен. Откройте приложение и внесите данные.",
                "reminder:diary-missed:" + card.getId() + ":" + today);
    }

    private void sendWeeklySymptomReminder(PatientCard card, ZonedDateTime now, LocalDate today) {
        if (now.getDayOfWeek() != DayOfWeek.MONDAY
                || now.getHour() != 10
                || now.getMinute() != 0) {
            return;
        }
        notificationService.notifyPatient(card,
                "Не забудьте проверить самочувствие и заполнить анкету тревожных симптомов.",
                "reminder:symptoms:" + card.getId() + ":" + today);
    }

    private void flagMissedDiary(PatientCard card, ZonedDateTime now, LocalDate today) {
        if (!card.isDiaryRequired()) {
            return;
        }
        if (now.getHour() != 0 || now.getMinute() != 10) {
            return;
        }
        LocalDate yesterday = today.minusDays(1);
        if (diaryEntryRepository.existsByPatientCardIdAndEntryDate(card.getId(), yesterday)) {
            return;
        }
        alertService.raise(card, AlarmType.MISSED_DIARY, Severity.INFO,
                Map.of("date", yesterday.toString()), Duration.ofHours(23));
    }
}
