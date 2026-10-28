package ru.antenatal.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.common.TokenUtil;
import ru.antenatal.domain.AuthSession;
import ru.antenatal.domain.PrincipalType;
import ru.antenatal.repo.AuthSessionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthSessionRepository authSessionRepository;

    @Transactional
    public String createSession(PrincipalType type, String principalId) {
        String raw = TokenUtil.randomToken(32);
        AuthSession session = new AuthSession();
        session.setTokenHash(TokenUtil.sha256(raw));
        session.setPrincipalType(type);
        session.setPrincipalId(principalId);
        session.setExpiresAt(Instant.now().plus(ttl(type)));
        authSessionRepository.save(session);
        return raw;
    }

    @Transactional(readOnly = true)
    public Optional<CurrentPrincipal> resolve(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return authSessionRepository.findByTokenHashAndExpiresAtAfter(
                        TokenUtil.sha256(rawToken), Instant.now())
                .map(session -> {
                    if (session.getPrincipalType() == PrincipalType.TELEGRAM) {
                        return new CurrentPrincipal(session.getPrincipalType(), null, session.getPrincipalId());
                    }
                    return new CurrentPrincipal(session.getPrincipalType(), Long.valueOf(session.getPrincipalId()), null);
                });
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            authSessionRepository.deleteByTokenHash(TokenUtil.sha256(rawToken));
        }
    }

    @Transactional
    public void revokeDoctorSessions(Long doctorId) {
        authSessionRepository.findAll().stream()
                .filter(s -> s.getPrincipalType() != PrincipalType.TELEGRAM)
                .filter(s -> s.getPrincipalId().equals(String.valueOf(doctorId)))
                .forEach(authSessionRepository::delete);
    }

    public CurrentPrincipal require(CurrentPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }
        return principal;
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void cleanup() {
        authSessionRepository.deleteByExpiresAtBefore(Instant.now().minus(Duration.ofDays(1)));
    }

    private Duration ttl(PrincipalType type) {
        return switch (type) {
            case DOCTOR, ADMIN -> Duration.ofHours(12);
            case TELEGRAM -> Duration.ofDays(30);
        };
    }
}
