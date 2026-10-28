package ru.antenatal.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.antenatal.common.ApiException;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PrincipalType;
import ru.antenatal.repo.DoctorRepository;
import ru.antenatal.security.AuthService;
import ru.antenatal.security.CurrentPrincipal;
import ru.antenatal.service.AuditService;
import ru.antenatal.web.dto.AuthDtos.ChangePasswordRequest;
import ru.antenatal.web.dto.AuthDtos.LoginRequest;
import ru.antenatal.web.dto.AuthDtos.LoginResponse;
import ru.antenatal.web.dto.AuthDtos.MeResponse;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditService auditService;

    @PostMapping("/login")
    @Transactional
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Doctor doctor = doctorRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> ApiException.unauthorized("Неверный email или пароль"));
        if (!"ACTIVE".equals(doctor.getStatus())) {
            throw ApiException.forbidden("Учётная запись отключена");
        }
        if (!passwordEncoder.matches(request.password(), doctor.getPasswordHash())) {
            throw ApiException.unauthorized("Неверный email или пароль");
        }
        doctor.setLastLoginAt(Instant.now());
        doctorRepository.save(doctor);

        PrincipalType type = "ADMIN".equals(doctor.getRole().name())
                ? PrincipalType.ADMIN
                : PrincipalType.DOCTOR;
        String token = authService.createSession(type, String.valueOf(doctor.getId()));
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "LOGIN", "DOCTOR", String.valueOf(doctor.getId()), null);

        return new LoginResponse(token, doctor.getRole().name(), doctor.getFullName(),
                doctor.getId(), doctor.isMustChangePassword());
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal CurrentPrincipal principal) {
        Doctor doctor = doctorRepository.findById(principal.requireDoctorId())
                .orElseThrow(() -> ApiException.unauthorized("Врач не найден"));
        return new MeResponse(doctor.getId(), doctor.getFullName(), doctor.getEmail(),
                doctor.getRole().name(), doctor.getClinicId());
    }

    @PostMapping("/change-password")
    @Transactional
    public void changePassword(@AuthenticationPrincipal CurrentPrincipal principal,
                               @Valid @RequestBody ChangePasswordRequest request) {
        Doctor doctor = doctorRepository.findById(principal.requireDoctorId())
                .orElseThrow(() -> ApiException.unauthorized("Врач не найден"));
        if (!passwordEncoder.matches(request.currentPassword(), doctor.getPasswordHash())) {
            throw ApiException.badRequest("Текущий пароль неверен");
        }
        doctor.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        doctor.setMustChangePassword(false);
        doctorRepository.save(doctor);
        auditService.log(principal, "CHANGE_PASSWORD", "DOCTOR",
                String.valueOf(doctor.getId()), null);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                       @AuthenticationPrincipal CurrentPrincipal principal) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logout(authorization.substring("Bearer ".length()).trim());
        }
        if (principal != null) {
            auditService.log(principal, "LOGOUT", "DOCTOR",
                    String.valueOf(principal.principalId()), null);
        }
    }
}
