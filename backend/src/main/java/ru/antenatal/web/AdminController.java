package ru.antenatal.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.antenatal.domain.AuditLog;
import ru.antenatal.domain.Doctor;
import ru.antenatal.security.CurrentPrincipal;
import ru.antenatal.service.AdminService;
import ru.antenatal.service.PatientCardService;
import ru.antenatal.web.dto.DoctorDtos.DoctorCreateRequest;
import ru.antenatal.web.dto.DoctorDtos.DoctorUpdateRequest;
import ru.antenatal.web.dto.DoctorDtos.DoctorView;
import ru.antenatal.web.dto.DoctorDtos.PatientSummary;
import ru.antenatal.web.dto.DoctorDtos.ResetPasswordRequest;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PatientCardService patientCardService;

    @GetMapping("/doctors")
    public List<DoctorView> doctors(@AuthenticationPrincipal CurrentPrincipal principal) {
        return adminService.list(principal);
    }

    @PostMapping("/doctors")
    public DoctorView createDoctor(@AuthenticationPrincipal CurrentPrincipal principal,
                                   @Valid @RequestBody DoctorCreateRequest request) {
        return adminService.create(principal, request);
    }

    @PatchMapping("/doctors/{id}")
    public DoctorView updateDoctor(@AuthenticationPrincipal CurrentPrincipal principal,
                                   @PathVariable Long id,
                                   @Valid @RequestBody DoctorUpdateRequest request) {
        return adminService.update(principal, id, request);
    }

    @PostMapping("/doctors/{id}/reset-password")
    public void resetPassword(@AuthenticationPrincipal CurrentPrincipal principal,
                              @PathVariable Long id,
                              @Valid @RequestBody ResetPasswordRequest request) {
        adminService.resetPassword(principal, id, request.password());
    }

    @GetMapping("/audit")
    public List<AuditLog> audit(@AuthenticationPrincipal CurrentPrincipal principal) {
        return adminService.audit(principal);
    }

    @GetMapping("/patients")
    public List<PatientSummary> patients(@AuthenticationPrincipal CurrentPrincipal principal,
                                         @RequestParam(required = false) String query) {
        Doctor admin = adminService.requireAdmin(principal);
        return patientCardService.list(admin, query);
    }
}
