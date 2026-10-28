package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.DiaryEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {
    Optional<DiaryEntry> findByPatientCardIdAndEntryDate(Long patientCardId, LocalDate entryDate);

    List<DiaryEntry> findByPatientCardIdAndEntryDateBetweenOrderByEntryDateAsc(
            Long patientCardId, LocalDate from, LocalDate to);

    List<DiaryEntry> findTop400ByPatientCardIdOrderByEntryDateDesc(Long patientCardId);

    Optional<DiaryEntry> findFirstByPatientCardIdOrderByEntryDateDesc(Long patientCardId);

    Optional<DiaryEntry> findFirstByPatientCardIdAndWeightKgIsNotNullOrderByEntryDateDesc(Long patientCardId);

    List<DiaryEntry> findTop2ByPatientCardIdAndWeightKgIsNotNullOrderByEntryDateDesc(Long patientCardId);

    boolean existsByPatientCardIdAndEntryDate(Long patientCardId, LocalDate entryDate);
}
