package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.PatientAccount;
import ru.antenatal.domain.Platform;

import java.util.Optional;

public interface PatientAccountRepository extends JpaRepository<PatientAccount, Long> {
    Optional<PatientAccount> findByPlatformAndPlatformUserIdAndRevokedAtIsNull(Platform platform, String platformUserId);

    Optional<PatientAccount> findByPatientCardIdAndRevokedAtIsNull(Long patientCardId);
}
