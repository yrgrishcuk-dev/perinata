package ru.antenatal.calc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class GainCalculator {

    public enum Status {
        BELOW,
        WITHIN,
        ABOVE,
        UNKNOWN
    }

    public record GestationalAge(int weeks, int days) {
        public String label() {
            return weeks + " нед " + days + " дн";
        }
    }

    public record Assessment(
            GestationalAge age,
            BigDecimal minGainKg,
            BigDecimal maxGainKg,
            BigDecimal gainKg,
            Status status
    ) {
    }

    private GainCalculator() {
    }

    public static GestationalAge gestationalAge(LocalDate edd, LocalDate today) {
        if (edd == null) {
            return new GestationalAge(0, 0);
        }
        long daysPregnant = 280 - ChronoUnit.DAYS.between(today, edd);
        if (daysPregnant < 0) {
            daysPregnant = 0;
        }
        if (daysPregnant > 294) {
            daysPregnant = 294;
        }
        return new GestationalAge((int) (daysPregnant / 7), (int) (daysPregnant % 7));
    }

    public static Assessment assess(
            LocalDate edd,
            LocalDate today,
            BigDecimal prePregnancyWeightKg,
            BigDecimal currentWeightKg,
            BigDecimal minGainKg,
            BigDecimal maxGainKg
    ) {
        GestationalAge age = gestationalAge(edd, today);
        if (prePregnancyWeightKg == null || currentWeightKg == null) {
            return new Assessment(age, minGainKg, maxGainKg, null, Status.UNKNOWN);
        }
        BigDecimal gain = currentWeightKg.subtract(prePregnancyWeightKg);
        Status status = Status.WITHIN;
        if (minGainKg != null && gain.compareTo(minGainKg) < 0) {
            status = Status.BELOW;
        } else if (maxGainKg != null && gain.compareTo(maxGainKg) > 0) {
            status = Status.ABOVE;
        }
        return new Assessment(age, minGainKg, maxGainKg, gain, status);
    }
}
