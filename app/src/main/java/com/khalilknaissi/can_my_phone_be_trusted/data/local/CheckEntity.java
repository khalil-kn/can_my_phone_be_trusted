package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One row = one time the phone was compared with the baseline. */
@Entity(tableName = "checks")
public class CheckEntity {
    public static final String MANUAL = "MANUAL";
    public static final String AUTO = "AUTO";
    public static final String BASELINE = "BASELINE";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long ranAt;
    /** MANUAL, AUTO (background) or BASELINE (a new baseline was sealed). */
    public String trigger;
    public int appCount;
    public int newChanges;
}
