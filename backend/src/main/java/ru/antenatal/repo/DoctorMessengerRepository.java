package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.DoctorMessenger;
import ru.antenatal.domain.Platform;

import java.util.Optional;

public interface DoctorMessengerRepository extends JpaRepository<DoctorMessenger, Long> {
    Optional<DoctorMessenger> findByDoctorIdAndRevokedAtIsNull(Long doctorId);

    Optional<DoctorMessenger> findByPlatformAndPlatformUserIdAndRevokedAtIsNull(Platform platform, String platformUserId);
}
