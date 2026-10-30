package ru.antenatal.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.antenatal.domain.AlarmStatus;
import ru.antenatal.domain.CalendarFeedOwnerType;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.Platform;
import ru.antenatal.security.CurrentPrincipal;
import ru.antenatal.service.AlertService;
import ru.antenatal.service.AppointmentService;
import ru.antenatal.service.AppointmentAnalyticsService;
import ru.antenatal.service.CalendarEventService;
import ru.antenatal.service.CalendarFeedService;
import ru.antenatal.service.DiaryService;
import ru.antenatal.service.DoctorAccessService;
import ru.antenatal.service.LinkService;
import ru.antenatal.service.MessageService;
import ru.antenatal.service.PatientCardService;
import ru.antenatal.service.SymptomService;
import ru.antenatal.web.dto.CommonDtos.AlertView;
import ru.antenatal.web.dto.CommonDtos.MessageView;
import ru.antenatal.web.dto.AppointmentDtos.AppointmentAttentionView;
import ru.antenatal.web.dto.AppointmentDtos.AppointmentCreateRequest;
import ru.antenatal.web.dto.AppointmentDtos.AppointmentUpdateRequest;
import ru.antenatal.web.dto.AppointmentDtos.AppointmentView;
import ru.antenatal.web.dto.AnalyticsDtos.AppointmentAnalyticsView;
import ru.antenatal.web.dto.CalendarDtos.CalendarEventRequest;
import ru.antenatal.web.dto.CalendarDtos.CalendarEventView;
import ru.antenatal.web.dto.CalendarFeedDtos.CalendarFeedLink;
import ru.antenatal.web.dto.CalendarFeedDtos.CalendarFeedStatus;
import ru.antenatal.web.dto.DoctorDtos.AlertActionRequest;
import ru.antenatal.web.dto.DoctorDtos.CloseRequest;
import ru.antenatal.web.dto.DoctorDtos.DashboardView;
import ru.antenatal.web.dto.DoctorDtos.DoctorMessengerResponse;
import ru.antenatal.web.dto.DoctorDtos.LinkResponse;
import ru.antenatal.web.dto.DoctorDtos.PatientCreateRequest;
import ru.antenatal.web.dto.DoctorDtos.PatientDetails;
import ru.antenatal.web.dto.DoctorDtos.PatientSummary;
import ru.antenatal.web.dto.DoctorDtos.PatientUpdateRequest;
import ru.antenatal.web.dto.DoctorDtos.ThresholdUpdateRequest;
import ru.antenatal.web.dto.PatientDtos.DiaryView;
import ru.antenatal.web.dto.PatientDtos.MessageSendRequest;
import ru.antenatal.web.dto.PatientDtos.MessagesResponse;
import ru.antenatal.web.dto.PatientDtos.SymptomReportResponse;
import ru.antenatal.web.dto.PatientDtos.ContractionStateResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorAccessService doctorAccessService;
    private final PatientCardService patientCardService;
    private final LinkService linkService;
    private final DiaryService diaryService;
    private final AlertService alertService;
    private final SymptomService symptomService;
    private final MessageService messageService;
    private final ru.antenatal.service.ContractionService contractionService;
    private final CalendarEventService calendarEventService;
    private final AppointmentService appointmentService;
    private final AppointmentAnalyticsService appointmentAnalyticsService;
    private final CalendarFeedService calendarFeedService;

    @GetMapping("/dashboard")
    public DashboardView dashboard(@AuthenticationPrincipal CurrentPrincipal principal) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return patientCardService.dashboard(doctor);
    }

    @GetMapping("/patients")
    public List<PatientSummary> patients(@AuthenticationPrincipal CurrentPrincipal principal,
                                         @RequestParam(required = false) String query) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return patientCardService.list(doctor, query);
    }

    @PostMapping("/patients")
    public PatientSummary create(@AuthenticationPrincipal CurrentPrincipal principal,
                                 @Valid @RequestBody PatientCreateRequest request) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return patientCardService.create(doctor, request);
    }

    @GetMapping("/patients/{id}")
    public PatientDetails patient(@AuthenticationPrincipal CurrentPrincipal principal,
                                  @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return patientCardService.details(doctor, id);
    }

    @PatchMapping("/patients/{id}")
    public PatientSummary update(@AuthenticationPrincipal CurrentPrincipal principal,
                                 @PathVariable Long id,
                                 @Valid @RequestBody PatientUpdateRequest request) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return patientCardService.update(doctor, id, request);
    }

    @DeleteMapping("/patients/{id}")
    public void delete(@AuthenticationPrincipal CurrentPrincipal principal,
                       @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        patientCardService.delete(doctor, id);
    }

    @PostMapping("/patients/{id}/close")
    public void close(@AuthenticationPrincipal CurrentPrincipal principal,
                      @PathVariable Long id,
                      @RequestBody(required = false) CloseRequest request) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        patientCardService.close(doctor, id, request == null ? null : request.comment());
    }

    @PostMapping("/patients/{id}/revoke-binding")
    public void revokeBinding(@AuthenticationPrincipal CurrentPrincipal principal,
                              @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        patientCardService.revokeBinding(doctor, id);
    }

    @PostMapping("/patients/{id}/link")
    public LinkResponse link(@AuthenticationPrincipal CurrentPrincipal principal,
                             @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return linkService.createPatientLink(doctor, id);
    }

    @PutMapping("/patients/{id}/thresholds")
    public Map<String, java.math.BigDecimal> thresholds(@AuthenticationPrincipal CurrentPrincipal principal,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody ThresholdUpdateRequest request) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return patientCardService.setThresholds(doctor, id, request.thresholds());
    }

    @GetMapping("/patients/{id}/diary")
    public List<DiaryView> diary(@AuthenticationPrincipal CurrentPrincipal principal,
                                 @PathVariable Long id,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        PatientCard card = doctorAccessService.requirePatient(doctor, id);
        return diaryService.history(card, from, to);
    }

    @GetMapping("/patients/{id}/alerts")
    public List<AlertView> patientAlerts(@AuthenticationPrincipal CurrentPrincipal principal,
                                         @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        PatientCard card = doctorAccessService.requirePatient(doctor, id);
        return alertService.forPatient(card);
    }

    @GetMapping("/patients/{id}/symptoms")
    public List<SymptomReportResponse> symptoms(@AuthenticationPrincipal CurrentPrincipal principal,
                                                @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        PatientCard card = doctorAccessService.requirePatient(doctor, id);
        return symptomService.history(card);
    }

    @GetMapping("/patients/{id}/contractions")
    public ContractionStateResponse contractions(@AuthenticationPrincipal CurrentPrincipal principal,
                                                 @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        PatientCard card = doctorAccessService.requirePatient(doctor, id);
        return contractionService.state(card);
    }

    @GetMapping("/patients/{id}/messages")
    public MessagesResponse messages(@AuthenticationPrincipal CurrentPrincipal principal,
                                     @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        PatientCard card = doctorAccessService.requirePatient(doctor, id);
        return messageService.forDoctor(card);
    }

    @PostMapping("/patients/{id}/messages")
    public MessagesResponse sendMessage(@AuthenticationPrincipal CurrentPrincipal principal,
                                        @PathVariable Long id,
                                        @Valid @RequestBody MessageSendRequest request) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        PatientCard card = doctorAccessService.requirePatient(doctor, id);
        return messageService.sendByDoctor(doctor, card, request);
    }

    @PostMapping("/patients/{id}/messages/read")
    public void readMessages(@AuthenticationPrincipal CurrentPrincipal principal,
                             @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        PatientCard card = doctorAccessService.requirePatient(doctor, id);
        messageService.markReadByDoctor(card);
    }

    @GetMapping("/alerts")
    public List<AlertView> alerts(@AuthenticationPrincipal CurrentPrincipal principal,
                                  @RequestParam(required = false) AlarmStatus status) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return alertService.forDoctor(doctor, status);
    }

    @PostMapping("/alerts/{id}/ack")
    public AlertView acknowledge(@AuthenticationPrincipal CurrentPrincipal principal,
                                 @PathVariable Long id,
                                 @RequestBody(required = false) AlertActionRequest request) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return alertService.acknowledge(doctor, id, request == null ? null : request.comment());
    }

    @PostMapping("/alerts/{id}/close")
    public AlertView closeAlert(@AuthenticationPrincipal CurrentPrincipal principal,
                                @PathVariable Long id,
                                @RequestBody(required = false) AlertActionRequest request) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return alertService.close(doctor, id, request == null ? null : request.comment());
    }

    @PostMapping("/messenger-link")
    public DoctorMessengerResponse messengerLink(@AuthenticationPrincipal CurrentPrincipal principal,
                                                 @RequestParam(required = false) Platform platform) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return linkService.createDoctorLink(doctor,
                platform == null ? Platform.TELEGRAM : platform);
    }

    @GetMapping("/calendar/events")
    public List<CalendarEventView> calendarEvents(
            @AuthenticationPrincipal CurrentPrincipal principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return calendarEventService.list(doctor, from, to);
    }

    @PostMapping("/calendar/events")
    public CalendarEventView createCalendarEvent(
            @AuthenticationPrincipal CurrentPrincipal principal,
            @Valid @RequestBody CalendarEventRequest request) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return calendarEventService.create(doctor, request);
    }

    @PutMapping("/calendar/events/{id}")
    public CalendarEventView updateCalendarEvent(
            @AuthenticationPrincipal CurrentPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CalendarEventRequest request) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return calendarEventService.update(doctor, id, request);
    }

    @DeleteMapping("/calendar/events/{id}")
    public void deleteCalendarEvent(@AuthenticationPrincipal CurrentPrincipal principal,
                                    @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        calendarEventService.delete(doctor, id);
    }

    @GetMapping("/appointments")
    public List<AppointmentView> appointments(
            @AuthenticationPrincipal CurrentPrincipal principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return appointmentService.listForDoctor(doctor, from, to);
    }

    @GetMapping("/appointments/attention")
    public AppointmentAttentionView appointmentAttention(
            @AuthenticationPrincipal CurrentPrincipal principal) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return appointmentService.attention(doctor);
    }

    @PostMapping("/appointments")
    public AppointmentView createAppointment(
            @AuthenticationPrincipal CurrentPrincipal principal,
            @Valid @RequestBody AppointmentCreateRequest request) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return appointmentService.create(doctor, request);
    }

    @PutMapping("/appointments/{id}")
    public AppointmentView updateAppointment(
            @AuthenticationPrincipal CurrentPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AppointmentUpdateRequest request) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return appointmentService.update(doctor, id, request);
    }

    @PostMapping("/appointments/{id}/cancel")
    public AppointmentView cancelAppointment(
            @AuthenticationPrincipal CurrentPrincipal principal,
            @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return appointmentService.cancel(doctor, id);
    }

    @PostMapping("/appointments/{id}/complete")
    public AppointmentView completeAppointment(
            @AuthenticationPrincipal CurrentPrincipal principal,
            @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return appointmentService.complete(doctor, id);
    }

    @PostMapping("/appointments/{id}/no-show")
    public AppointmentView noShowAppointment(
            @AuthenticationPrincipal CurrentPrincipal principal,
            @PathVariable Long id) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return appointmentService.noShow(doctor, id);
    }

    @GetMapping("/analytics/appointments")
    public AppointmentAnalyticsView appointmentAnalytics(
            @AuthenticationPrincipal CurrentPrincipal principal,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) Long doctorId) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return appointmentAnalyticsService.analytics(doctor, days, doctorId);
    }

    @GetMapping("/calendar/feed")
    public CalendarFeedStatus calendarFeedStatus(
            @AuthenticationPrincipal CurrentPrincipal principal) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return calendarFeedService.status(CalendarFeedOwnerType.DOCTOR, doctor.getId());
    }

    @PostMapping("/calendar/feed")
    public CalendarFeedLink createCalendarFeed(
            @AuthenticationPrincipal CurrentPrincipal principal) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        return calendarFeedService.createOrRotate(CalendarFeedOwnerType.DOCTOR, doctor.getId());
    }

    @DeleteMapping("/calendar/feed")
    public void revokeCalendarFeed(@AuthenticationPrincipal CurrentPrincipal principal) {
        Doctor doctor = doctorAccessService.requireDoctor(principal);
        calendarFeedService.revoke(CalendarFeedOwnerType.DOCTOR, doctor.getId());
    }
}
