package ru.antenatal.calc;

import ru.antenatal.domain.GlucoseContext;
import ru.antenatal.domain.Severity;

import java.math.BigDecimal;
import java.util.Map;

public final class VitalsEvaluator {

    public record Thresholds(
            BigDecimal sbpWarn,
            BigDecimal sbpCrit,
            BigDecimal dbpWarn,
            BigDecimal dbpCrit,
            BigDecimal gluFastWarn,
            BigDecimal gluFastCrit,
            BigDecimal gluPostWarn,
            BigDecimal gluPostCrit
    ) {
    }

    public record VitalAlert(Severity severity, String message) {
    }

    private VitalsEvaluator() {
    }

    public static Thresholds fromMap(Map<String, BigDecimal> values) {
        return new Thresholds(
                values.getOrDefault("SBP_WARN", BigDecimal.valueOf(140)),
                values.getOrDefault("SBP_CRIT", BigDecimal.valueOf(160)),
                values.getOrDefault("DBP_WARN", BigDecimal.valueOf(90)),
                values.getOrDefault("DBP_CRIT", BigDecimal.valueOf(110)),
                values.getOrDefault("GLU_FAST_WARN", BigDecimal.valueOf(5.1)),
                values.getOrDefault("GLU_FAST_CRIT", BigDecimal.valueOf(7.0)),
                values.getOrDefault("GLU_POST_WARN", BigDecimal.valueOf(6.7)),
                values.getOrDefault("GLU_POST_CRIT", BigDecimal.valueOf(11.1))
        );
    }

    public static VitalAlert evaluateBp(Integer sbp, Integer dbp, Thresholds t) {
        if (sbp == null && dbp == null) {
            return null;
        }
        BigDecimal s = sbp == null ? null : BigDecimal.valueOf(sbp);
        BigDecimal d = dbp == null ? null : BigDecimal.valueOf(dbp);

        if ((s != null && s.compareTo(t.sbpCrit()) >= 0) || (d != null && d.compareTo(t.dbpCrit()) >= 0)) {
            return new VitalAlert(Severity.CRITICAL,
                    "Очень высокое давление. Срочно свяжитесь с врачом или вызовите 103.");
        }
        if ((s != null && s.compareTo(t.sbpWarn()) >= 0) || (d != null && d.compareTo(t.dbpWarn()) >= 0)) {
            return new VitalAlert(Severity.WARNING,
                    "Давление выше целевого. Сообщите врачу и измерьте повторно через 15 минут.");
        }
        return null;
    }

    public static VitalAlert evaluateGlucose(BigDecimal glucose, GlucoseContext context, Thresholds t) {
        if (glucose == null || context == null) {
            return null;
        }
        boolean fasting = context == GlucoseContext.FASTING;
        BigDecimal warn = fasting ? t.gluFastWarn() : t.gluPostWarn();
        BigDecimal crit = fasting ? t.gluFastCrit() : t.gluPostCrit();
        if (glucose.compareTo(crit) >= 0) {
            return new VitalAlert(Severity.CRITICAL,
                    "Очень высокий уровень глюкозы. Срочно свяжитесь с врачом.");
        }
        if (glucose.compareTo(warn) >= 0) {
            return new VitalAlert(Severity.WARNING,
                    "Уровень глюкозы выше целевого. Сообщите врачу.");
        }
        return null;
    }
}
