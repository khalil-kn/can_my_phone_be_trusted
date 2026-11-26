package com.khalilknaissi.can_my_phone_be_trusted.model;

/** One difference between the baseline and the phone right now. */
public final class Finding {
    public final ChangeKind kind;
    /** Null for setting changes. */
    public final String packageName;
    public final String appLabel;
    /** The permission, special access or setting key involved. Null for whole-app changes. */
    public final String subject;
    public final String oldValue;
    public final String newValue;
    /** Exact time from Android, or 0 when only a window between two checks is known. */
    public final long exactAt;
    /** Risk points this change adds to an incident score. */
    public final int points;

    public Finding(ChangeKind kind, String packageName, String appLabel, String subject,
                   String oldValue, String newValue, long exactAt, int points) {
        this.kind = kind;
        this.packageName = packageName;
        this.appLabel = appLabel;
        this.subject = subject;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.exactAt = exactAt;
        this.points = points;
    }

    /** Identifies "the same change" across checks, so it is stored once and not on every check. */
    public String key() {
        return kind + "|" + (packageName == null ? "" : packageName) + "|" + (subject == null ? "" : subject);
    }

    public Severity severity() {
        return Severity.ofPoints(points);
    }
}
