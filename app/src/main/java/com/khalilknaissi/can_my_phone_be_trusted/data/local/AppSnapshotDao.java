package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AppSnapshotDao {

    @Insert
    void insertAll(List<AppSnapshotEntity> apps);

    @Query("SELECT * FROM app_snapshots ORDER BY app_label")
    List<AppSnapshotEntity> getAll();

    @Query("SELECT COUNT(*) FROM app_snapshots")
    int count();

    @Query("DELETE FROM app_snapshots")
    void deleteAll();
}
