package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One row = one saved report file (PDF or JSON) kept on the phone. */
@Entity(tableName = "reports")
public class ReportEntity {
    public static final String PDF = "PDF";
    public static final String JSON = "JSON";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long createdAt;
    public String title;
    public String format;
    /** 0 when the report covers everything since the baseline. */
    public long incidentId;
    public String path;
    public long sizeBytes;
    public int pages;

    public boolean isPdf() {
        return PDF.equals(format);
    }
}
