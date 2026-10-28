package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.domain.AuthorType;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.Message;
import ru.antenatal.domain.MessageDirection;
import ru.antenatal.domain.MessageThread;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.repo.MessageRepository;
import ru.antenatal.repo.MessageThreadRepository;
import ru.antenatal.web.dto.CommonDtos.MessageView;
import ru.antenatal.web.dto.PatientDtos.MessageSendRequest;
import ru.antenatal.web.dto.PatientDtos.MessagesResponse;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public MessagesResponse sendByDoctor(Doctor doctor, PatientCard card, MessageSendRequest request) {
        String body = normalize(request.body());
        MessageThread thread = thread(card);
        Message message = new Message();
        message.setThreadId(thread.getId());
        message.setDirection(MessageDirection.TO_PATIENT);
        message.setAuthorType(AuthorType.DOCTOR);
        message.setAuthorId(doctor.getId());
        message.setBody(body);
        messageRepository.save(message);

        notificationService.notifyPatient(card, "Новое сообщение от врача.",
                "message:" + message.getId() + ":patient");
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "MESSAGE_SEND", "MESSAGE", String.valueOf(message.getId()), null);
        return forDoctor(card);
    }

    @Transactional
    public MessagesResponse sendByPatient(PatientCard card, MessageSendRequest request) {
        String body = normalize(request.body());
        MessageThread thread = thread(card);
        Message message = new Message();
        message.setThreadId(thread.getId());
        message.setDirection(MessageDirection.TO_DOCTOR);
        message.setAuthorType(AuthorType.PATIENT);
        message.setAuthorId(card.getId());
        message.setBody(body);
        messageRepository.save(message);

        notificationService.notifyDoctor(card,
                "Новое сообщение от пациентки по карточке " + card.getPublicCode() + ".",
                "message:" + message.getId() + ":doctor");
        auditService.log("PATIENT", String.valueOf(card.getId()),
                "MESSAGE_SEND", "MESSAGE", String.valueOf(message.getId()), null);
        return forPatient(card);
    }

    @Transactional(readOnly = true)
    public MessagesResponse forDoctor(PatientCard card) {
        MessageThread thread = threadRepository.findByPatientCardId(card.getId()).orElse(null);
        if (thread == null) {
            return new MessagesResponse(List.of(), 0);
        }
        List<MessageView> messages = messageRepository
                .findByThreadIdOrderByCreatedAtAsc(thread.getId())
                .stream().map(this::view).toList();
        long unread = messageRepository
                .countByThreadIdAndDirectionAndReadAtIsNull(thread.getId(), MessageDirection.TO_DOCTOR);
        return new MessagesResponse(messages, unread);
    }

    @Transactional(readOnly = true)
    public MessagesResponse forPatient(PatientCard card) {
        MessageThread thread = threadRepository.findByPatientCardId(card.getId()).orElse(null);
        if (thread == null) {
            return new MessagesResponse(List.of(), 0);
        }
        List<MessageView> messages = messageRepository
                .findByThreadIdOrderByCreatedAtAsc(thread.getId())
                .stream().map(this::view).toList();
        long unread = messageRepository
                .countByThreadIdAndDirectionAndReadAtIsNull(thread.getId(), MessageDirection.TO_PATIENT);
        return new MessagesResponse(messages, unread);
    }

    @Transactional
    public void markReadByDoctor(PatientCard card) {
        markRead(card, MessageDirection.TO_DOCTOR);
    }

    @Transactional
    public void markReadByPatient(PatientCard card) {
        markRead(card, MessageDirection.TO_PATIENT);
    }

    private void markRead(PatientCard card, MessageDirection direction) {
        MessageThread thread = threadRepository.findByPatientCardId(card.getId()).orElse(null);
        if (thread == null) {
            return;
        }
        List<Message> unread = messageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId())
                .stream()
                .filter(message -> message.getDirection() == direction)
                .filter(message -> message.getReadAt() == null)
                .toList();
        Instant now = Instant.now();
        for (Message message : unread) {
            message.setReadAt(now);
            messageRepository.save(message);
        }
    }

    private MessageThread thread(PatientCard card) {
        return threadRepository.findByPatientCardId(card.getId())
                .orElseGet(() -> {
                    MessageThread created = new MessageThread();
                    created.setPatientCardId(card.getId());
                    return threadRepository.save(created);
                });
    }

    private String normalize(String body) {
        if (body == null || body.isBlank()) {
            throw ApiException.badRequest("Сообщение не может быть пустым");
        }
        String trimmed = body.trim();
        if (trimmed.length() > 2000) {
            throw ApiException.badRequest("Сообщение слишком длинное");
        }
        return trimmed;
    }

    private MessageView view(Message message) {
        return new MessageView(
                message.getId(),
                message.getDirection(),
                message.getAuthorType(),
                message.getBody(),
                message.getCreatedAt(),
                message.getReadAt());
    }
}
