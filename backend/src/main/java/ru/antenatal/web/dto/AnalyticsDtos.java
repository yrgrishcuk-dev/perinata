package ru.antenatal.web.dto;

import ru.antenatal.domain.AppointmentType;

import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record DailyPoint(
            LocalDate date,
            long total,
            long confirmed,
            long completed,
            long noShow,
            long cancelled
    ) {
    }

    public record TypePoint(
            AppointmentType type,
            long total,
            long completed,
            long noShow
    ) {
    }

    public record DoctorPoint(
            long doctorId,
            String fullName,
            long total,
            long completed,
            long noShow,
            long cancelled
    ) {
    }

    public record AppointmentAnalyticsView(
            LocalDate from,
            LocalDate to,
            int days,
            long total,
            long confirmed,
            long declined,
            long completed,
            long noShow,
            long cancelled,
            double confirmationRate,
            double showRate,
            double noShowRate,
            double cancelRate,
            List<DailyPoint> daily,
            List<TypePoint> byType,
            List<DoctorPoint> doctors
    ) {
    }
}
