package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.antenatal.domain.NotificationOutbox;
import ru.antenatal.domain.NotificationStatus;
import ru.antenatal.domain.Platform;
import ru.antenatal.repo.NotificationOutboxRepository;
import ru.antenatal.telegram.TelegramClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final NotificationOutboxRepository outboxRepository;
    private final TelegramClient telegramClient;

    @Scheduled(fixedDelay = 10_000)
    public void process() {
        List<NotificationOutbox> items = outboxRepository
                .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
                        NotificationStatus.PENDING, Instant.now());
        for (NotificationOutbox item : items) {
            processOne(item);
        }
    }

    private void processOne(NotificationOutbox item) {
        if (item.getPlatform() != Platform.TELEGRAM) {
            item.setStatus(NotificationStatus.SKIPPED);
            item.setError("Platform not implemented");
            outboxRepository.save(item);
            return;
        }
        boolean sent = telegramClient.sendMessage(item.getChatId(), item.getText(), null);
        item.setAttempts(item.getAttempts() + 1);
        if (sent) {
            item.setStatus(NotificationStatus.SENT);
            item.setSentAt(Instant.now());
            item.setError(null);
        } else {
            if (item.getAttempts() >= 10) {
                item.setStatus(NotificationStatus.FAILED);
                item.setError("Max attempts reached");
            } else {
                long delaySeconds = Math.min(3600, 30L * (1L << Math.min(6, item.getAttempts())));
                item.setNextAttemptAt(Instant.now().plus(Duration.ofSeconds(delaySeconds)));
                item.setError("Send failed");
            }
        }
        outboxRepository.save(item);
    }
}
