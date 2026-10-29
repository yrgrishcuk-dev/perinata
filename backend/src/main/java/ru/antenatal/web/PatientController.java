package ru.antenatal.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PatientAccount;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.PatientStatus;
import ru.antenatal.domain.PatientResponse;
import ru.antenatal.domain.Platform;
import ru.antenatal.repo.DoctorRepository;
import ru.antenatal.repo.PatientAccountRepository;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.security.CurrentPrincipal;
import ru.antenatal.service.AlertService;
import ru.antenatal.service.AppointmentService;
import ru.antenatal.service.ContentService;
import ru.antenatal.service.ContractionService;
import ru.antenatal.service.DiaryService;
import ru.antenatal.service.GainService;
import ru.antenatal.service.LinkService;
import ru.antenatal.service.MessageService;
import ru.antenatal.service.PatientAccessService;
import ru.antenatal.service.PatientCardService;
import ru.antenatal.service.SymptomService;
import ru.antenatal.web.dto.CommonDtos.AlertView;
import ru.antenatal.web.dto.CommonDtos.GainView;
import ru.antenatal.web.dto.CommonDtos.WeekContentView;
import ru.antenatal.web.dto.AppointmentDtos.AppointmentView;
import ru.antenatal.web.dto.PatientDtos.ContentBundle;
import ru.antenatal.web.dto.PatientDtos.ContractionAddRequest;
import ru.antenatal.web.dto.PatientDtos.ContractionStateResponse;
import ru.antenatal.web.dto.PatientDtos.DiarySaveRequest;
import ru.antenatal.web.dto.PatientDtos.DiarySaveResponse;
import ru.antenatal.web.dto.PatientDtos.DiaryView;
import ru.antenatal.web.dto.PatientDtos.InviteConfirmRequest;
import ru.antenatal.web.dto.PatientDtos.InviteConfirmResponse;
import ru.antenatal.web.dto.PatientDtos.InvitePreviewResponse;
import ru.antenatal.web.dto.PatientDtos.MessageSendRequest;
import ru.antenatal.web.dto.PatientDtos.MessagesResponse;
import ru.antenatal.web.dto.PatientDtos.PatientHomeResponse;
import ru.antenatal.web.dto.PatientDtos.PatientMeResponse;
import ru.antenatal.web.dto.PatientDtos.ProfileResponse;
import ru.antenatal.web.dto.PatientDtos.SymptomReportRequest;
import ru.antenatal.web.dto.PatientDtos.SymptomReportResponse;
import ru.antenatal.web.dto.PatientDtos.WatersResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientAccessService patientAccessService;
    private final PatientAccountRepository patientAccountRepository;
    private final PatientCardRepository patientCardRepository;
    private final DoctorRepository doctorRepository;
    private final PatientCardService patientCardService;
    private final GainService gainService;
    private final ContentService contentService;
    private final DiaryService diaryService;
    private final SymptomService symptomService;
    private final ContractionService contractionService;
    private final AlertService alertService;
    private final MessageService messageService;
    private final LinkService linkService;
    private final AppointmentService appointmentService;
    private final ru.antenatal.service.AuditService auditService;

    @GetMapping("/me")
    public PatientMeResponse me(@AuthenticationPrincipal CurrentPrincipal principal) {
        String messengerId = principal.requireMessengerId();
        Platform platform = patientAccessService.platformOf(principal);
        Optional<PatientAccount> account = patientAccountRepository
                .findByPlatformAndPlatformUserIdAndRevokedAtIsNull(platform, messengerId);
        if (account.isEmpty()) {
            return new PatientMeResponse(false, null);
        }
        PatientCard card = patientCardRepository.findById(account.get().getPatientCardId()).orElse(null);
        if (card == null || card.getStatus() != PatientStatus.ACTIVE) {
            return new PatientMeResponse(false, null);
        }
        return new PatientMeResponse(true, home(card));
    }

    @GetMapping("/invite/{token}")
    public InvitePreviewResponse preview(@AuthenticationPrincipal CurrentPrincipal principal,
                                         @PathVariable String token) {
        return linkService.preview(token, patientAccessService.platformOf(principal),
                principal.requireMessengerId());
    }

    @PostMapping("/invite/{token}/confirm")
    public InviteConfirmResponse confirm(@AuthenticationPrincipal CurrentPrincipal principal,
                                         @PathVariable String token,
                                         @RequestBody InviteConfirmRequest request) {
        String messengerId = principal.requireMessengerId();
        PatientCard card = linkService.confirm(token, patientAccessService.platformOf(principal),
                messengerId, messengerId, request.consent());
        return new InviteConfirmResponse(true, patientCardService.summary(card));
    }

    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal CurrentPrincipal principal) {
        PatientAccessService.PatientContext context = patientAccessService.requireContext(principal);
        PatientCard card = context.card();
        Doctor doctor = doctorRepository.findById(card.getDoctorId()).orElse(null);
        return new ProfileResponse(
                patientCardService.summary(card),
                doctor == null ? null : doctor.getFullName(),
                card.getReminderTime(),
                card.getHeightCm(),
                card.getPrePregnancyWeightKg(),
                context.account().getConsentVersion(),
                context.account().getConsentAt());
    }

    @GetMapping("/gain")
    public GainView gain(@AuthenticationPrincipal CurrentPrincipal principal) {
        return gainService.gain(patientAccessService.requireCard(principal));
    }

    @GetMapping("/content/current")
    public ContentBundle currentContent(@AuthenticationPrincipal CurrentPrincipal principal) {
        PatientCard card = patientAccessService.requireCard(principal);
        int week = contentService.currentWeek(card);
        WeekContentView content = contentService.weekContent(week).orElse(null);
        return new ContentBundle(week, content, contentService.ration(card).orElse(null));
    }

    @GetMapping("/content/{week}")
    public ContentBundle content(@AuthenticationPrincipal CurrentPrincipal principal,
                                 @PathVariable int week) {
        PatientCard card = patientAccessService.requireCard(principal);
        int safeWeek = Math.max(4, Math.min(42, week));
        WeekContentView content = contentService.weekContent(safeWeek).orElse(null);
        return new ContentBundle(safeWeek, content, contentService.ration(card).orElse(null));
    }

    @GetMapping("/diary")
    public List<DiaryView> diary(@AuthenticationPrincipal CurrentPrincipal principal,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return diaryService.history(patientAccessService.requireCard(principal), from, to);
    }

    @GetMapping("/diary/today")
    public DiaryView today(@AuthenticationPrincipal CurrentPrincipal principal) {
        return diaryService.today(patientAccessService.requireCard(principal));
    }

    @PostMapping("/diary")
    public DiarySaveResponse saveDiary(@AuthenticationPrincipal CurrentPrincipal principal,
                                       @RequestBody DiarySaveRequest request) {
        return diaryService.save(patientAccessService.requireCard(principal), request);
    }

    @PostMapping("/symptoms")
    public SymptomReportResponse symptoms(@AuthenticationPrincipal CurrentPrincipal principal,
                                          @Valid @RequestBody SymptomReportRequest request) {
        return symptomService.report(patientAccessService.requireCard(principal), request);
    }

    @GetMapping("/symptoms/history")
    public List<SymptomReportResponse> symptomHistory(@AuthenticationPrincipal CurrentPrincipal principal) {
        return symptomService.history(patientAccessService.requireCard(principal));
    }

    @GetMapping("/contractions")
    public ContractionStateResponse contractions(@AuthenticationPrincipal CurrentPrincipal principal) {
        return contractionService.state(patientAccessService.requireCard(principal));
    }

    @PostMapping("/contractions")
    public ContractionStateResponse addContraction(@AuthenticationPrincipal CurrentPrincipal principal,
                                                   @Valid @RequestBody ContractionAddRequest request) {
        return contractionService.add(patientAccessService.requireCard(principal), request);
    }

    @PostMapping("/contractions/close")
    public void closeContractions(@AuthenticationPrincipal CurrentPrincipal principal) {
        contractionService.close(patientAccessService.requireCard(principal));
    }

    @PostMapping("/contractions/waters")
    public WatersResponse waters(@AuthenticationPrincipal CurrentPrincipal principal) {
        PatientCard card = patientAccessService.requireCard(principal);
        ContractionStateResponse response = contractionService.waters(card);
        return new WatersResponse(true, response.message());
    }

    @GetMapping("/messages")
    public MessagesResponse messages(@AuthenticationPrincipal CurrentPrincipal principal) {
        return messageService.forPatient(patientAccessService.requireCard(principal));
    }

    @PostMapping("/messages")
    public MessagesResponse sendMessage(@AuthenticationPrincipal CurrentPrincipal principal,
                                        @Valid @RequestBody MessageSendRequest request) {
        return messageService.sendByPatient(patientAccessService.requireCard(principal), request);
    }

    @PostMapping("/messages/read")
    public void readMessages(@AuthenticationPrincipal CurrentPrincipal principal) {
        messageService.markReadByPatient(patientAccessService.requireCard(principal));
    }

    @GetMapping("/alerts")
    public List<AlertView> alerts(@AuthenticationPrincipal CurrentPrincipal principal) {
        return alertService.forPatient(patientAccessService.requireCard(principal));
    }

    @GetMapping("/appointments")
    public List<AppointmentView> appointments(@AuthenticationPrincipal CurrentPrincipal principal) {
        return appointmentService.listForPatient(patientAccessService.requireCard(principal));
    }

    @PostMapping("/appointments/{id}/confirm")
    public AppointmentView confirmAppointment(@AuthenticationPrincipal CurrentPrincipal principal,
                                              @PathVariable Long id) {
        return appointmentService.respond(patientAccessService.requireCard(principal),
                id, PatientResponse.CONFIRMED);
    }

    @PostMapping("/appointments/{id}/decline")
    public AppointmentView declineAppointment(@AuthenticationPrincipal CurrentPrincipal principal,
                                              @PathVariable Long id) {
        return appointmentService.respond(patientAccessService.requireCard(principal),
                id, PatientResponse.DECLINED);
    }

    @PostMapping("/consent/revoke")
    public void revokeConsent(@AuthenticationPrincipal CurrentPrincipal principal) {
        PatientAccessService.PatientContext context = patientAccessService.requireContext(principal);
        patientAccessService.revokeConsent(context.account());
        auditService.log("PATIENT", principal.requireMessengerId(),
                "CONSENT_REVOKE", "PATIENT_CARD",
                String.valueOf(context.card().getId()), null);
    }

    private PatientHomeResponse home(PatientCard card) {
        int week = contentService.currentWeek(card);
        Doctor doctor = doctorRepository.findById(card.getDoctorId()).orElse(null);
        return new PatientHomeResponse(
                patientCardService.summary(card),
                gainService.gain(card),
                contentService.weekContent(week).orElse(null),
                contentService.ration(card).orElse(null),
                diaryService.today(card),
                messageService.forPatient(card).unread(),
                doctor == null ? null : doctor.getFullName(),
                appointmentService.nextForPatient(card));
    }
}
