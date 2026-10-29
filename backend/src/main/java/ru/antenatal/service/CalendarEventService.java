package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.CalendarEvent;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.repo.CalendarEventRepository;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.web.dto.CalendarDtos.CalendarEventRequest;
import ru.antenatal.web.dto.CalendarDtos.CalendarEventView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final PatientCardRepository patientCardRepository;
    private final DoctorAccessService doctorAccessService;
    private final AuditService auditService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public List<CalendarEventView> list(Doctor doctor, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        LocalDate end = to != null ? to : start.withDayOfMonth(start.lengthOfMonth());
        if (start.isAfter(end)) {
            throw ApiException.badRequest("Начало периода позже окончания");
        }
        if (ChronoUnit.DAYS.between(start, end) > 366) {
            throw ApiException.badRequest("Слишком большой период");
        }

        List<CalendarEvent> events = calendarEventRepository
                .findByDoctorIdAndEventDateBetweenOrderByEventDateAscEventTimeAsc(
                        doctor.getId(), start, end);
        Map<Long, PatientCard> patients = patientCardRepository
                .findAllById(events.stream()
                        .map(CalendarEvent::getPatientCardId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(PatientCard::getId, Function.identity()));

        return events.stream()
                .map(event -> view(event, patients.get(event.getPatientCardId())))
                .toList();
    }

    @Transactional
    public CalendarEventView create(Doctor doctor, CalendarEventRequest request) {
        PatientCard patient = resolvePatient(doctor, request.patientId());
        CalendarEvent event = new CalendarEvent();
        event.setDoctorId(doctor.getId());
        apply(event, request);
        calendarEventRepository.save(event);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "CALENDAR_EVENT_CREATE", "CALENDAR_EVENT", String.valueOf(event.getId()), null);
        return view(event, patient);
    }

    @Transactional
    public CalendarEventView update(Doctor doctor, Long id, CalendarEventRequest request) {
        CalendarEvent event = requireEvent(doctor, id);
        PatientCard patient = resolvePatient(doctor, request.patientId());
        apply(event, request);
        calendarEventRepository.save(event);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "CALENDAR_EVENT_UPDATE", "CALENDAR_EVENT", String.valueOf(id), null);
        return view(event, patient);
    }

    @Transactional
    public void delete(Doctor doctor, Long id) {
        CalendarEvent event = requireEvent(doctor, id);
        calendarEventRepository.delete(event);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "CALENDAR_EVENT_DELETE", "CALENDAR_EVENT", String.valueOf(id), null);
    }

    private CalendarEvent requireEvent(Doctor doctor, Long id) {
        return calendarEventRepository.findByIdAndDoctorId(id, doctor.getId())
                .orElseThrow(() -> ApiException.notFound("Событие не найдено"));
    }

    private PatientCard resolvePatient(Doctor doctor, Long patientId) {
        return patientId == null ? null : doctorAccessService.requirePatient(doctor, patientId);
    }

    private void apply(CalendarEvent event, CalendarEventRequest request) {
        event.setEventDate(request.date());
        event.setEventTime(request.time());
        event.setTitle(request.title().trim());
        event.setPatientCardId(request.patientId());
        event.setNote(normalizeNote(request.note()));
        event.setUpdatedAt(Instant.now());
    }

    private String normalizeNote(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CalendarEventView view(CalendarEvent event, PatientCard patient) {
        return new CalendarEventView(
                event.getId(),
                event.getEventDate(),
                event.getEventTime(),
                event.getTitle(),
                event.getNote(),
                event.getPatientCardId(),
                patient == null ? null : patient.getFullName(),
                patient == null ? null : patient.getPublicCode());
    }
}
