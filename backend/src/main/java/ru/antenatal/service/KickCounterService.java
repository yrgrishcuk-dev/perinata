package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.AlarmType;
import ru.antenatal.domain.DiaryEntry;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.Severity;
import ru.antenatal.repo.DiaryEntryRepository;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.web.dto.PatientDtos.DiaryView;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KickCounterService {

    private static final Duration SESSION_LENGTH = Duration.ofHours(2);

    private final DiaryEntryRepository diaryEntryRepository;
    private final PatientCardRepository patientCardRepository;
    private final DiaryService diaryService;
    private final AlertService alertService;
    private final AppProperties appProperties;

    @Transactional
    public DiaryView start(PatientCard card) {
        DiaryEntry entry = todayEntry(card);
        if (entry.getKickStartedAt() != null && entry.getKickEndedAt() == null) {
            throw ApiException.badRequest("Сеанс счётчика уже идёт");
        }
        entry.setKickStartedAt(Instant.now());
        entry.setKickEndedAt(null);
        entry.setKickCount(0);
        diaryEntryRepository.save(entry);
        return diaryService.view(entry);
    }

    @Transactional
    public DiaryView tap(PatientCard card) {
        DiaryEntry entry = todayEntry(card);
        if (entry.getKickStartedAt() == null || entry.getKickEndedAt() != null) {
            throw ApiException.badRequest("Сеанс счётчика не запущен");
        }
        entry.setKickCount(entry.getKickCount() + 1);
        diaryEntryRepository.save(entry);
        return diaryService.view(entry);
    }

    @Transactional
    public DiaryView finish(PatientCard card) {
        DiaryEntry entry = todayEntry(card);
        if (entry.getKickStartedAt() == null || entry.getKickEndedAt() != null) {
            throw ApiException.badRequest("Сеанс счётчика не запущен");
        }
        finishEntry(entry, false);
        return diaryService.view(entry);
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void finishExpired() {
        Instant now = Instant.now();
        List<DiaryEntry> active = diaryEntryRepository
                .findByKickStartedAtIsNotNullAndKickEndedAtIsNull();
        for (DiaryEntry entry : active) {
            if (!entry.getKickStartedAt().plus(SESSION_LENGTH).isAfter(now)) {
                finishEntry(entry, true);
            }
        }
    }

    private DiaryEntry todayEntry(PatientCard card) {
        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        return diaryEntryRepository.findByPatientCardIdAndEntryDate(card.getId(), today)
                .orElseGet(() -> {
                    DiaryEntry entry = new DiaryEntry();
                    entry.setPatientCardId(card.getId());
                    entry.setEntryDate(today);
                    return entry;
                });
    }

    private void finishEntry(DiaryEntry entry, boolean evaluate) {
        Instant ended = Instant.now();
        entry.setKickEndedAt(ended);
        diaryEntryRepository.save(entry);

        if (!evaluate) {
            long seconds = Duration.between(entry.getKickStartedAt(), ended).getSeconds();
            if (seconds < SESSION_LENGTH.getSeconds()) {
                return;
            }
        }

        if (entry.getKickCount() == 0) {
            alertService.raise(entryCard(entry), AlarmType.FETAL_MOVEMENTS, Severity.CRITICAL,
                    Map.of("kickCount", 0, "durationSeconds",
                            Duration.between(entry.getKickStartedAt(), ended).getSeconds()),
                    Duration.ofHours(23));
        } else if (entry.getKickCount() < 10) {
            alertService.raise(entryCard(entry), AlarmType.FETAL_MOVEMENTS, Severity.WARNING,
                    Map.of("kickCount", entry.getKickCount(), "durationSeconds",
                            Duration.between(entry.getKickStartedAt(), ended).getSeconds()),
                    Duration.ofHours(23));
        }
    }

    private PatientCard entryCard(DiaryEntry entry) {
        return patientCardRepository.findById(entry.getPatientCardId())
                .orElseThrow(() -> ApiException.notFound("Карточка не найдена"));
    }
}
