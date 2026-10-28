package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.PatientStatus;

import java.util.List;
import java.util.Optional;

public interface PatientCardRepository extends JpaRepository<PatientCard, Long> {
    Optional<PatientCard> findByIdAndDoctorId(Long id, Long doctorId);

    Optional<PatientCard> findByPublicCode(String publicCode);

    boolean existsByPublicCode(String publicCode);

    List<PatientCard> findByDoctorIdAndStatusOrderByCreatedAtDesc(Long doctorId, PatientStatus status);

    List<PatientCard> findByClinicIdAndStatusOrderByCreatedAtDesc(Long clinicId, PatientStatus status);

    List<PatientCard> findByStatus(PatientStatus status);

    long countByDoctorIdAndStatus(Long doctorId, PatientStatus status);
}
