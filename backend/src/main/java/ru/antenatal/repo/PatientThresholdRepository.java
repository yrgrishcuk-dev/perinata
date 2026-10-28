package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.PatientThreshold;

import java.util.List;
import java.util.Optional;

public interface PatientThresholdRepository extends JpaRepository<PatientThreshold, Long> {
    Optional<PatientThreshold> findByPatientCardIdAndCode(Long patientCardId, String code);

    List<PatientThreshold> findByPatientCardId(Long patientCardId);

    void deleteByPatientCardIdAndCode(Long patientCardId, String code);
}
