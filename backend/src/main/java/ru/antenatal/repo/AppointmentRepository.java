package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.Appointment;
import ru.antenatal.domain.AppointmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdAndStartAtBetweenOrderByStartAtAsc(
            Long doctorId, Instant from, Instant to);

    List<Appointment> findByPatientCardIdOrderByStartAtDesc(Long patientCardId);

    Optional<Appointment> findByIdAndDoctorId(Long id, Long doctorId);

    Optional<Appointment> findByIdAndPatientCardId(Long id, Long patientCardId);

    Optional<Appointment> findFirstByPatientCardIdAndStatusAndStartAtAfterOrderByStartAtAsc(
            Long patientCardId, AppointmentStatus status, Instant after);

    List<Appointment> findByStatusAndReminderSentAtIsNullAndStartAtBetweenOrderByStartAtAsc(
            AppointmentStatus status, Instant from, Instant to);
}
