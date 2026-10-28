package ru.antenatal.calc;

import org.junit.jupiter.api.Test;
import ru.antenatal.domain.GlucoseContext;
import ru.antenatal.domain.Severity;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VitalsEvaluatorTest {

    private final VitalsEvaluator.Thresholds thresholds = VitalsEvaluator.fromMap(Map.of());

    @Test
    void normalBpHasNoAlert() {
        assertThat(VitalsEvaluator.evaluateBp(120, 80, thresholds)).isNull();
    }

    @Test
    void elevatedBpIsWarning() {
        VitalsEvaluator.VitalAlert alert = VitalsEvaluator.evaluateBp(145, 95, thresholds);
        assertThat(alert).isNotNull();
        assertThat(alert.severity()).isEqualTo(Severity.WARNING);
    }

    @Test
    void severeBpIsCritical() {
        VitalsEvaluator.VitalAlert alert = VitalsEvaluator.evaluateBp(165, 115, thresholds);
        assertThat(alert).isNotNull();
        assertThat(alert.severity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void fastingGlucoseThresholds() {
        assertThat(VitalsEvaluator.evaluateGlucose(
                new BigDecimal("5.5"), GlucoseContext.FASTING, thresholds).severity())
                .isEqualTo(Severity.WARNING);

        assertThat(VitalsEvaluator.evaluateGlucose(
                new BigDecimal("7.2"), GlucoseContext.FASTING, thresholds).severity())
                .isEqualTo(Severity.CRITICAL);
    }

    @Test
    void postprandialGlucoseThresholds() {
        assertThat(VitalsEvaluator.evaluateGlucose(
                new BigDecimal("6.7"), GlucoseContext.POSTPRANDIAL, thresholds).severity())
                .isEqualTo(Severity.WARNING);

        assertThat(VitalsEvaluator.evaluateGlucose(
                new BigDecimal("12.0"), GlucoseContext.POSTPRANDIAL, thresholds).severity())
                .isEqualTo(Severity.CRITICAL);
    }

    @Test
    void glucoseWithoutContextIsIgnored() {
        assertThat(VitalsEvaluator.evaluateGlucose(
                new BigDecimal("7.2"), null, thresholds)).isNull();
    }
}
