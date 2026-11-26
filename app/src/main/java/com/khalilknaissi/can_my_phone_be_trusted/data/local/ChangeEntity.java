package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.khalilknaissi.can_my_phone_be_trusted.engine.RiskRules;
import com.khalilknaissi.can_my_phone_be_trusted.model.ChangeKind;
import com.khalilknaissi.can_my_phone_be_trusted.model.Finding;
import com.khalilknaissi.can_my_phone_be_trusted.model.Severity;

/**
 * One row = one security event: a difference from the baseline, stored the first time
 * a check sees it. While it stays on the phone, later checks don't store it again.
 */
@Entity(tableName = "changes", indices = {@Index("fingerprint"), @Index("incidentId")})
public class ChangeEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    /** Same as Finding.key(): tells two checks they saw the same change. */
    @NonNull
    public String fingerprint = "";
    @NonNull
    public String kind = "";
    public String packageName;
    public String appLabel;
    public String subject;
    public String oldValue;
    public String newValue;
    public int points;
    /** When the check that found it ran. */
    public long detectedAt;
    /** The change happened somewhere between windowStart and windowEnd... */
    public long windowStart;
    public long windowEnd;
    /** ...unless Android gave the exact time (installs and updates). 0 = unknown. */
    public long exactAt;
    public boolean reviewed;
    /** When a later check saw the phone back to its baseline value. 0 = still there. */
    public long revertedAt;
    /** The incident it belongs to. 0 = none. */
    public long incidentId;

    public static ChangeEntity from(Finding f, long checkTime, long previousCheck) {
        ChangeEntity c = new ChangeEntity();
        c.fingerprint = f.key();
        c.kind = f.kind.name();
        c.packageName = f.packageName;
        c.appLabel = f.appLabel;
        c.subject = f.subject;
        c.oldValue = f.oldValue;
        c.newValue = f.newValue;
        c.points = f.points;
        c.detectedAt = checkTime;
        c.windowStart = previousCheck;
        c.windowEnd = checkTime;
        c.exactAt = f.exactAt;
        return c;
    }

    public Finding toFinding() {
        return new Finding(kind(), packageName, appLabel, subject, oldValue, newValue, exactAt, points);
    }

    public ChangeKind kind() {
        return ChangeKind.valueOf(kind);
    }

    public Severity severity() {
        return Severity.ofPoints(points);
    }

    public boolean isSetting() {
        return packageName == null;
    }

    public boolean isReverted() {
        return revertedAt > 0;
    }

    public String title() {
        if (isSetting()) return RiskRules.settingLabel(subject);
        return RiskRules.title(kind(), appLabel, subject);
    }

    /** Who the change is about: the app's name, or "Setting". */
    public String source() {
        return isSetting() ? "Setting" : appLabel;
    }
}
