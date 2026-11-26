package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One row = one app as it was when the baseline was sealed. */
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

    // Added in Sprint 2 for deeper detection. Room fills these through the public fields.
    public String installer;

    @ColumnInfo(name = "first_install_time")
    public long firstInstallTime;

    @ColumnInfo(name = "last_update_time")
    public long lastUpdateTime;

    /** SHA-256 of the signing certificate. */
    public String signer;

    /** Granted dangerous permissions, comma-separated (READ_SMS,CAMERA). */
    public String permissions;

    /** Special access held, comma-separated (Accessibility service,Device admin). */
    @ColumnInfo(name = "special_access")
    public String specialAccess;

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
