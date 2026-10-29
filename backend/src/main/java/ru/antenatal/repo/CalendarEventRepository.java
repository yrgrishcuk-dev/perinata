package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.CalendarEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByDoctorIdAndEventDateBetweenOrderByEventDateAscEventTimeAsc(
            Long doctorId, LocalDate from, LocalDate to);

    Optional<CalendarEvent> findByIdAndDoctorId(Long id, Long doctorId);
}
