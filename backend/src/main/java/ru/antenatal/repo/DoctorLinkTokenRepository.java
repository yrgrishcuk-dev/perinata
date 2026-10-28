package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.DoctorLinkToken;

import java.util.Optional;

public interface DoctorLinkTokenRepository extends JpaRepository<DoctorLinkToken, Long> {
    Optional<DoctorLinkToken> findByTokenHash(String tokenHash);

    java.util.List<DoctorLinkToken> findByDoctorIdAndUsedAtIsNull(Long doctorId);
}
