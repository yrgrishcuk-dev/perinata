package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.Appointment;
import ru.antenatal.domain.AppointmentStatus;
import ru.antenatal.domain.AppointmentType;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PatientResponse;
import ru.antenatal.domain.Role;
import ru.antenatal.repo.AppointmentRepository;
import ru.antenatal.repo.DoctorRepository;
import ru.antenatal.web.dto.AnalyticsDtos.AppointmentAnalyticsView;
import ru.antenatal.web.dto.AnalyticsDtos.DailyPoint;
import ru.antenatal.web.dto.AnalyticsDtos.DoctorPoint;
import ru.antenatal.web.dto.AnalyticsDtos.TypePoint;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentAnalyticsService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAccessService doctorAccessService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public AppointmentAnalyticsView analytics(Doctor actor, int days, Long doctorId) {
        if (days != 7 && days != 30 && days != 90) {
            throw ApiException.badRequest("Период должен быть 7, 30 или 90 дней");
        }

        boolean admin = actor.getRole() == Role.ADMIN;
        List<Doctor> clinicDoctors = admin
                ? doctorRepository.findByClinicIdOrderByFullNameAsc(actor.getClinicId())
                : List.of(actor);
        if (doctorId != null) {
            if (!admin && !doctorId.equals(actor.getId())) {
                throw ApiException.forbidden("Можно смотреть только свою аналитику");
            }
            boolean allowed = clinicDoctors.stream().anyMatch(doctor -> doctor.getId().equals(doctorId));
            if (!allowed) {
                throw ApiException.notFound("Врач не найден");
            }
        }
        List<Long> doctorIds = clinicDoctors.stream()
                .filter(doctor -> doctorId == null || doctor.getId().equals(doctorId))
                .map(Doctor::getId)
                .toList();

        ZoneId zone = ZoneId.of(appProperties.getClinicTimezone());
        LocalDate to = LocalDate.now(zone);
        LocalDate from = to.minusDays(days - 1L);
        Instant fromInstant = from.atStartOfDay(zone).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdInAndStartAtBetweenOrderByStartAtAsc(
                        doctorIds, fromInstant, toInstant);

        Counts total = new Counts();
        Map<LocalDate, Counts> daily = new LinkedHashMap<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            daily.put(date, new Counts());
        }
        Map<AppointmentType, Counts> byType = new LinkedHashMap<>();
        for (AppointmentType type : AppointmentType.values()) {
            byType.put(type, new Counts());
        }
        Map<Long, Counts> byDoctor = new LinkedHashMap<>();
        for (Doctor doctor : clinicDoctors) {
            byDoctor.put(doctor.getId(), new Counts());
        }

        for (Appointment appointment : appointments) {
            total.add(appointment);
            LocalDate date = appointment.getStartAt().atZone(zone).toLocalDate();
            daily.get(date).add(appointment);
            byType.get(appointment.getType()).add(appointment);
            byDoctor.computeIfAbsent(appointment.getDoctorId(), key -> new Counts())
                    .add(appointment);
        }

        List<DailyPoint> dailyPoints = daily.entrySet().stream()
                .map(entry -> new DailyPoint(
                        entry.getKey(),
                        entry.getValue().total,
                        entry.getValue().confirmed,
                        entry.getValue().completed,
                        entry.getValue().noShow,
                        entry.getValue().cancelled))
                .toList();
        List<TypePoint> typePoints = byType.entrySet().stream()
                .map(entry -> new TypePoint(
                        entry.getKey(),
                        entry.getValue().total,
                        entry.getValue().completed,
                        entry.getValue().noShow))
                .toList();
        List<DoctorPoint> doctorPoints = admin
                ? clinicDoctors.stream()
                .map(doctor -> {
                    Counts counts = byDoctor.getOrDefault(doctor.getId(), new Counts());
                    return new DoctorPoint(doctor.getId(), doctor.getFullName(),
                            counts.total, counts.completed, counts.noShow, counts.cancelled);
                })
                .toList()
                : List.of();

        return new AppointmentAnalyticsView(
                from,
                to,
                days,
                total.total,
                total.confirmed,
                total.declined,
                total.completed,
                total.noShow,
                total.cancelled,
                rate(total.confirmed, total.total),
                rate(total.completed, total.completed + total.noShow),
                rate(total.noShow, total.completed + total.noShow),
                rate(total.cancelled, total.total),
                dailyPoints,
                typePoints,
                doctorPoints);
    }

    private double rate(long value, long base) {
        if (base <= 0) {
            return 0;
        }
        return Math.round(value * 1000.0 / base) / 10.0;
    }

    private static final class Counts {
        private long total;
        private long confirmed;
        private long declined;
        private long completed;
        private long noShow;
        private long cancelled;

        private void add(Appointment appointment) {
            total++;
            if (appointment.getPatientResponse() == PatientResponse.CONFIRMED) {
                confirmed++;
            }
            if (appointment.getPatientResponse() == PatientResponse.DECLINED) {
                declined++;
            }
            if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                completed++;
            }
            if (appointment.getStatus() == AppointmentStatus.NO_SHOW) {
                noShow++;
            }
            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                cancelled++;
            }
        }
    }
}
