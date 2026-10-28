package ru.antenatal.domain;

public enum BmiCategory {
    UNDERWEIGHT("Дефицит массы"),
    NORMAL("Норма"),
    OVERWEIGHT("Избыточная масса"),
    OBESE("Ожирение");

    private final String label;

    BmiCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static BmiCategory fromBmi(double bmi) {
        if (bmi < 18.5) {
            return UNDERWEIGHT;
        }
        if (bmi < 25.0) {
            return NORMAL;
        }
        if (bmi < 30.0) {
            return OVERWEIGHT;
        }
        return OBESE;
    }
}
