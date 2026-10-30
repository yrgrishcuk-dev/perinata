package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.AlarmType;
import ru.antenatal.domain.Appointment;
import ru.antenatal.domain.AppointmentStatus;
import ru.antenatal.domain.AppointmentType;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.PatientResponse;
import ru.antenatal.domain.PatientStatus;
import ru.antenatal.domain.Severity;
import ru.antenatal.repo.AppointmentRepository;
import ru.antenatal.repo.DoctorRepository;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.web.dto.AppointmentDtos.AppointmentAttentionView;
import ru.antenatal.web.dto.AppointmentDtos.AppointmentCreateRequest;
import ru.antenatal.web.dto.AppointmentDtos.AppointmentUpdateRequest;
import ru.antenatal.web.dto.AppointmentDtos.AppointmentView;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Duration REMINDER_LEAD = Duration.ofHours(24);
    private static final Duration ALLOWED_PAST = Duration.ofMinutes(5);

    private final AppointmentRepository appointmentRepository;
    private final PatientCardRepository patientCardRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAccessService doctorAccessService;
    private final NotificationService notificationService;
    private final AlertService alertService;
    private final AuditService auditService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public List<AppointmentView> listForDoctor(Doctor doctor, LocalDate from, LocalDate to) {
        ZoneId zone = zone();
        LocalDate today = LocalDate.now(zone);
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        LocalDate end = to != null ? to : start.withDayOfMonth(start.lengthOfMonth());
        if (start.isAfter(end)) {
            throw ApiException.badRequest("Начало периода позже окончания");
        }
        if (ChronoUnit.DAYS.between(start, end) > 366) {
            throw ApiException.badRequest("Слишком большой период");
        }

        Instant fromInstant = start.atStartOfDay(zone).toInstant();
        Instant toInstant = end.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndStartAtBetweenOrderByStartAtAsc(
                        doctor.getId(), fromInstant, toInstant);
        Map<Long, PatientCard> patients = patientCardRepository
                .findAllById(appointments.stream()
                        .map(Appointment::getPatientCardId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(PatientCard::getId, Function.identity()));

        return appointments.stream()
                .map(appointment -> view(appointment, patients.get(appointment.getPatientCardId()), doctor))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentView> listForPatient(PatientCard card) {
        List<Appointment> appointments = appointmentRepository
                .findByPatientCardIdOrderByStartAtDesc(card.getId());
        Map<Long, Doctor> doctors = doctorRepository
                .findAllById(appointments.stream()
                        .map(Appointment::getDoctorId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Doctor::getId, Function.identity()));
        return appointments.stream()
                .map(appointment -> view(appointment, card, doctors.get(appointment.getDoctorId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentView nextForPatient(PatientCard card) {
        return appointmentRepository
                .findFirstByPatientCardIdAndStatusAndStartAtAfterOrderByStartAtAsc(
                        card.getId(), AppointmentStatus.SCHEDULED, Instant.now())
                .map(appointment -> view(appointment, card,
                        doctorRepository.findById(appointment.getDoctorId()).orElse(null)))
                .orElse(null);
    }

    @Transactional
    public AppointmentView create(Doctor doctor, AppointmentCreateRequest request) {
        PatientCard patient = doctorAccessService.requirePatient(doctor, request.patientId());
        if (patient.getStatus() != PatientStatus.ACTIVE) {
            throw ApiException.badRequest("Наблюдение завершено");
        }
        Instant startAt = toInstant(request.date(), request.time());
        validateStart(startAt);

        Appointment appointment = new Appointment();
        appointment.setDoctorId(doctor.getId());
        appointment.setPatientCardId(patient.getId());
        appointment.setStartAt(startAt);
        appointment.setDurationMinutes(request.durationMinutes() == null ? 30 : request.durationMinutes());
        appointment.setType(request.type() == null ? AppointmentType.VISIT : request.type());
        appointment.setPlace(normalize(request.place()));
        appointment.setComment(normalize(request.comment()));
        Instant now = Instant.now();
        if (!startAt.isAfter(now.plus(REMINDER_LEAD))) {
            appointment.setReminderSentAt(now);
        }
        appointmentRepository.save(appointment);

        notifyCreated(appointment, patient);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "APPOINTMENT_CREATE", "APPOINTMENT", String.valueOf(appointment.getId()), null);
        return view(appointment, patient, doctor);
    }

    @Transactional
    public AppointmentView update(Doctor doctor, Long id, AppointmentUpdateRequest request) {
        Appointment appointment = requireOwned(doctor, id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw ApiException.badRequest("Приём уже завершён или отменён");
        }
        PatientCard patient = patientCardRepository.findById(appointment.getPatientCardId())
                .orElseThrow(() -> ApiException.notFound("Карточка не найдена"));

        Instant previousStart = appointment.getStartAt();
        Instant startAt = toInstant(request.date(), request.time());
        validateStart(startAt);
        boolean timeChanged = !startAt.equals(previousStart);

        appointment.setStartAt(startAt);
        appointment.setDurationMinutes(request.durationMinutes() == null ? 30 : request.durationMinutes());
        appointment.setType(request.type() == null ? AppointmentType.VISIT : request.type());
        appointment.setPlace(normalize(request.place()));
        appointment.setComment(normalize(request.comment()));
        touch(appointment);

        if (timeChanged) {
            appointment.setPatientResponse(PatientResponse.PENDING);
            if (startAt.isAfter(Instant.now().plus(REMINDER_LEAD))) {
                appointment.setReminderSentAt(null);
            } else {
                appointment.setReminderSentAt(Instant.now());
            }
            notifyChanged(appointment, patient);
        }
        appointmentRepository.save(appointment);

        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "APPOINTMENT_UPDATE", "APPOINTMENT", String.valueOf(id), null);
        return view(appointment, patient, doctor);
    }

    @Transactional
    public AppointmentView cancel(Doctor doctor, Long id) {
        Appointment appointment = requireOwned(doctor, id);
        PatientCard patient = patientCardRepository.findById(appointment.getPatientCardId())
                .orElseThrow(() -> ApiException.notFound("Карточка не найдена"));
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            return view(appointment, patient, doctor);
        }
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw ApiException.badRequest("Состоявшийся приём нельзя отменить");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        touch(appointment);
        appointmentRepository.save(appointment);
        notifyCancelled(appointment, patient);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "APPOINTMENT_CANCEL", "APPOINTMENT", String.valueOf(id), null);
        return view(appointment, patient, doctor);
    }

    @Transactional
    public AppointmentView complete(Doctor doctor, Long id) {
        return finish(doctor, id, AppointmentStatus.COMPLETED);
    }

    @Transactional
    public AppointmentView noShow(Doctor doctor, Long id) {
        return finish(doctor, id, AppointmentStatus.NO_SHOW);
    }

    @Transactional(readOnly = true)
    public AppointmentAttentionView attention(Doctor doctor) {
        ZoneId zone = zone();
        LocalDate today = LocalDate.now(zone);
        Instant from = today.atStartOfDay(zone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);
        List<Appointment> todayList = appointmentRepository
                .findByDoctorIdAndStartAtBetweenOrderByStartAtAsc(doctor.getId(), from, to);
        List<Appointment> pending = appointmentRepository
                .findByDoctorIdAndStatusAndStartAtBeforeOrderByStartAtAsc(
                        doctor.getId(), AppointmentStatus.SCHEDULED, Instant.now())
                .stream()
                .filter(appointment -> !appointment.endAt().isAfter(Instant.now()))
                .limit(20)
                .toList();
        Map<Long, PatientCard> patients = patientCardRepository
                .findAllById(todayList.stream()
                        .map(Appointment::getPatientCardId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(PatientCard::getId, Function.identity()));
        Map<Long, PatientCard> pendingPatients = patientCardRepository
                .findAllById(pending.stream()
                        .map(Appointment::getPatientCardId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(PatientCard::getId, Function.identity()));
        return new AppointmentAttentionView(
                todayList.stream()
                        .map(appointment -> view(appointment,
                                patients.get(appointment.getPatientCardId()), doctor))
                        .toList(),
                pending.stream()
                        .map(appointment -> view(appointment,
                                pendingPatients.get(appointment.getPatientCardId()), doctor))
                        .toList());
    }

    private AppointmentView finish(Doctor doctor, Long id, AppointmentStatus target) {
        Appointment appointment = requireOwned(doctor, id);
        if (appointment.getStatus() == target) {
            PatientCard patient = patientCardRepository.findById(appointment.getPatientCardId())
                    .orElseThrow(() -> ApiException.notFound("Карточка не найдена"));
            return view(appointment, patient, doctor);
        }
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw ApiException.badRequest("Приём уже завершён или отменён");
        }
        if (Instant.now().isBefore(appointment.endAt())) {
            throw ApiException.badRequest("Приём ещё не закончился");
        }
        PatientCard patient = patientCardRepository.findById(appointment.getPatientCardId())
                .orElseThrow(() -> ApiException.notFound("Карточка не найдена"));
        appointment.setStatus(target);
        touch(appointment);
        appointmentRepository.save(appointment);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                target == AppointmentStatus.COMPLETED ? "APPOINTMENT_COMPLETE" : "APPOINTMENT_NO_SHOW",
                "APPOINTMENT", String.valueOf(id), null);
        return view(appointment, patient, doctor);
    }

    @Transactional
    public AppointmentView respond(PatientCard card, Long id, PatientResponse response) {
        Appointment appointment = appointmentRepository.findByIdAndPatientCardId(id, card.getId())
                .orElseThrow(() -> ApiException.notFound("Приём не найден"));
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw ApiException.badRequest("Приём уже завершён или отменён");
        }
        if (!appointment.getStartAt().isAfter(Instant.now())) {
            throw ApiException.badRequest("Приём уже прошёл");
        }
        if (appointment.getPatientResponse() == response) {
            return view(appointment, card, doctorRepository.findById(appointment.getDoctorId()).orElse(null));
        }

        appointment.setPatientResponse(response);
        touch(appointment);
        appointmentRepository.save(appointment);

        String message = response == PatientResponse.CONFIRMED
                ? "Пациентка " + card.getPublicCode() + " подтвердила приём "
                + formatWhen(appointment.getStartAt()) + "."
                : "Пациентка " + card.getPublicCode() + " не сможет прийти на приём "
                + formatWhen(appointment.getStartAt()) + ".";
        alertService.raise(card, AlarmType.APPOINTMENT_RESPONSE, Severity.INFO,
                Map.of(
                        "appointmentId", appointment.getId(),
                        "response", response.name(),
                        "startAt", appointment.getStartAt().toString()),
                Duration.ofMinutes(1), message);
        auditService.log("PATIENT", String.valueOf(card.getId()),
                "APPOINTMENT_RESPONSE", "APPOINTMENT", String.valueOf(id), response.name());

        return view(appointment, card, doctorRepository.findById(appointment.getDoctorId()).orElse(null));
    }

    private Appointment requireOwned(Doctor doctor, Long id) {
        return appointmentRepository.findByIdAndDoctorId(id, doctor.getId())
                .orElseThrow(() -> ApiException.notFound("Приём не найден"));
    }

    private void touch(Appointment appointment) {
        appointment.setUpdatedAt(Instant.now());
        appointment.setSequence(appointment.getSequence() + 1);
    }

    private Instant toInstant(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, zone()).toInstant();
    }

    private void validateStart(Instant startAt) {
        Instant now = Instant.now();
        if (startAt.isBefore(now.minus(ALLOWED_PAST))) {
            throw ApiException.badRequest("Дата приёма уже прошла");
        }
        if (startAt.isAfter(now.plus(730, ChronoUnit.DAYS))) {
            throw ApiException.badRequest("Дата приёма слишком далеко");
        }
    }

    private void notifyCreated(Appointment appointment, PatientCard patient) {
        notificationService.notifyPatient(patient,
                "Вам назначен приём: " + formatWhen(appointment.getStartAt())
                        + placeSuffix(appointment) + ". Откройте приложение.",
                "appointment:created:" + appointment.getId() + ":" + appointment.getStartAt().toEpochMilli());
    }

    private void notifyChanged(Appointment appointment, PatientCard patient) {
        notificationService.notifyPatient(patient,
                "Приём перенесён: " + formatWhen(appointment.getStartAt())
                        + placeSuffix(appointment) + ". Откройте приложение.",
                "appointment:changed:" + appointment.getId() + ":" + appointment.getStartAt().toEpochMilli());
    }

    private void notifyCancelled(Appointment appointment, PatientCard patient) {
        notificationService.notifyPatient(patient,
                "Приём " + formatWhen(appointment.getStartAt())
                        + " отменён. Свяжитесь с врачом, если нужно перенести.",
                "appointment:cancelled:" + appointment.getId() + ":" + Instant.now().toEpochMilli());
    }

    private String placeSuffix(Appointment appointment) {
        return appointment.getPlace() == null ? "" : ", " + appointment.getPlace();
    }

    private String formatWhen(Instant startAt) {
        ZonedDateTime zoned = startAt.atZone(zone());
        return zoned.format(DATE_FORMAT) + " в " + zoned.format(TIME_FORMAT);
    }

    private ZoneId zone() {
        return ZoneId.of(appProperties.getClinicTimezone());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AppointmentView view(Appointment appointment, PatientCard patient, Doctor doctor) {
        ZonedDateTime zoned = appointment.getStartAt().atZone(zone());
        return new AppointmentView(
                appointment.getId(),
                appointment.getPatientCardId(),
                patient == null ? null : patient.getFullName(),
                patient == null ? null : patient.getPublicCode(),
                doctor == null ? null : doctor.getFullName(),
                appointment.getStartAt(),
                appointment.endAt(),
                zoned.toLocalDate(),
                zoned.toLocalTime(),
                zoned.toLocalTime().plusMinutes(appointment.getDurationMinutes()),
                appointment.getDurationMinutes(),
                appointment.getType(),
                appointment.getPlace(),
                appointment.getComment(),
                appointment.getStatus(),
                appointment.getPatientResponse(),
                appointment.getSequence(),
                zone().getId());
    }
}
