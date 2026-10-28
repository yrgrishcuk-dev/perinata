package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.antenatal.domain.NotificationOutbox;
import ru.antenatal.domain.NotificationRecipientType;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.Platform;
import ru.antenatal.repo.DoctorMessengerRepository;
import ru.antenatal.repo.NotificationOutboxRepository;
import ru.antenatal.repo.PatientAccountRepository;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationOutboxRepository outboxRepository;
    private final PatientAccountRepository patientAccountRepository;
    private final DoctorMessengerRepository doctorMessengerRepository;

    public void enqueue(Platform platform, NotificationRecipientType recipientType,
                        Long recipientId, String chatId, String text, String dedupeKey) {
        if (chatId == null || chatId.isBlank() || text == null || text.isBlank()) {
            return;
        }
        if (dedupeKey != null && outboxRepository.existsByDedupeKey(dedupeKey)) {
            return;
        }
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setPlatform(platform);
        outbox.setRecipientType(recipientType);
        outbox.setRecipientId(recipientId);
        outbox.setChatId(chatId);
        outbox.setText(text);
        outbox.setDedupeKey(dedupeKey);
        outboxRepository.save(outbox);
    }

    public void notifyDoctor(PatientCard card, String text, String dedupeKey) {
        doctorMessengerRepository.findByDoctorIdAndRevokedAtIsNull(card.getDoctorId())
                .ifPresent(link -> enqueue(link.getPlatform(), NotificationRecipientType.DOCTOR,
                        card.getDoctorId(), link.getChatId(), text, dedupeKey));
    }

    public void notifyPatient(PatientCard card, String text, String dedupeKey) {
        patientAccountRepository.findByPatientCardIdAndRevokedAtIsNull(card.getId())
                .ifPresent(account -> enqueue(account.getPlatform(), NotificationRecipientType.PATIENT,
                        card.getId(), account.getChatId(), text, dedupeKey));
    }
}
