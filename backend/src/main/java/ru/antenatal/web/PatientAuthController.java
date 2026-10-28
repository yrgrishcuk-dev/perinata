package ru.antenatal.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.antenatal.domain.Platform;
import ru.antenatal.domain.PrincipalType;
import ru.antenatal.repo.PatientAccountRepository;
import ru.antenatal.security.AuthService;
import ru.antenatal.service.AuditService;
import ru.antenatal.telegram.InitDataValidator;
import ru.antenatal.telegram.InitDataValidator.TelegramUser;
import ru.antenatal.web.dto.AuthDtos.PatientSessionRequest;
import ru.antenatal.web.dto.AuthDtos.PatientSessionResponse;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientAuthController {

    private final InitDataValidator initDataValidator;
    private final AuthService authService;
    private final PatientAccountRepository patientAccountRepository;
    private final AuditService auditService;

    @PostMapping("/session")
    public PatientSessionResponse session(@RequestBody PatientSessionRequest request) {
        TelegramUser user = initDataValidator.validate(request.initData());
        return session(user, Platform.TELEGRAM, PrincipalType.TELEGRAM, "TELEGRAM_USER");
    }

    @PostMapping("/session/max")
    public PatientSessionResponse sessionMax(@RequestBody PatientSessionRequest request) {
        TelegramUser user = initDataValidator.validateMax(request.initData());
        return session(user, Platform.MAX, PrincipalType.MAX, "MAX_USER");
    }

    private PatientSessionResponse session(TelegramUser user, Platform platform,
                                           PrincipalType principalType, String entityType) {
        boolean bound = patientAccountRepository
                .findByPlatformAndPlatformUserIdAndRevokedAtIsNull(platform, user.id())
                .isPresent();
        String token = authService.createSession(principalType, user.id());
        auditService.log("PATIENT", user.id(), "SESSION", entityType, user.id(), null);
        return new PatientSessionResponse(token, bound);
    }
}
