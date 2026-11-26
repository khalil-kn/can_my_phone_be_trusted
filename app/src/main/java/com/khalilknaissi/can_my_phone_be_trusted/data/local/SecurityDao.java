package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/** Every query the app runs on changes, incidents, checks, reports and alerts. */
@Dao
public interface SecurityDao {

    // ---- baseline settings
    @Insert
    void insertSettings(List<SettingSnapshotEntity> settings);

    @Query("SELECT * FROM setting_snapshots")
    List<SettingSnapshotEntity> settings();

    @Query("DELETE FROM setting_snapshots")
    void deleteSettings();

    // ---- changes (security events)
    @Insert
    long insertChange(ChangeEntity change);

    @Update
    void updateChange(ChangeEntity change);

    @Query("SELECT * FROM changes WHERE revertedAt = 0")
    List<ChangeEntity> activeChanges();

    @Query("SELECT * FROM changes WHERE revertedAt = 0 AND reviewed = 0 ORDER BY detectedAt DESC, points DESC")
    List<ChangeEntity> unreviewedChanges();

    @Query("SELECT * FROM changes WHERE detectedAt >= :since ORDER BY detectedAt DESC, points DESC")
    List<ChangeEntity> changesSince(long since);

    @Query("SELECT * FROM changes ORDER BY detectedAt DESC LIMIT 400")
    List<ChangeEntity> recentChanges();

    @Query("SELECT * FROM changes WHERE id = :id")
    ChangeEntity change(long id);

    @Query("SELECT * FROM changes WHERE incidentId = :incidentId ORDER BY points DESC")
    List<ChangeEntity> changesFor(long incidentId);

    @Query("SELECT COALESCE(SUM(points), 0) FROM changes WHERE incidentId = :incidentId")
    int pointsFor(long incidentId);

    @Query("UPDATE changes SET incidentId = :incidentId WHERE id = :changeId")
    void linkChange(long changeId, long incidentId);

    @Query("UPDATE changes SET reviewed = 1 WHERE id = :id")
    void markReviewed(long id);

    @Query("UPDATE changes SET reviewed = 1 WHERE incidentId = :incidentId")
    void markIncidentReviewed(long incidentId);

    /** A new baseline makes every current difference the new normal. */
    @Query("UPDATE changes SET reviewed = 1, revertedAt = :now WHERE revertedAt = 0")
    void acceptAll(long now);

    @Query("SELECT COUNT(*) FROM changes")
    int changeCount();

    // ---- incidents
    @Insert
    long insertIncident(IncidentEntity incident);

    @Update
    void updateIncident(IncidentEntity incident);

    @Query("SELECT * FROM incidents WHERE id = :id")
    IncidentEntity incident(long id);

    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    List<IncidentEntity> incidents();

    @Query("SELECT * FROM incidents WHERE state IN ('OPEN', 'INVESTIGATING') ORDER BY score DESC, createdAt DESC")
    List<IncidentEntity> activeIncidents();

    @Query("SELECT * FROM incidents WHERE state IN ('OPEN', 'INVESTIGATING') AND primaryPackage IS :pkg ORDER BY createdAt DESC LIMIT 1")
    IncidentEntity activeIncidentFor(String pkg);

    @Query("SELECT * FROM incidents WHERE state IN ('RESOLVED', 'FALSE_POSITIVE') ORDER BY closedAt DESC LIMIT 1")
    IncidentEntity lastClosedIncident();

    // ---- checks
    @Insert
    long insertCheck(CheckEntity check);

    @Query("SELECT * FROM checks ORDER BY ranAt DESC LIMIT 200")
    List<CheckEntity> checks();

    @Query("SELECT COUNT(*) FROM checks")
    int checkCount();

    @Query("SELECT COUNT(*) FROM changes WHERE detectedAt >= :since")
    int changesSinceCount(long since);

    // ---- reports
    @Insert
    long insertReport(ReportEntity report);

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    List<ReportEntity> reports();

    // ---- alerts
    @Insert
    long insertAlert(AlertEntity alert);

    @Query("SELECT * FROM alerts WHERE at >= :since ORDER BY at DESC")
    List<AlertEntity> alertsSince(long since);

    @Query("SELECT COUNT(*) FROM alerts WHERE read = 0")
    int unreadAlerts();

    @Query("UPDATE alerts SET read = 1")
    void markAlertsRead();
}
