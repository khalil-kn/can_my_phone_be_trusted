package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One row = one alert shown in the Notifications screen. */
@Entity(tableName = "alerts")
public class AlertEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long at;
    public String title;
    public String body;
    /** 0 for a weekly summary. */
    public long incidentId;
    public boolean read;
}
