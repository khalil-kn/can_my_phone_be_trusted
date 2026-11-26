package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        AppSnapshotEntity.class,
        SettingSnapshotEntity.class,
        CheckEntity.class,
        ChangeEntity.class,
        IncidentEntity.class,
        ReportEntity.class,
        AlertEntity.class
}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AppSnapshotDao appSnapshotDao();

    public abstract SecurityDao securityDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "can_my_phone_be_trusted.db")
                            // shortcut: wipes the database when the schema version changes. Fine before
                            // the first public release; write real migrations once people rely on their data.
                            .fallbackToDestructiveMigration(true)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
