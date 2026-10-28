package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.LinkToken;

import java.time.Instant;
import java.util.Optional;

public interface LinkTokenRepository extends JpaRepository<LinkToken, Long> {
    Optional<LinkToken> findByTokenHash(String tokenHash);

    Optional<LinkToken> findFirstByPatientCardIdAndExpiresAtAfterAndUsedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(
            Long patientCardId, Instant now);

    java.util.List<LinkToken> findByPatientCardIdAndUsedAtIsNullAndRevokedAtIsNull(Long patientCardId);
}
