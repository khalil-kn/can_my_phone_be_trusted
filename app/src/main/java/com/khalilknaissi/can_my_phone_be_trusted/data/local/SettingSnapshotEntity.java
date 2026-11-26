package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One row = one security setting as it was when the baseline was sealed. */
@Entity(tableName = "setting_snapshots")
public class SettingSnapshotEntity {
    @PrimaryKey
    @NonNull
    public String key = "";
    public String value;
}
