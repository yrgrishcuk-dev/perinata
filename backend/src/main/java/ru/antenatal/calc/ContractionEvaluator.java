package ru.antenatal.calc;

import ru.antenatal.domain.Severity;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class ContractionEvaluator {

    public record Point(Instant start, Instant end) {
    }

    public record Parameters(
            int pretermWeeksBefore,
            int pretermCount,
            int windowMinutes,
            int termCount,
            int goIntervalMinutes,
            int goDurationSeconds,
            int goCount,
            String pretermMessage,
            String termWarnMessage,
            String termGoMessage
    ) {
        public static Parameters defaults() {
            return new Parameters(
                    37, 4, 60, 4, 5, 60, 10,
                    "Признаки преждевременных родов. Срочно обратитесь в стационар или вызовите 103.",
                    "Схватки чаще 4 раз в час. Сообщите врачу и продолжайте наблюдение.",
                    "Регулярные схватки каждые 5 минут в течение часа. Пора в родильный дом.");
        }
    }

    public record Assessment(
            Severity severity,
            String message,
            int countLastHour,
            Integer averageIntervalSeconds,
            Integer averageDurationSeconds
    ) {
    }

    private ContractionEvaluator() {
    }

    public static Assessment evaluate(int gestationalWeeks, List<Point> contractions, Instant now) {
        return evaluate(gestationalWeeks, contractions, now, Parameters.defaults());
    }

    public static Assessment evaluate(int gestationalWeeks, List<Point> contractions,
                                      Instant now, Parameters parameters) {
        Instant from = now.minus(Duration.ofMinutes(parameters.windowMinutes()));
        List<Point> recent = contractions.stream()
                .filter(p -> !p.start().isBefore(from))
                .sorted(Comparator.comparing(Point::start))
                .toList();

        int count = recent.size();
        Integer averageDuration = null;
        Integer averageInterval = null;

        if (!recent.isEmpty()) {
            double totalDuration = recent.stream()
                    .mapToLong(p -> Duration.between(p.start(), p.end()).getSeconds())
                    .sum();
            averageDuration = (int) Math.round(totalDuration / count);
        }
        if (count >= 2) {
            long span = Duration.between(recent.get(0).start(), recent.get(count - 1).start()).getSeconds();
            averageInterval = (int) Math.round((double) span / (count - 1));
        }

        if (gestationalWeeks < parameters.pretermWeeksBefore()
                && count >= parameters.pretermCount()) {
            return new Assessment(
                    Severity.CRITICAL,
                    parameters.pretermMessage(),
                    count, averageInterval, averageDuration);
        }

        if (gestationalWeeks >= parameters.pretermWeeksBefore()
                && count >= parameters.termCount()) {
            boolean regular = averageInterval != null
                    && averageInterval <= parameters.goIntervalMinutes() * 60
                    && averageDuration != null
                    && averageDuration >= parameters.goDurationSeconds()
                    && count >= parameters.goCount();
            if (regular) {
                return new Assessment(
                        Severity.CRITICAL,
                        parameters.termGoMessage(),
                        count, averageInterval, averageDuration);
            }
            return new Assessment(
                    Severity.WARNING,
                    parameters.termWarnMessage(),
                    count, averageInterval, averageDuration);
        }

        return new Assessment(
                Severity.INFO,
                "Схватки пока нерегулярные. Продолжайте наблюдение.",
                count, averageInterval, averageDuration);
    }

    public static Assessment watersBroken() {
        return new Assessment(
                Severity.CRITICAL,
                "Отошли воды. Немедленно поезжайте в родильный дом или вызовите 103.",
                0, null, null);
    }
}
