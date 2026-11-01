package ru.antenatal.calc;

import java.time.LocalDate;

public final class EddCalculator {

    private EddCalculator() {
    }

    public static LocalDate byLmp(LocalDate lmpDate, int cycleLength) {
        return lmpDate.plusDays(280L + cycleLength - 28L);
    }
}
