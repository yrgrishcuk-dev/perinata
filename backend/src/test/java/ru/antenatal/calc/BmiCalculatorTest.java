package ru.antenatal.calc;

import org.junit.jupiter.api.Test;
import ru.antenatal.domain.BmiCategory;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BmiCalculatorTest {

    @Test
    void calculatesBmiWithOneDecimal() {
        assertThat(BmiCalculator.bmi(new BigDecimal("165"), new BigDecimal("62")))
                .isEqualByComparingTo(new BigDecimal("22.8"));
    }

    @Test
    void returnsNullWhenDataMissing() {
        assertThat(BmiCalculator.bmi(null, new BigDecimal("62"))).isNull();
        assertThat(BmiCalculator.bmi(new BigDecimal("165"), null)).isNull();
    }

    @Test
    void categorizesBmi() {
        assertThat(BmiCalculator.category(new BigDecimal("17.0"))).isEqualTo(BmiCategory.UNDERWEIGHT);
        assertThat(BmiCalculator.category(new BigDecimal("18.5"))).isEqualTo(BmiCategory.NORMAL);
        assertThat(BmiCalculator.category(new BigDecimal("24.9"))).isEqualTo(BmiCategory.NORMAL);
        assertThat(BmiCalculator.category(new BigDecimal("25.0"))).isEqualTo(BmiCategory.OVERWEIGHT);
        assertThat(BmiCalculator.category(new BigDecimal("30.0"))).isEqualTo(BmiCategory.OBESE);
    }
}
