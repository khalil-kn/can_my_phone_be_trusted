package com.khalilknaissi.can_my_phone_be_trusted.model;

/** The five severity levels from the scoping document, lowest first. */
public enum Severity {
    INFO("Info"), LOW("Low"), MODERATE("Moderate"), HIGH("High"), CRITICAL("Critical");

    public final String label;

    Severity(String label) {
        this.label = label;
    }

    /** How many of the five severity bars are filled (1 to 5). */
    public int segments() {
        return ordinal() + 1;
    }

    /** High and Critical are drawn in red and trigger alerts. */
    public boolean isHot() {
        return this == HIGH || this == CRITICAL;
    }

    /** Severity of one change, from its risk points. */
    public static Severity ofPoints(int points) {
        if (points >= 30) return CRITICAL;
        if (points >= 15) return HIGH;
        if (points >= 5) return MODERATE;
        if (points >= 1) return LOW;
        return INFO;
    }

    /** Severity of an incident, from its 0-100 score. */
    public static Severity ofScore(int score) {
        if (score >= 80) return CRITICAL;
        if (score >= 60) return HIGH;
        if (score >= 40) return MODERATE;
        if (score >= 20) return LOW;
        return INFO;
    }
}
