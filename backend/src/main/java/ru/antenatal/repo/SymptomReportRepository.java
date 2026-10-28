package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.SymptomReport;

import java.util.List;

public interface SymptomReportRepository extends JpaRepository<SymptomReport, Long> {
    List<SymptomReport> findTop50ByPatientCardIdOrderByReportedAtDesc(Long patientCardId);
}
