package ru.antenatal.calc;

import ru.antenatal.domain.BmiCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BmiCalculator {

    private BmiCalculator() {
    }

    public static BigDecimal bmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null) {
            return null;
        }
        double heightM = heightCm.doubleValue() / 100.0;
        double value = weightKg.doubleValue() / (heightM * heightM);
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    public static BmiCategory category(BigDecimal bmi) {
        if (bmi == null) {
            return null;
        }
        return BmiCategory.fromBmi(bmi.doubleValue());
    }
}
