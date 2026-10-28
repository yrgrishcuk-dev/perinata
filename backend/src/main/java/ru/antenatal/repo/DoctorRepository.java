package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.Doctor;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Doctor> findByClinicIdOrderByFullNameAsc(Long clinicId);

    long countByClinicIdAndRole(Long clinicId, ru.antenatal.domain.Role role);
}
