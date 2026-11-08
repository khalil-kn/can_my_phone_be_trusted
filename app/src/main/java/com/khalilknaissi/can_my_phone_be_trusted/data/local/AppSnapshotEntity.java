package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_snapshots")
public class AppSnapshotEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "package_name")
    public String packageName;

    @ColumnInfo(name = "app_label")
    public String appLabel;

    @ColumnInfo(name = "version_name")
    public String versionName;

    @ColumnInfo(name = "version_code")
    public long versionCode;

    @ColumnInfo(name = "is_system_app")
    public boolean isSystemApp;

    @ColumnInfo(name = "captured_at")
    public long capturedAt;

    public AppSnapshotEntity(@NonNull String packageName, String appLabel, String versionName,
                             long versionCode, boolean isSystemApp, long capturedAt) {
        this.packageName = packageName;
        this.appLabel = appLabel;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.isSystemApp = isSystemApp;
        this.capturedAt = capturedAt;
    }
}
