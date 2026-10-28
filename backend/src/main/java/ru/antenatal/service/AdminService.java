package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.domain.AuditLog;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.Role;
import ru.antenatal.repo.AuditLogRepository;
import ru.antenatal.repo.DoctorMessengerRepository;
import ru.antenatal.repo.DoctorRepository;
import ru.antenatal.security.CurrentPrincipal;
import ru.antenatal.web.dto.DoctorDtos.DoctorCreateRequest;
import ru.antenatal.web.dto.DoctorDtos.DoctorUpdateRequest;
import ru.antenatal.web.dto.DoctorDtos.DoctorView;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DoctorRepository doctorRepository;
    private final DoctorMessengerRepository doctorMessengerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Doctor requireAdmin(CurrentPrincipal principal) {
        Long adminId = principal.requireAdminId();
        Doctor admin = doctorRepository.findById(adminId)
                .orElseThrow(() -> ApiException.unauthorized("Администратор не найден"));
        if (admin.getRole() != Role.ADMIN || !"ACTIVE".equals(admin.getStatus())) {
            throw ApiException.forbidden("Недостаточно прав");
        }
        return admin;
    }

    @Transactional
    public DoctorView create(CurrentPrincipal principal, DoctorCreateRequest request) {
        Doctor admin = requireAdmin(principal);
        String email = request.email().trim().toLowerCase();
        if (doctorRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Врач с таким email уже существует");
        }
        Doctor doctor = new Doctor();
        doctor.setClinicId(admin.getClinicId());
        doctor.setFullName(request.fullName().trim());
        doctor.setEmail(email);
        doctor.setPasswordHash(passwordEncoder.encode(request.password()));
        doctor.setRole(parseRole(request.role()));
        doctor.setMustChangePassword(true);
        doctorRepository.save(doctor);
        auditService.log(principal, "DOCTOR_CREATE", "DOCTOR",
                String.valueOf(doctor.getId()), null);
        return view(doctor);
    }

    @Transactional(readOnly = true)
    public List<DoctorView> list(CurrentPrincipal principal) {
        Doctor admin = requireAdmin(principal);
        return doctorRepository.findByClinicIdOrderByFullNameAsc(admin.getClinicId())
                .stream().map(this::view).toList();
    }

    @Transactional
    public DoctorView update(CurrentPrincipal principal, Long doctorId, DoctorUpdateRequest request) {
        Doctor admin = requireAdmin(principal);
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> ApiException.notFound("Врач не найден"));
        if (!doctor.getClinicId().equals(admin.getClinicId())) {
            throw ApiException.notFound("Врач не найден");
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            doctor.setFullName(request.fullName().trim());
        }
        if (request.role() != null) {
            doctor.setRole(parseRole(request.role()));
        }
        if (request.status() != null) {
            if (!request.status().equals("ACTIVE") && !request.status().equals("DISABLED")) {
                throw ApiException.badRequest("Недопустимый статус");
            }
            if (doctor.getId().equals(admin.getId()) && !request.status().equals("ACTIVE")) {
                throw ApiException.badRequest("Нельзя отключить собственную учётную запись");
            }
            doctor.setStatus(request.status());
        }
        if (doctor.getId().equals(admin.getId()) && doctor.getRole() != Role.ADMIN) {
            throw ApiException.badRequest("Нельзя снять с себя роль администратора");
        }
        doctorRepository.save(doctor);
        auditService.log(principal, "DOCTOR_UPDATE", "DOCTOR",
                String.valueOf(doctorId), request.toString());
        return view(doctor);
    }

    @Transactional
    public void resetPassword(CurrentPrincipal principal, Long doctorId, String password) {
        Doctor admin = requireAdmin(principal);
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> ApiException.notFound("Врач не найден"));
        if (!doctor.getClinicId().equals(admin.getClinicId())) {
            throw ApiException.notFound("Врач не найден");
        }
        doctor.setPasswordHash(passwordEncoder.encode(password));
        doctor.setMustChangePassword(true);
        doctorRepository.save(doctor);
        auditService.log(principal, "DOCTOR_PASSWORD_RESET", "DOCTOR",
                String.valueOf(doctorId), null);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> audit(CurrentPrincipal principal) {
        requireAdmin(principal);
        return auditLogRepository.findTop200ByOrderByCreatedAtDesc();
    }

    private DoctorView view(Doctor doctor) {
        boolean linked = doctorMessengerRepository
                .findByDoctorIdAndRevokedAtIsNull(doctor.getId()).isPresent();
        return new DoctorView(
                doctor.getId(),
                doctor.getFullName(),
                doctor.getEmail(),
                doctor.getRole().name(),
                doctor.getStatus(),
                linked,
                doctor.getLastLoginAt(),
                doctor.getCreatedAt());
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return Role.DOCTOR;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Недопустимая роль");
        }
    }
}
