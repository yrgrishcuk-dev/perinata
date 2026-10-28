package ru.antenatal.calc;

import org.junit.jupiter.api.Test;
import ru.antenatal.domain.Severity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContractionEvaluatorTest {

    private final Instant now = Instant.parse("2026-01-01T12:00:00Z");

    private List<ContractionEvaluator.Point> pointsEveryMinutes(int count, int intervalMinutes) {
        List<ContractionEvaluator.Point> points = new ArrayList<>();
        Instant first = now.minusSeconds((long) intervalMinutes * (count - 1) * 60L);
        for (int i = 0; i < count; i++) {
            Instant start = first.plusSeconds((long) intervalMinutes * i * 60L);
            points.add(new ContractionEvaluator.Point(start, start.plusSeconds(60)));
        }
        return points;
    }

    @Test
    void criticalBeforeThirtySevenWeeks() {
        ContractionEvaluator.Assessment assessment =
                ContractionEvaluator.evaluate(36, pointsEveryMinutes(4, 10), now);

        assertThat(assessment.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(assessment.countLastHour()).isEqualTo(4);
    }

    @Test
    void warningAtTermWithFourContractions() {
        ContractionEvaluator.Assessment assessment =
                ContractionEvaluator.evaluate(38, pointsEveryMinutes(4, 10), now);

        assertThat(assessment.severity()).isEqualTo(Severity.WARNING);
    }

    @Test
    void criticalAtTermWithRegularFiveMinuteContractions() {
        ContractionEvaluator.Assessment assessment =
                ContractionEvaluator.evaluate(39, pointsEveryMinutes(11, 5), now);

        assertThat(assessment.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(assessment.averageIntervalSeconds()).isBetween(290, 310);
        assertThat(assessment.averageDurationSeconds()).isBetween(55, 65);
    }

    @Test
    void informationalWhenRare() {
        ContractionEvaluator.Assessment assessment =
                ContractionEvaluator.evaluate(38, pointsEveryMinutes(2, 15), now);

        assertThat(assessment.severity()).isEqualTo(Severity.INFO);
    }

    @Test
    void watersAlwaysCritical() {
        assertThat(ContractionEvaluator.watersBroken().severity()).isEqualTo(Severity.CRITICAL);
    }
}
