package ru.antenatal.calc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GainCalculatorTest {

    private final LocalDate today = LocalDate.of(2026, 7, 14);
    private final LocalDate edd = LocalDate.of(2026, 12, 1);

    @Test
    void calculatesGestationalAgeFromEdd() {
        GainCalculator.GestationalAge age = GainCalculator.gestationalAge(edd, today);
        assertThat(age.weeks()).isEqualTo(20);
        assertThat(age.days()).isEqualTo(0);
        assertThat(age.label()).isEqualTo("20 нед 0 дн");
    }

    @Test
    void evaluatesGainWithinCorridor() {
        GainCalculator.Assessment assessment = GainCalculator.assess(
                edd, today, new BigDecimal("60"), new BigDecimal("65"),
                new BigDecimal("4"), new BigDecimal("8"));

        assertThat(assessment.gainKg()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(assessment.status()).isEqualTo(GainCalculator.Status.WITHIN);
    }

    @Test
    void detectsAboveAndBelowCorridor() {
        assertThat(GainCalculator.assess(
                edd, today, new BigDecimal("60"), new BigDecimal("70"),
                new BigDecimal("4"), new BigDecimal("8")).status())
                .isEqualTo(GainCalculator.Status.ABOVE);

        assertThat(GainCalculator.assess(
                edd, today, new BigDecimal("60"), new BigDecimal("62"),
                new BigDecimal("4"), new BigDecimal("8")).status())
                .isEqualTo(GainCalculator.Status.BELOW);
    }

    @Test
    void returnsUnknownWithoutWeight() {
        GainCalculator.Assessment assessment = GainCalculator.assess(
                edd, today, new BigDecimal("60"), null,
                new BigDecimal("4"), new BigDecimal("8"));

        assertThat(assessment.status()).isEqualTo(GainCalculator.Status.UNKNOWN);
        assertThat(assessment.gainKg()).isNull();
    }
}
