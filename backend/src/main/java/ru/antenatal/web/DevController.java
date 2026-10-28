package ru.antenatal.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.antenatal.common.ApiException;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PatientAccount;
import ru.antenatal.domain.Platform;
import ru.antenatal.domain.PrincipalType;
import ru.antenatal.repo.DoctorRepository;
import ru.antenatal.repo.PatientAccountRepository;
import ru.antenatal.security.AuthService;
import ru.antenatal.web.dto.AuthDtos.PatientSessionResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevController {

    private final AppProperties appProperties;
    private final DoctorRepository doctorRepository;
    private final PatientAccountRepository patientAccountRepository;
    private final AuthService authService;

    public record DevLoginRequest(String email) {
    }

    public record DevLoginResponse(String token, String role, String fullName) {
    }

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("devMode", appProperties.isDevMode());
    }

    @PostMapping("/login")
    public DevLoginResponse login(@RequestBody DevLoginRequest request) {
        requireDev();
        String email = request.email() == null ? "doctor@example.com" : request.email().trim();
        Doctor doctor = doctorRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Врач не найден"));
        PrincipalType type = "ADMIN".equals(doctor.getRole().name())
                ? PrincipalType.ADMIN : PrincipalType.DOCTOR;
        String token = authService.createSession(type, String.valueOf(doctor.getId()));
        return new DevLoginResponse(token, doctor.getRole().name(), doctor.getFullName());
    }

    @PostMapping("/patient-session")
    public PatientSessionResponse patientSession() {
        requireDev();
        PatientAccount account = patientAccountRepository
                .findByPlatformAndPlatformUserIdAndRevokedAtIsNull(Platform.TELEGRAM, "demo-patient")
                .orElseThrow(() -> ApiException.notFound("Демо-пациентка не создана"));
        String token = authService.createSession(PrincipalType.TELEGRAM, account.getPlatformUserId());
        return new PatientSessionResponse(token, true);
    }

    private void requireDev() {
        if (!appProperties.isDevMode()) {
            throw ApiException.notFound("Not found");
        }
    }
}
