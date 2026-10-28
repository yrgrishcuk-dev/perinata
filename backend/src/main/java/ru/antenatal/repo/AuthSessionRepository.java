package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.AuthSession;

import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    void deleteByExpiresAtBefore(Instant now);

    void deleteByTokenHash(String tokenHash);
}
