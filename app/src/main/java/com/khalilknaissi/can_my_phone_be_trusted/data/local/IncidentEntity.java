package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.khalilknaissi.can_my_phone_be_trusted.model.IncidentState;
import com.khalilknaissi.can_my_phone_be_trusted.model.Severity;

import java.util.Locale;

/** One row = one incident: related changes grouped and scored, with the user's investigation. */
@Entity(tableName = "incidents")
public class IncidentEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String title;
    /** The app most of the changes are about. Null when it's only settings. */
    public String primaryPackage;
    public String appLabel;
    public int score;
    /** An IncidentState name. */
    public String state;
    public String notes;
    /** Keys of investigation steps marked done, comma-separated. */
    public String stepsDone;
    public long createdAt;
    public long updatedAt;
    public long closedAt;

    public IncidentState state() {
        return IncidentState.valueOf(state);
    }

    public Severity severity() {
        return Severity.ofScore(score);
    }

    /** INC-0007 */
    public String code() {
        return String.format(Locale.US, "INC-%04d", id);
    }
}
